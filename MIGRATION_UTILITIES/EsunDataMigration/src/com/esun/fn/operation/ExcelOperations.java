package com.esun.fn.operation;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import com.config.ConfigReader;
import com.esun.fn.datamodel.ESUNMetaData;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;

public class ExcelOperations {

	private static final Logger logger = LogManager.getLogger(ExcelOperations.class);

	private static final String BUSINESS_UNIT_CORPORATE_BANKING = "Corporate Banking";
	private static final String BUSINESS_UNIT_PRIVATE_BANKING = "Private Banking";

	// Matches path separators, other filesystem-reserved characters, and control characters
	// (including CR/LF) so Excel-sourced values can never escape the intended folder via a crafted
	// customer ID/name, and can never forge extra lines in the log output.
	private static final String UNSAFE_FILE_AND_LOG_CHARS = "[\\\\/:*?\"<>|\\r\\n\\t\\p{Cntrl}]+";

	/**
	 * Neutralizes an Excel-sourced value before it is used to build a filesystem path or written to
	 * the log: strips path separators/reserved characters and control characters (replacing them
	 * with "_"), and collapses ".." sequences so the result can never be interpreted as a directory
	 * traversal segment.
	 */
	static String sanitizeForFileSystemAndLog(String value) {
		if (value == null) {
			return "";
		}
		return value.replaceAll(UNSAFE_FILE_AND_LOG_CHARS, "_").replace("..", "_").trim();
	}

	/**
	 * Reads CB and PB Excel files and combines all customer metadata into one list.

	 *
	 * The paths are read from:
	 *
	 * cb.excel.path pb.excel.path
	 */
	
	public List<ESUNMetaData> readMetaDataInfoFrmExcel(File excelFile) {

	    List<ESUNMetaData> documentMetaDataList = new ArrayList<>();

	    if (excelFile == null) {

	        logger.error("Excel file reference is null.");
	        return documentMetaDataList;
	    }

	    if (!excelFile.exists()) {

	        logger.error("Excel file not found: {}", excelFile.getAbsolutePath());
	        return documentMetaDataList;
	    }

	    logger.info("Reading metadata Excel file: {}", excelFile.getAbsolutePath());

	    Map<String, ESUNMetaData> customerMetadataMap = new LinkedHashMap<>();

		readSingleExcelFile(excelFile, customerMetadataMap, null);

	    documentMetaDataList.addAll(customerMetadataMap.values());

	    logger.info("Customers loaded from {}: {}",
	            excelFile.getName(),
	            documentMetaDataList.size());

	    return documentMetaDataList;
	}


	/**
	 * Reads one Excel file and adds customer metadata into the common map.
	 */
	private void readSingleExcelFile(File excelFile, Map<String, ESUNMetaData> customerMetadataMap, String sourceType) {

		logger.info("--------------------------------------------------");
		logger.info("Reading {} metadata Excel: {}", sourceType, excelFile.getAbsolutePath());
		logger.info("--------------------------------------------------");

		try (FileInputStream fis = new FileInputStream(excelFile); XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

			if (workbook.getNumberOfSheets() == 0) {

				logger.error("Excel file contains no sheets: {}", excelFile.getName());

				return;
			}

			XSSFSheet sheet = workbook.getSheetAt(0);

			logger.info("Excel sheet selected: {}", sheet.getSheetName());

			logger.info("Excel last row number: {}", sheet.getLastRowNum());

			DataFormatter formatter = new DataFormatter();

			XSSFRow headerRow = sheet.getRow(0);

			if (!validateHeader(headerRow, formatter)) {

				logger.error("Excel header validation FAILED for: {}", excelFile.getName());

				return;
			}

			logger.info("Excel header validation SUCCESSFUL: {}", excelFile.getName());

			// ======================================================
			// BUILD HEADER MAP
			// ======================================================

			Map<String, Integer> headerMap = new HashMap<>();

			for (int cellNum = 0; cellNum < headerRow.getLastCellNum(); cellNum++) {

				Cell cell = headerRow.getCell(cellNum);

				if (cell == null) {
					continue;
				}

				String header = formatter.formatCellValue(cell).trim();

				if (!header.isEmpty()) {

					String normalizedHeader = header.toLowerCase().trim();

					headerMap.put(normalizedHeader, cellNum);

					logger.info("Excel Header [{}] -> Column [{}]", header, cellNum);
				}
			}

			// ======================================================
			// READ DATA ROWS
			// ======================================================

			int processedRows = 0;
			int validCustomers = 0;
			int skippedRows = 0;

			for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {

				XSSFRow row = sheet.getRow(rowNum);

				if (row == null) {

					logger.warn("Skipping empty row {} in {}", rowNum + 1, excelFile.getName());

					continue;
				}

				processedRows++;

				String customerId = getValue(row, headerMap, formatter, "customer_id").trim();

				if (customerId.isEmpty()) {

					logger.warn("Skipping row {} in {} because Customer ID is empty.", rowNum + 1, excelFile.getName());

					skippedRows++;

					continue;
				}

				// Customer ID is the unique/dedup key: unlike Customer Name, it must never be
				// silently mutated. Two different raw IDs could otherwise sanitize to the same
				// value (e.g. "ABC/123" and "ABC\123" both becoming "ABC_123") and be treated as
				// the same customer. Reject the row instead so bad source data gets fixed upstream.
				if (!customerId.equals(sanitizeForFileSystemAndLog(customerId))) {

					// Do not log the raw customerId here: it is, by definition, the value that just
					// failed the safety check, so it could itself contain characters unsafe to log
					// (e.g. embedded newlines). Row number + file name is enough to locate it.
					logger.error(
							"Skipping row {} in {} because its Customer ID contains characters that are not "
									+ "safe to use as a unique identifier or folder name. Fix the source data and re-run.",
							rowNum + 1, excelFile.getName());

					skippedRows++;

					continue;
				}

				String customerName = getValue(row, headerMap, formatter, "customer_name");

				customerName = sanitizeForFileSystemAndLog(customerName);

				if (customerName.isEmpty()) {

					logger.warn("Customer Name is empty for Customer ID {} in row {}.", customerId, rowNum + 1);
				}

				// ==================================================
				// GET / CREATE CUSTOMER METADATA
				// ==================================================

				ESUNMetaData metadata = customerMetadataMap.get(customerId);

				if (metadata == null) {

					metadata = new ESUNMetaData();

					metadata.setCustomerId(customerId);

					metadata.setCustomerName(customerName);

					metadata.setAccountClosed("No");

					customerMetadataMap.put(customerId, metadata);

					validCustomers++;

					logger.info("NEW CUSTOMER LOADED -> [{}] {} | Source: {}", customerId, customerName, sourceType);

				} else {

					/*
					 * Same Customer ID can have multiple rows. Do not create another customer
					 * object.
					 */
					logger.debug("Existing Customer ID {} found again in {} row {}.", customerId, excelFile.getName(),
							rowNum + 1);

					/*
					 * If the existing customer name is empty, update it when a valid name is
					 * available.
					 */
					if ((metadata.getCustomerName() == null || metadata.getCustomerName().trim().isEmpty())
							&& !customerName.isEmpty()) {

						metadata.setCustomerName(customerName);
					}
				}

				// ==================================================
				// ACCOUNT NUMBER
				// ==================================================

				String accountNumber = getValue(row, headerMap, formatter, "account number");

				if (!accountNumber.isEmpty()
						&& (metadata.getAccountNumber() == null || metadata.getAccountNumber().trim().isEmpty())) {

					metadata.setAccountNumber(accountNumber);

					logger.debug("Customer {} -> Account Number: {}", customerId, accountNumber);
				}

				// ==================================================
				// ACCOUNT OPENING DATE
				// ==================================================

				String accountOpeningDate = getValue(row, headerMap, formatter, "account opening date");

				if (!accountOpeningDate.isEmpty() && (metadata.getAccountOpeningDate() == null
						|| metadata.getAccountOpeningDate().trim().isEmpty())) {

					metadata.setAccountOpeningDate(accountOpeningDate);
				}

				// ==================================================
				// BUSINESS UNIT
				// ==================================================

				String businessUnit = getValue(row, headerMap, formatter, "business unit");

				if (!businessUnit.isEmpty()) {

					if (businessUnit.equalsIgnoreCase(BUSINESS_UNIT_CORPORATE_BANKING)) {

						metadata.setBusinessUnit(BUSINESS_UNIT_CORPORATE_BANKING);

					} else if (businessUnit.equalsIgnoreCase(BUSINESS_UNIT_PRIVATE_BANKING)) {

						metadata.setBusinessUnit(BUSINESS_UNIT_PRIVATE_BANKING);

					} else {

						metadata.setBusinessUnit(businessUnit);
					}
				}

				// ==================================================
				// COUNTRY
				// ==================================================

				String placeOfBirthCountry = getValue(row, headerMap, formatter, "place_of_birth_country");

				String countryOfIncorporation = getValue(row, headerMap, formatter, "country_of_incorporation");

				if (!placeOfBirthCountry.isEmpty()) {

					metadata.setCountryOfIncorporationBirth(placeOfBirthCountry);

				} else if (!countryOfIncorporation.isEmpty()) {

					metadata.setCountryOfIncorporationBirth(countryOfIncorporation);
				}

				// ==================================================
				// LEGAL DOCUMENT
				// ==================================================

				String legalDocType = getValue(row, headerMap, formatter, "legal_doc_type");

				String legalIdNo = getValue(row, headerMap, formatter, "legal_id_no");

				if (!legalDocType.isEmpty() && !legalIdNo.isEmpty()) {

					if (legalDocType.equalsIgnoreCase("Passport")) {

						metadata.setPassportNo(legalIdNo);

						logger.info("Customer {} - Passport = {}", customerId, legalIdNo);

					} else if (legalDocType.equalsIgnoreCase("Identification Number")) {

						metadata.setIdentityNumber(legalIdNo);

						logger.info("Customer {} - Identity Number = {}", customerId, legalIdNo);

					} else if (legalDocType.equalsIgnoreCase("Business Registration Certificate")) {

						metadata.setBusinessRegistrationCertificate(legalIdNo);

						logger.info("Customer {} - Business Registration Certificate = {}", customerId, legalIdNo);
					}
				}

				// ==================================================
				// DOCUMENT ISSUE DATE
				// ==================================================

				String documentIssueDate = getValue(row, headerMap, formatter, "document_issue_date");

				if (!documentIssueDate.isEmpty()) {

					metadata.setW8BenDate(documentIssueDate);
				}

				// ==================================================
				// RM / STAFF
				// ==================================================

				String rmStaffNo = getValue(row, headerMap, formatter, "rm_staff_no");

				String cashManagementStaffNo = getValue(row, headerMap, formatter, "cash_management_staff_no");

				String wmPbStaffNo = getValue(row, headerMap, formatter, "wm_pb_staff_no");

				if (BUSINESS_UNIT_CORPORATE_BANKING.equalsIgnoreCase(metadata.getBusinessUnit())) {

					if (!rmStaffNo.isEmpty()) {

						metadata.setRmId(rmStaffNo);

					} else if (!cashManagementStaffNo.isEmpty()) {

						metadata.setRmId(cashManagementStaffNo);
					}

				} else if (BUSINESS_UNIT_PRIVATE_BANKING.equalsIgnoreCase(metadata.getBusinessUnit())
						&& !wmPbStaffNo.isEmpty()) {

					metadata.setRmId(wmPbStaffNo);
				}

				logger.info("Processed Excel row {} -> Customer ID: {} | Name: {} | Source: {}", rowNum + 1, customerId,
						customerName, sourceType);
			}

			logger.info("Finished reading {} Excel: {}", sourceType, excelFile.getName());

			logger.info("Rows processed: {} | New customers: {} | Skipped rows: {}", processedRows, validCustomers,
					skippedRows);

		} catch (Exception e) {

			logger.error("Failed to read Excel file: {}", excelFile.getAbsolutePath(), e);
		}
	}

	/**
	 * Reads a value from an Excel row.
	 */
	private String getValue(Row row, Map<String, Integer> headerMap, DataFormatter formatter, String headerName) {

		Integer columnIndex = headerMap.get(headerName.toLowerCase().trim());

		if (columnIndex == null) {

			return "";
		}

		Cell cell = row.getCell(columnIndex);

		if (cell == null) {

			return "";
		}

		return formatter.formatCellValue(cell).trim();
	}

	/**
	 * Validates required Excel headers.
	 */
	private boolean validateHeader(XSSFRow headerRow, DataFormatter formatter) {

		if (headerRow == null) {

			logger.error("Excel header row is missing.");

			return false;
		}

		Set<String> actualHeaders = new HashSet<>();

		for (int i = 0; i < headerRow.getLastCellNum(); i++) {

			Cell cell = headerRow.getCell(i);

			if (cell == null) {
				continue;
			}

			String header = formatter.formatCellValue(cell).trim();

			if (!header.isEmpty()) {

				actualHeaders.add(header.toLowerCase().trim());
			}
		}

		logger.info("Detected Excel headers: {}", actualHeaders);

		String[] requiredHeaders = {

				"customer_id", "customer_name", "place_of_birth_country", "legal_doc_type", "legal_id_no",
				"country_of_incorporation", "document_issue_date", "rm_staff_no", "cash_management_staff_no",
				"wm_pb_staff_no", "business unit", "account number", "account opening date" };

		boolean valid = true;

		for (String requiredHeader : requiredHeaders) {

			if (!actualHeaders.contains(requiredHeader.toLowerCase())) {

				logger.error("Required Excel header missing: [{}]", requiredHeader);

				valid = false;
			}
		}

		return valid;
	}

	/**
	 * Processes files and subfolders under a customer folder.
	 */
	public int iterateDirectory(String processingFile, ObjectStore osInstance, String subFolderDir, Folder ceRootFolder,
			ESUNMetaData metadata, JSONArray foldersInfo, String successFolderLocation, String failureFolderLocation,
			String folderPath) {

		int documentCount = 0;

		logger.info("Processing folder: {}", processingFile);

		try {

			CeOperations ceOps = new CeOperations();

			FileOperations fileOps = new FileOperations();

			File dataFolder = new File(subFolderDir);

			if (!dataFolder.exists()) {

				logger.error("Folder does not exist: {}", subFolderDir);

				return documentCount;
			}

			JSONObject folderJsonObj = new JSONObject();

			folderJsonObj.put("FolderName", folderPath);

			folderJsonObj.put("totalFilesCount", FileOperations.getRootFolderFileCount(dataFolder));

			int folderFilesSuccessCount = 0;

			int folderFilesFailureCount = 0;

			File[] dataFolderList = dataFolder.listFiles();

			if (dataFolderList == null) {

				logger.info("No files found in folder: {}", folderPath);

				foldersInfo.put(folderJsonObj);

				return documentCount;
			}

			for (File processingData : dataFolderList) {

				if (processingData.isDirectory()) {

					logger.info("Processing subfolder: {}", processingData.getName());

					String subfolderId = ceOps.createSubfolderHiracy(osInstance, processingData.getName(),
							ceRootFolder);

					if (subfolderId != null && !subfolderId.isEmpty()) {

						Folder subFolderInstance = ceOps.getFolderInstance(osInstance, subfolderId);

						documentCount += this.iterateDirectory(processingData.getName(), osInstance,
								processingData.getAbsolutePath(), subFolderInstance, metadata, foldersInfo,
								successFolderLocation, failureFolderLocation,
								folderPath + "/" + processingData.getName());
					}

				} else {

					File customerRootFolder = new File(ConfigReader.getProperty("source.root") + File.separator
							+ metadata.getCustomerId() + " - " + metadata.getCustomerName());

					logger.info("Uploading document: {}", processingData.getName());

					boolean docCreationStatus = ceOps.uploadDocument(osInstance, ceRootFolder, processingData,
							metadata);

					if (docCreationStatus) {

						fileOps.moveProcessedFile(processingData, customerRootFolder, successFolderLocation);

						folderFilesSuccessCount++;

						logger.info("Document uploaded successfully: {}", processingData.getName());

					} else {

						fileOps.moveProcessedFile(processingData, customerRootFolder, failureFolderLocation);

						folderFilesFailureCount++;

						logger.error("Document upload failed: {}", processingData.getName());
					}

					documentCount++;
				}
			}

			folderJsonObj.put("successFiles", folderFilesSuccessCount);

			folderJsonObj.put("failureFiles", folderFilesFailureCount);

			foldersInfo.put(folderJsonObj);

			logger.info("Folder processed successfully: {} | Success: {} | Failure: {}", folderPath,
					folderFilesSuccessCount, folderFilesFailureCount);

		} catch (Exception e) {

			logger.error("Error while processing folder: {}", folderPath, e);
		}

		return documentCount;
	}
}