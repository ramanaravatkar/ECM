package com.esun.fn.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.esun.fn.datamodel.ESUNMetaData;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;

public class ExcelOperations {

	public ArrayList<ESUNMetaData> readMetaDataInfoFrmExcel(File excelFile) {

		ArrayList<ESUNMetaData> documentMetaDataList = new ArrayList<>();

		try {

			// Step 2 : Open Workbook

			try (FileInputStream fis = new FileInputStream(excelFile); XSSFWorkbook workbook = new XSSFWorkbook(fis);) {

				XSSFSheet sheet = workbook.getSheetAt(0);

				DataFormatter formatter = new DataFormatter();

				// Step 3 : Read Header

				XSSFRow headerRow = sheet.getRow(0);

				if (!validateHeader(headerRow, formatter)) {
					// logger.info(Header values are mismatched.. Please verify);

					return documentMetaDataList;
				}

				System.out.println("Header Verification Successful.");

				// Step 4 : Read Data

				for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {

					XSSFRow row = sheet.getRow(rowNum);

					if (row == null) {

						continue;
					}

					ESUNMetaData metadata = new ESUNMetaData();

					metadata.setCustomerId(formatter.formatCellValue(row.getCell(0)));
					metadata.setCustomerName(formatter.formatCellValue(row.getCell(1)));
					metadata.setAccountNumber(formatter.formatCellValue(row.getCell(2)));
					metadata.setIdentityNumber(formatter.formatCellValue(row.getCell(3)));
					metadata.setW8BenDate(formatter.formatCellValue(row.getCell(4)));
					metadata.setPassportNo(formatter.formatCellValue(row.getCell(5)));
					metadata.setBusinessRegistrationCertificate(formatter.formatCellValue(row.getCell(6)));
					metadata.setMainDocumentType(formatter.formatCellValue(row.getCell(7)));
					metadata.setSubDocumentType(formatter.formatCellValue(row.getCell(8)));
					metadata.setAccountOpeningDate(formatter.formatCellValue(row.getCell(9)));
					metadata.setBusinessUnit(formatter.formatCellValue(row.getCell(10)));
					metadata.setRmId(formatter.formatCellValue(row.getCell(11)));
					metadata.setCountryOfIncorporationBirth(formatter.formatCellValue(row.getCell(12)));
					metadata.setAccountClosed(formatter.formatCellValue(row.getCell(13)));
					metadata.setShareholderName(formatter.formatCellValue(row.getCell(14)));
					metadata.setTypeOfUpdate(formatter.formatCellValue(row.getCell(15)));

					documentMetaDataList.add(metadata);

				}

			} catch (Exception e) {
				// logger.info("Error occured in the Reading data from Excel ::{},e);
				System.out.println("Erro r in excel reading ::");
				e.printStackTrace();
			}

		} catch (Exception e) {
			// logger.info("Error occured in the Reading data from Excel ::{},e);
			e.printStackTrace();
		}

		return documentMetaDataList;

	}

	/**
	 * Header Verification
	 */
	private boolean validateHeader(Row headerRow, DataFormatter formatter) {

		String[] expectedHeaders = { "Customer ID", "Customer Name", // "SIP Number",
				"Account Number", "Identity Number", "W-8 Ben/W-8 Ben-E Date", "Passport No.",
				"Business Registration Certificate", "Main Document Type", "Sub Document Type", "Account Opening Date",
				"Business Unit", "RM ID", "Country Of Incorporation/Birth", "Account Closed", "Shareholder Name",
				"Type of Update" };

		for (int i = 0; i < expectedHeaders.length; i++) {

			String excelHeader = formatter.formatCellValue(headerRow.getCell(i)).trim();

			if (!expectedHeaders[i].equalsIgnoreCase(excelHeader)) {
// replace with logger. 
				System.out.println("Header Mismatch at Column : " + (i + 1));

				System.out.println("Expected : " + expectedHeaders[i]);

				System.out.println("Found    : " + excelHeader);

				return false;
			}

		}

		return true;

	}

	public int iterateDirectory(String processingFile, ObjectStore osInstance, String subFolderDir, Folder ceRootFolder,
			ESUNMetaData metadata) {
		int documentCount = 0;
		try {

			System.out.println("Processing Folder :::" + processingFile);
			System.out.println("subFolderDir :::" + subFolderDir);

			CeOperations ceOps = new CeOperations();
			File dataFolder = new File(subFolderDir);
			if (dataFolder.exists()) {
				System.out.println("Subfolder exists and processing :::");
				File[] dataFolderList = dataFolder.listFiles();
				for (File processingData : dataFolderList) {

					System.out.println("Subfolder having the files :::");

					if (processingData.isDirectory()) {

						System.out.println("Subfolder having a dire ::" + processingData.getName());

						String subfolderId = ceOps.createSubfolderHiracy(osInstance, processingData.getName(),
								ceRootFolder);

						System.out.println("Created subfolder id ::" + processingData.getName());

						if (subfolderId != null && !subfolderId.isEmpty()) {

							Folder subFolderInstance = ceOps.getFolderInstance(osInstance, subfolderId);

							System.out.println("Able to fetch the created folder :::" + subFolderInstance);

							documentCount += this.iterateDirectory(processingData.getName(), osInstance,
									processingData.getAbsolutePath(), subFolderInstance, metadata);
						}

					} else {

						System.out.println("Uploading File : " + processingData.getName());

						DocumentMigrationImpl migration = new DocumentMigrationImpl();

						ceOps.uploadDocument(osInstance, ceRootFolder, processingData, metadata);
						documentCount++;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error occured in the iterateDirectory");
		}
		return documentCount;
	}

}