package com.widowsfolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.configreader.ConfigReader;

public class CheckWindowsFolder {

	private static final Logger logger = LogManager.getLogger(CheckWindowsFolder.class);

	/*
	 * Expected folder format:
	 *
	 * CustomerID - CustomerName
	 *
	 * Example: 100001 - ABC Company
	 */
	private static final Pattern FOLDER_PATTERN = Pattern.compile("^(\\d+)\\s*-\\s*(.*)$");

	/*
	 * true = copy folder and then delete source folder false = copy folder only
	 */
	private static final boolean MOVE_FOLDERS = true;

	public static void main(String[] args) {

		logger.info("====================================================");
		logger.info("WINDOWS FOLDER PRE-VALIDATION STARTED");
		logger.info("====================================================");

		try {

			// ==========================================================
			// STEP 0: READING CONFIGURATION
			// ==========================================================

			logger.info("STEP 0: READING CONFIGURATION");

			String inputFolderPath = ConfigReader.getProperty("source.folder");

			String validationOutputPath = ConfigReader.getProperty("validation.location");

			String cbErrorExcelPath = ConfigReader.getProperty("cb.error.excel");

			String pbErrorExcelPath = ConfigReader.getProperty("pb.error.excel");

			logger.info("source.folder       : {}", inputFolderPath);
			logger.info("validation.location : {}", validationOutputPath);
			logger.info("cb.error.excel      : {}", cbErrorExcelPath);
			logger.info("pb.error.excel      : {}", pbErrorExcelPath);

			if (isBlank(inputFolderPath)) {

				logger.error("CONFIGURATION ERROR: source.folder is missing in config.properties.");

				logger.error("WINDOWS FOLDER PRE-VALIDATION TERMINATED.");

				return;
			}

			if (isBlank(validationOutputPath)) {

				logger.error("CONFIGURATION ERROR: validation.location is missing in config.properties.");

				logger.error("WINDOWS FOLDER PRE-VALIDATION TERMINATED.");

				return;
			}

			if (isBlank(cbErrorExcelPath)) {

				logger.warn("cb.error.excel is missing in config.properties. "
						+ "CB Excel validation errors will be skipped.");
			}

			if (isBlank(pbErrorExcelPath)) {

				logger.warn("pb.error.excel is missing in config.properties. "
						+ "PB Excel validation errors will be skipped.");
			}

			logger.info("STEP 0 COMPLETED: Configuration loaded successfully.");

			// ==========================================================
			// SOURCE FOLDER VALIDATION
			// ==========================================================

			logger.info("STEP 0.1: VALIDATING SOURCE FOLDER");

			File mainFolder = new File(inputFolderPath);

			logger.info("Source folder absolute path: {}", mainFolder.getAbsolutePath());

			if (!mainFolder.exists()) {

				logger.error("SOURCE FOLDER ERROR: Source folder does not exist: {}", mainFolder.getAbsolutePath());

				return;
			}

			if (!mainFolder.isDirectory()) {

				logger.error("SOURCE FOLDER ERROR: Source path is not a directory: {}", mainFolder.getAbsolutePath());

				return;
			}

			if (!mainFolder.canRead()) {

				logger.error("SOURCE FOLDER ERROR: Source folder is not readable: {}", mainFolder.getAbsolutePath());

				return;
			}

			logger.info("Source folder exists and is readable.");
			logger.info("STEP 0.1 COMPLETED.");

			// ==========================================================
			// STEP 1: PREPARING VALIDATION FOLDERS
			// ==========================================================

			logger.info("STEP 1: PREPARING VALIDATION FOLDERS");

			File validationFolder = new File(validationOutputPath);

			File originalMigrateFolder = new File(validationFolder, "Original-Migrate");

			File excelValidationErrorFolder = new File(validationFolder, "Error folder from excel validation");

			File errorFolders = new File(validationFolder, "Error-Folders");

			createDirectory(validationFolder, "Validation folder");

			createDirectory(originalMigrateFolder, "Original-Migrate folder");

			createDirectory(excelValidationErrorFolder, "Error folder from excel validation");

			createDirectory(errorFolders, "Error-Folders folder");

			logger.info("Validation folders prepared successfully.");
			logger.info("Validation folder      : {}", validationFolder.getAbsolutePath());
			logger.info("Original-Migrate       : {}", originalMigrateFolder.getAbsolutePath());
			logger.info("Excel validation error : {}", excelValidationErrorFolder.getAbsolutePath());
			logger.info("Error-Folders          : {}", errorFolders.getAbsolutePath());

			logger.info("STEP 1 COMPLETED.");

			// ==========================================================
			// STEP 2: CHECKING INITIAL SOURCE FOLDERS
			// ==========================================================

			logger.info("STEP 2: CHECKING INITIAL SOURCE FOLDERS");

			File[] initialFolders = mainFolder.listFiles(File::isDirectory);

			if (initialFolders == null) {

				logger.error("Unable to read directories from source folder: {}", mainFolder.getAbsolutePath());

				return;
			}

			int initialFolderCount = initialFolders.length;

			logger.info("Initial folders found in source folder: {}", initialFolderCount);

			if (initialFolderCount == 0) {

				logger.warn("No folders found in source folder.");

			} else {

				logger.info("Initial source folders:");

				for (File folder : initialFolders) {

					logger.info("  -> {}", folder.getName());
				}
			}

			logger.info("STEP 2 COMPLETED.");

			// ==========================================================
			// STEP 3: COUNTING MIGRATION EXCEL CUSTOMERS
			// ==========================================================

			logger.info("STEP 3: COUNTING MIGRATION EXCEL CUSTOMERS");

			int migrationExcelCustomerCount = countMigrationExcelCustomers(cbErrorExcelPath, pbErrorExcelPath);

			logger.info("Total UNIQUE Migration Excel Customers: {}", migrationExcelCustomerCount);

			logger.info("STEP 3 COMPLETED.");

			// ==========================================================
			// INITIAL COLLECTIONS
			// ==========================================================

			List<FolderDetails> originalFolders = new ArrayList<>();

			List<FolderDetails> duplicateFolders = new ArrayList<>();

			List<ErrorDetails> errorFolderList = new ArrayList<>();

			List<FolderDetails> excelErrorFolders = new ArrayList<>();

			// ==========================================================
			// NO SOURCE FOLDERS
			// ==========================================================

			if (initialFolderCount == 0) {

				logger.warn("No folders found in source folder before Excel validation movement.");

				logger.info("STEP 4: GENERATING EMPTY VALIDATION REPORT");

				String outputExcelPath = new File(validationFolder, "Folder_Validation.xlsx").getAbsolutePath();

				generateExcel(outputExcelPath, originalFolders, duplicateFolders, errorFolderList, excelErrorFolders,
						originalMigrateFolder, excelValidationErrorFolder, errorFolders, initialFolderCount,
						migrationExcelCustomerCount, 0, 0);

				logger.info("Validation report generation completed.");

				logger.info("====================================================");
				logger.info("WINDOWS FOLDER PRE-VALIDATION COMPLETED");
				logger.info("====================================================");

				return;
			}

			// ==========================================================
			// STEP 5: READING CB AND PB EXCEL VALIDATION ERRORS
			// ==========================================================

			logger.info("STEP 5: READING CB AND PB EXCEL VALIDATION ERRORS");

			Set<String> errorCustomerIds = new HashSet<>();

			// ----------------------------------------------------------
			// CB ERROR EXCEL
			// ----------------------------------------------------------

			if (!isBlank(cbErrorExcelPath)) {

				logger.info("Reading CB Excel validation errors from: {}", cbErrorExcelPath);

				Set<String> cbErrorIds = readErrorCustomerIds(cbErrorExcelPath);

				logger.info("CB Excel error customers found: {}", cbErrorIds.size());

				errorCustomerIds.addAll(cbErrorIds);

			} else {

				logger.info("CB error Excel path is empty. Skipping CB error validation.");
			}

			// ----------------------------------------------------------
			// PB ERROR EXCEL
			// ----------------------------------------------------------

			if (!isBlank(pbErrorExcelPath)) {

				logger.info("Reading PB Excel validation errors from: {}", pbErrorExcelPath);

				Set<String> pbErrorIds = readErrorCustomerIds(pbErrorExcelPath);

				logger.info("PB Excel error customers found: {}", pbErrorIds.size());

				errorCustomerIds.addAll(pbErrorIds);

			} else {

				logger.info("PB error Excel path is empty. Skipping PB error validation.");
			}

			logger.info("Total UNIQUE Excel error customers found: {}", errorCustomerIds.size());

			logger.info("STEP 5 COMPLETED.");

			// ==========================================================
			// STEP 6: MOVING EXCEL ERROR CUSTOMERS
			// ==========================================================

			logger.info("STEP 6: MOVING EXCEL ERROR CUSTOMERS");

			int movedErrorCustomers = moveErrorCustomerFolders(errorCustomerIds, inputFolderPath,
					excelValidationErrorFolder.getAbsolutePath(), excelErrorFolders);

			logger.info("Excel error customer folders moved: {}", movedErrorCustomers);

			logger.info("STEP 6 COMPLETED.");

			// ==========================================================
			// STEP 7: READING REMAINING SOURCE FOLDERS
			// ==========================================================

			logger.info("STEP 7: READING REMAINING SOURCE FOLDERS");

			File[] folders = mainFolder.listFiles(File::isDirectory);

			if (folders == null) {

				logger.error("Unable to read remaining folders from source folder: {}", mainFolder.getAbsolutePath());

				return;
			}

			int remainingFolderCount = folders.length;

			logger.info("Remaining folders after Excel validation movement: {}", remainingFolderCount);

			if (remainingFolderCount > 0) {

				logger.info("Remaining source folders:");

				for (File folder : folders) {

					logger.info("  -> {}", folder.getName());
				}

			} else {

				logger.info("No folders remain after Excel validation movement.");
			}

			logger.info("STEP 7 COMPLETED.");

			// ==========================================================
			// STEP 8: IF NO REMAINING FOLDERS
			// ==========================================================

			if (remainingFolderCount == 0) {

				logger.warn("No remaining folders require Windows folder validation.");

				logger.info("STEP 8: GENERATING VALIDATION REPORT");

				String outputExcelPath = new File(validationFolder, "Folder_Validation.xlsx").getAbsolutePath();

				int totalMigrationErrorFolders = duplicateFolders.size() + errorFolderList.size();

				int totalExcelValidationErrors = excelErrorFolders.size();

				generateExcel(outputExcelPath, originalFolders, duplicateFolders, errorFolderList, excelErrorFolders,
						originalMigrateFolder, excelValidationErrorFolder, errorFolders, initialFolderCount,
						migrationExcelCustomerCount, totalMigrationErrorFolders, totalExcelValidationErrors);

				logger.info("Validation report generated: {}", outputExcelPath);

				logger.info("All source folders were handled by Excel pre-validation.");

				logger.info("====================================================");
				logger.info("WINDOWS FOLDER PRE-VALIDATION COMPLETED");
				logger.info("====================================================");

				return;
			}

			// ==========================================================
			// STEP 9: WINDOWS FOLDER VALIDATION STARTED
			// ==========================================================

			logger.info("STEP 9: WINDOWS FOLDER VALIDATION STARTED");

			logger.info("Validating {} remaining folders.", remainingFolderCount);

			Map<String, List<FolderDetails>> foldersByCustomerId = new LinkedHashMap<>();

			int invalidFolderCount = 0;

			// ==========================================================
			// VALIDATE EACH FOLDER
			// ==========================================================

			for (File folder : folders) {

				String folderName = folder.getName();

				String folderPath = folder.getAbsolutePath();

				logger.info("--------------------------------------------");
				logger.info("VALIDATING SOURCE FOLDER: {}", folderName);

				logger.info("Folder Path: {}", folderPath);

				Matcher matcher = FOLDER_PATTERN.matcher(folderName);

				// ------------------------------------------------------
				// INVALID FOLDER FORMAT
				// ------------------------------------------------------

				if (!matcher.matches()) {

					logger.error("INVALID FOLDER -> Customer ID not found / invalid folder format: {}", folderName);

					logger.error("Reason : Customer ID not found / Invalid folder name format");

					logger.error("Action : Folder will be moved to Error-Folders");

					errorFolderList.add(new ErrorDetails("", "", folderName, folderPath,
							"Customer ID not found / Invalid folder name format"));

					invalidFolderCount++;

					logger.info("Validation result: INVALID");

					continue;
				}

				// ------------------------------------------------------
				// CUSTOMER ID
				// ------------------------------------------------------

				String customerId = matcher.group(1).trim();

				// ------------------------------------------------------
				// CUSTOMER NAME
				// ------------------------------------------------------

				String customerName = matcher.group(2).trim();

				logger.info("Customer ID extracted: {}", customerId);

				logger.info("Customer Name extracted: {}", customerName);

				// ------------------------------------------------------
				// CUSTOMER NAME MISSING
				// ------------------------------------------------------

				if (customerName.isEmpty()) {

					logger.error("INVALID FOLDER -> Customer Name not found: {}", folderName);

					logger.error("Reason : Customer Name not found");

					logger.error("Action : Folder will be moved to Error-Folders");

					errorFolderList
							.add(new ErrorDetails(customerId, "", folderName, folderPath, "Customer Name not found"));

					invalidFolderCount++;

					logger.info("Validation result: INVALID");

					continue;
				}

				// ------------------------------------------------------
				// VALID FOLDER
				// ------------------------------------------------------

				FolderDetails details = new FolderDetails(customerId, customerName, folderName, folderPath);

				foldersByCustomerId.computeIfAbsent(customerId, key -> new ArrayList<>()).add(details);

				logger.info("Validation result: VALID");

				logger.debug("Folder accepted for Customer ID {}: {}", customerId, folderName);
			}

			logger.info("--------------------------------------------");

			logger.info("STEP 9 RESULT: Folder format validation completed.");

			logger.info("Valid Customer IDs grouped: {}", foldersByCustomerId.size());

			logger.info("Invalid folder count: {}", invalidFolderCount);

			logger.info("STEP 9 COMPLETED.");

			// ==========================================================
			// STEP 10: IDENTIFY ORIGINAL AND DUPLICATE FOLDERS
			// ==========================================================

			logger.info("STEP 10: IDENTIFYING ORIGINAL AND DUPLICATE FOLDERS");

			for (Map.Entry<String, List<FolderDetails>> entry : foldersByCustomerId.entrySet()) {

				String customerId = entry.getKey();

				List<FolderDetails> customerFolders = entry.getValue();

				// ------------------------------------------------------
				// UNIQUE CUSTOMER
				// ------------------------------------------------------

				if (customerFolders.size() == 1) {

					FolderDetails original = customerFolders.get(0);

					originalFolders.add(original);

					logger.info("ORIGINAL CUSTOMER -> ID: {} | Folder: {}", customerId, original.folderName);

				}

				// ------------------------------------------------------
				// DUPLICATE CUSTOMER ID
				// ------------------------------------------------------

				else {

					duplicateFolders.addAll(customerFolders);

					logger.warn("DUPLICATE CUSTOMER ID -> ID: {} | Number of folders: {}", customerId,
							customerFolders.size());

					String firstCustomerName = customerFolders.get(0).customerName;

					boolean differentCustomerName = false;

					for (FolderDetails folder : customerFolders) {

						if (!firstCustomerName.equals(folder.customerName)) {

							differentCustomerName = true;

							break;
						}
					}

					String errorReason;

					if (differentCustomerName) {

						errorReason = "Same Customer ID with different Customer Name";

					} else {

						errorReason = "Duplicate folder for same Customer ID";
					}

					for (FolderDetails duplicate : customerFolders) {

						duplicate.duplicateReason = errorReason;

						logger.warn("DUPLICATE FOLDER -> {} | Reason: {}", duplicate.folderName, errorReason);
					}
				}
			}

			logger.info("Original unique folders identified: {}", originalFolders.size());

			logger.info("Duplicate folders identified: {}", duplicateFolders.size());

			logger.info("STEP 10 COMPLETED.");

			// ==========================================================
			// STEP 11: MOVING ORIGINAL CUSTOMERS
			// ==========================================================

			logger.info("STEP 11: MOVING ORIGINAL CUSTOMERS");

			if (originalFolders.isEmpty()) {

				logger.info("No original customer folders found.");

			} else {

				for (FolderDetails folder : originalFolders) {

					logger.info("Processing ORIGINAL folder: {}", folder.folderName);

					copyOrMoveFolder(folder, originalMigrateFolder, MOVE_FOLDERS);
				}
			}

			logger.info("Original customer folders processed: {}", originalFolders.size());

			logger.info("STEP 11 COMPLETED.");

			// ==========================================================
			// STEP 12: MOVING DUPLICATE CUSTOMERS
			// ==========================================================

			logger.info("STEP 12: MOVING DUPLICATE CUSTOMERS TO ERROR-FOLDERS");

			if (duplicateFolders.isEmpty()) {

				logger.info("No duplicate folders found.");

			} else {

				for (FolderDetails folder : duplicateFolders) {

					logger.info("Processing DUPLICATE folder: {}", folder.folderName);

					logger.info("Duplicate Reason: {}", folder.duplicateReason);

					copyOrMoveFolder(folder, errorFolders, MOVE_FOLDERS);
				}
			}

			logger.info("Duplicate folders processed: {}", duplicateFolders.size());

			logger.info("STEP 12 COMPLETED.");

			// ==========================================================
			// STEP 13: MOVING INVALID FOLDERS
			// ==========================================================

			logger.info("STEP 13: MOVING INVALID FOLDERS TO ERROR-FOLDERS");

			if (errorFolderList.isEmpty()) {

				logger.info("No invalid folders found.");

			} else {

				for (ErrorDetails error : errorFolderList) {

					logger.info("Processing INVALID folder: {}", error.folderName);

					logger.info("Customer ID: {}", error.customerId);

					logger.info("Customer Name: {}", error.customerName);

					logger.info("Error Reason: {}", error.errorReason);

					copyOrMoveErrorFolder(error, errorFolders, MOVE_FOLDERS);
				}
			}

			logger.info("Invalid folders processed: {}", errorFolderList.size());

			logger.info("STEP 13 COMPLETED.");

			// ==========================================================
			// STEP 14: COUNTS
			// ==========================================================

			int totalMigrationErrorFolders = duplicateFolders.size() + errorFolderList.size();

			int totalExcelValidationErrors = excelErrorFolders.size();

			logger.info("STEP 14: PREPARING FINAL VALIDATION COUNTS");

			logger.info("Initial Windows folders: {}", initialFolderCount);

			logger.info("Migration Excel unique customers: {}", migrationExcelCustomerCount);

			logger.info("Excel validation error folders: {}", totalExcelValidationErrors);

			logger.info("Original-Migrate folders: {}", originalFolders.size());

			logger.info("Duplicate folders: {}", duplicateFolders.size());

			logger.info("Invalid folder errors: {}", errorFolderList.size());

			logger.info("Total Windows validation errors: {}", totalMigrationErrorFolders);

			logger.info("Total Error folders: {}", totalMigrationErrorFolders + totalExcelValidationErrors);

			logger.info("STEP 14 COMPLETED.");

			// ==========================================================
			// STEP 15: GENERATING WINDOWS FOLDER VALIDATION EXCEL
			// ==========================================================

			logger.info("STEP 15: GENERATING WINDOWS FOLDER VALIDATION EXCEL");

			String outputExcelPath = new File(validationFolder, "Folder_Validation.xlsx").getAbsolutePath();

			logger.info("Excel output path: {}", outputExcelPath);

			generateExcel(outputExcelPath, originalFolders, duplicateFolders, errorFolderList, excelErrorFolders,
					originalMigrateFolder, excelValidationErrorFolder, errorFolders, initialFolderCount,
					migrationExcelCustomerCount, totalMigrationErrorFolders, totalExcelValidationErrors);

			logger.info("STEP 15 COMPLETED: Validation Excel generation finished.");

			// ==========================================================
			// FINAL SUMMARY
			// ==========================================================

			logger.info("====================================================");
			logger.info("WINDOWS FOLDER VALIDATION FINAL SUMMARY");
			logger.info("====================================================");

			logger.info("Initial source folders              : {}", initialFolderCount);

			logger.info("Migration Excel unique customers   : {}", migrationExcelCustomerCount);

			logger.info("Excel error customers moved        : {}", movedErrorCustomers);

			logger.info("Remaining folders for validation   : {}", remainingFolderCount);

			logger.info("Original customers                 : {}", originalFolders.size());

			logger.info("Duplicate folders                  : {}", duplicateFolders.size());

			logger.info("Invalid folder errors              : {}", errorFolderList.size());

			logger.info("Total Windows validation errors    : {}", totalMigrationErrorFolders);

			logger.info("Excel validation error folders     : {}", totalExcelValidationErrors);

			logger.info("Total Error Folders                 : {}",
					totalMigrationErrorFolders + totalExcelValidationErrors);

			logger.info("Original-Migrate location          : {}", originalMigrateFolder.getAbsolutePath());

			logger.info("Excel validation error location    : {}", excelValidationErrorFolder.getAbsolutePath());

			logger.info("Error-Folders location              : {}", errorFolders.getAbsolutePath());

			logger.info("Folder Validation Excel            : {}", outputExcelPath);

			logger.info("====================================================");
			logger.info("WINDOWS FOLDER PRE-VALIDATION COMPLETED");
			logger.info("====================================================");

		} catch (Exception e) {

			logger.error("====================================================");

			logger.error("WINDOWS FOLDER PRE-VALIDATION FAILED.", e);

			logger.error("====================================================");
		}
	}

	// ==============================================================
	// READ EXCEL ERROR CUSTOMER IDS
	// ==============================================================

	private static Set<String> readErrorCustomerIds(String errorExcelPath) {

		Set<String> errorCustomerIds = new HashSet<>();

		if (isBlank(errorExcelPath)) {

			logger.warn("Excel error path is empty. Skipping Excel error file.");

			return errorCustomerIds;
		}

		File errorExcel = new File(errorExcelPath);

		logger.info("Checking Excel error file: {}", errorExcel.getAbsolutePath());

		if (!errorExcel.exists()) {

			logger.warn("Migration error Excel not found: {}", errorExcel.getAbsolutePath());

			return errorCustomerIds;
		}

		if (!errorExcel.isFile()) {

			logger.error("Configured Excel error path is not a file: {}", errorExcel.getAbsolutePath());

			return errorCustomerIds;
		}

		logger.info("Reading migration error Excel: {}", errorExcel.getAbsolutePath());

		try (FileInputStream fis = new FileInputStream(errorExcel);

				XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

			if (workbook.getNumberOfSheets() == 0) {

				logger.error("No sheets found in Excel error file: {}", errorExcel.getAbsolutePath());

				return errorCustomerIds;
			}

			Sheet sheet = workbook.getSheetAt(0);

			DataFormatter formatter = new DataFormatter();

			Row headerRow = sheet.getRow(0);

			if (headerRow == null) {

				logger.error("Migration error Excel has no header row: {}", errorExcel.getAbsolutePath());

				return errorCustomerIds;
			}

			int customerIdColumn = -1;

			for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {

				String header = formatter.formatCellValue(headerRow.getCell(cellIndex)).trim();

				if ("customer_id".equalsIgnoreCase(header) || "customer id".equalsIgnoreCase(header)) {

					customerIdColumn = cellIndex;

					break;
				}
			}

			if (customerIdColumn == -1) {

				logger.error("customer_id column not found in migration error Excel: {}", errorExcel.getAbsolutePath());

				return errorCustomerIds;
			}

			logger.info("Customer ID column found at column index: {}", customerIdColumn);

			for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {

				Row row = sheet.getRow(rowNum);

				if (row == null) {

					continue;
				}

				String customerId = formatter.formatCellValue(row.getCell(customerIdColumn)).trim();

				if (!customerId.isEmpty()) {

					errorCustomerIds.add(customerId);

					logger.debug("Excel validation error customer ID: {}", customerId);
				}
			}

			logger.info("Total error customers read from {}: {}", errorExcel.getName(), errorCustomerIds.size());

		} catch (Exception e) {

			logger.error("ERROR reading migration error Excel: {}", errorExcel.getAbsolutePath(), e);
		}

		return errorCustomerIds;
	}

	// ==============================================================
	// MOVE EXCEL ERROR CUSTOMER FOLDERS
	// ==============================================================

	private static int moveErrorCustomerFolders(Set<String> errorCustomerIds, String sourceRoot, String errorLocation,
			List<FolderDetails> excelErrorFolders) {

		int movedCount = 0;

		if (errorCustomerIds == null || errorCustomerIds.isEmpty()) {

			logger.info("No Excel error customers found. " + "No folders will be moved at this stage.");

			return 0;
		}

		File sourceDirectory = new File(sourceRoot);

		File errorDirectory = new File(errorLocation);

		logger.info("Excel error customer source directory: {}", sourceDirectory.getAbsolutePath());

		logger.info("Excel validation error destination: {}", errorDirectory.getAbsolutePath());

		if (!sourceDirectory.exists()) {

			logger.error("Source folder does not exist: {}", sourceDirectory.getAbsolutePath());

			return 0;
		}

		if (!sourceDirectory.isDirectory()) {

			logger.error("Source path is not a directory: {}", sourceDirectory.getAbsolutePath());

			return 0;
		}

		if (!errorDirectory.exists()) {

			if (errorDirectory.mkdirs()) {

				logger.info("Created Excel validation error folder: {}", errorDirectory.getAbsolutePath());
			}
		}

		File[] customerFolders = sourceDirectory.listFiles(File::isDirectory);

		if (customerFolders == null) {

			logger.error("Unable to read source folders: {}", sourceDirectory.getAbsolutePath());

			return 0;
		}

		logger.info("Source folders available before Excel error movement: {}", customerFolders.length);

		for (File folder : customerFolders) {

			String folderName = folder.getName();

			String customerId = extractCustomerId(folderName);

			if (customerId.isEmpty()) {

				logger.debug("Skipping folder because Customer ID " + "could not be extracted: {}", folderName);

				continue;
			}

			if (!errorCustomerIds.contains(customerId)) {

				continue;
			}

			String customerName = extractCustomerName(folderName);

			FolderDetails excelError = new FolderDetails(customerId, customerName, folderName,
					folder.getAbsolutePath());

			excelError.duplicateReason = "Excel Pre-Validation Failed";

			excelErrorFolders.add(excelError);

			File destinationFolder = new File(errorDirectory, folderName);

			logger.warn("Excel validation error customer identified -> " + "ID: {} | Folder: {}", customerId,
					folderName);

			if (destinationFolder.exists()) {

				logger.warn("Excel error destination already exists. " + "Folder will not be moved: {}",
						destinationFolder.getAbsolutePath());

				continue;
			}

			try {

				logger.info("MOVING EXCEL ERROR CUSTOMER");

				logger.info("Customer ID : {}", customerId);

				logger.info("Customer Name : {}", customerName);

				logger.info("FROM : {}", folder.getAbsolutePath());

				logger.info("TO   : {}", destinationFolder.getAbsolutePath());

				copyDirectory(folder.toPath(), destinationFolder.toPath());

				if (!destinationFolder.exists()) {

					throw new IOException("Destination folder was not created.");
				}

				if (MOVE_FOLDERS) {

					deleteDirectory(folder.toPath());
				}

				logger.info("MOVED EXCEL ERROR CUSTOMER SUCCESSFULLY -> {}", folderName);

				movedCount++;

			} catch (Exception e) {

				logger.error("ERROR moving Excel error customer folder: {}", folderName, e);
			}
		}

		logger.info("Excel error folder movement completed. Total moved: {}", movedCount);

		return movedCount;
	}

	// ==============================================================
	// EXTRACT CUSTOMER ID
	// ==============================================================

	private static String extractCustomerId(String folderName) {

		if (folderName == null) {

			return "";
		}

		Matcher matcher = FOLDER_PATTERN.matcher(folderName);

		if (!matcher.matches()) {

			return "";
		}

		return matcher.group(1).trim();
	}

	// ==============================================================
	// EXTRACT CUSTOMER NAME
	// ==============================================================

	private static String extractCustomerName(String folderName) {

		if (folderName == null) {

			return "";
		}

		Matcher matcher = FOLDER_PATTERN.matcher(folderName);

		if (!matcher.matches()) {

			return "";
		}

		return matcher.group(2).trim();
	}

	// ==============================================================
	// COPY OR MOVE VALID FOLDER
	// ==============================================================

	private static void copyOrMoveFolder(FolderDetails folder, File destinationFolder, boolean move) {

		try {

			File source = new File(folder.folderPath);

			File destination = new File(destinationFolder, folder.folderName);

			if (!source.exists()) {

				logger.error("Source folder does not exist: {}", source.getAbsolutePath());

				return;
			}

			if (!source.isDirectory()) {

				logger.error("Source path is not a directory: {}", source.getAbsolutePath());

				return;
			}

			if (destination.exists()) {

				logger.warn("Destination already exists. " + "Folder will not be processed: {}",
						destination.getAbsolutePath());

				return;
			}

			logger.info("PROCESSING FOLDER: {}", folder.folderName);

			logger.info("Customer ID: {}", folder.customerId);

			logger.info("Customer Name: {}", folder.customerName);

			logger.info("FROM: {}", source.getAbsolutePath());

			logger.info("TO  : {}", destination.getAbsolutePath());

			copyDirectory(source.toPath(), destination.toPath());

			if (!destination.exists()) {

				throw new IOException("Destination folder was not created.");
			}

			if (move) {

				deleteDirectory(source.toPath());

				logger.info("MOVED SUCCESSFULLY -> {}", folder.folderName);

			} else {

				logger.info("COPIED SUCCESSFULLY -> {}", folder.folderName);
			}

		} catch (Exception e) {

			logger.error("ERROR processing folder: {}", folder.folderName, e);
		}
	}

	// ==============================================================
	// COPY OR MOVE INVALID FOLDER
	// ==============================================================

	private static void copyOrMoveErrorFolder(ErrorDetails error, File destinationFolder, boolean move) {

		try {

			File source = new File(error.folderPath);

			File destination = new File(destinationFolder, error.folderName);

			if (!source.exists()) {

				logger.error("Source error folder does not exist: {}", source.getAbsolutePath());

				return;
			}

			if (!source.isDirectory()) {

				logger.error("Source error path is not a directory: {}", source.getAbsolutePath());

				return;
			}

			if (destination.exists()) {

				logger.warn("Destination already exists: {}", destination.getAbsolutePath());

				return;
			}

			logger.info("PROCESSING INVALID FOLDER: {}", error.folderName);

			logger.info("Customer ID: {}", error.customerId);

			logger.info("Customer Name: {}", error.customerName);

			logger.info("Error Reason: {}", error.errorReason);

			logger.info("FROM: {}", source.getAbsolutePath());

			logger.info("TO: {}", destination.getAbsolutePath());

			copyDirectory(source.toPath(), destination.toPath());

			if (!destination.exists()) {

				throw new IOException("Destination error folder was not created.");
			}

			if (move) {

				deleteDirectory(source.toPath());

				logger.info("MOVED INVALID FOLDER SUCCESSFULLY -> {}", error.folderName);

			} else {

				logger.info("COPIED INVALID FOLDER SUCCESSFULLY -> {}", error.folderName);
			}

		} catch (Exception e) {

			logger.error("ERROR processing invalid folder: {}", error.folderName, e);
		}
	}

	// ==============================================================
	// COPY DIRECTORY
	// ==============================================================

	private static void copyDirectory(Path source, Path destination) throws IOException {

		try {

			Files.walk(source).forEach(sourcePath -> {

				try {

					Path relativePath = source.relativize(sourcePath);

					Path targetPath = destination.resolve(relativePath);

					if (Files.isDirectory(sourcePath)) {

						Files.createDirectories(targetPath);

					} else {

						if (targetPath.getParent() != null) {

							Files.createDirectories(targetPath.getParent());
						}

						Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
					}

				} catch (IOException e) {

					throw new RuntimeException("Failed to copy: " + sourcePath, e);
				}
			});

		} catch (RuntimeException e) {

			if (e.getCause() instanceof IOException) {

				throw (IOException) e.getCause();
			}

			throw e;
		}
	}

	// ==============================================================
	// DELETE DIRECTORY
	// ==============================================================

	private static void deleteDirectory(Path directory) throws IOException {

		if (!Files.exists(directory)) {

			return;
		}

		try {

			Files.walk(directory).sorted(Comparator.reverseOrder()).forEach(path -> {

				try {

					Files.delete(path);

				} catch (IOException e) {

					throw new RuntimeException("Failed to delete: " + path, e);
				}
			});

		} catch (RuntimeException e) {

			if (e.getCause() instanceof IOException) {

				throw (IOException) e.getCause();
			}

			throw e;
		}
	}

	// ==============================================================
	// CREATE DIRECTORY
	// ==============================================================

	private static void createDirectory(File directory, String description) {

		if (directory.exists()) {

			if (directory.isDirectory()) {

				logger.info("{} already exists: {}", description, directory.getAbsolutePath());

			} else {

				logger.error("{} path exists but is not a directory: {}", description, directory.getAbsolutePath());
			}

			return;
		}

		if (directory.mkdirs()) {

			logger.info("Created {}: {}", description, directory.getAbsolutePath());

		} else {

			logger.error("Unable to create {}: {}", description, directory.getAbsolutePath());
		}
	}

	// ==============================================================
	// COUNT MIGRATION EXCEL CUSTOMERS
	// ==============================================================

	private static int countMigrationExcelCustomers(String cbExcelPath, String pbExcelPath) {

		Set<String> customerIds = new HashSet<>();

		logger.info("Reading CB migration Excel for customer count.");

		readMigrationExcelCustomerIds(cbExcelPath, customerIds);

		logger.info("Reading PB migration Excel for customer count.");

		readMigrationExcelCustomerIds(pbExcelPath, customerIds);

		logger.info("Total UNIQUE migration Excel customers: {}", customerIds.size());

		return customerIds.size();
	}

	// ==============================================================
	// READ MIGRATION EXCEL CUSTOMER IDS
	// ==============================================================

	private static void readMigrationExcelCustomerIds(String excelPath, Set<String> customerIds) {

		if (isBlank(excelPath)) {

			logger.warn("Migration Excel path is empty. Skipping.");

			return;
		}

		File excelFile = new File(excelPath);

		if (!excelFile.exists()) {

			logger.warn("Migration Excel file not found: {}", excelFile.getAbsolutePath());

			return;
		}

		if (!excelFile.isFile()) {

			logger.error("Migration Excel path is not a file: {}", excelFile.getAbsolutePath());

			return;
		}

		logger.info("Reading migration Excel for customer count: {}", excelFile.getAbsolutePath());

		try (FileInputStream fis = new FileInputStream(excelFile);

				XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

			if (workbook.getNumberOfSheets() == 0) {

				logger.warn("No sheets found in migration Excel: {}", excelFile.getName());

				return;
			}

			Sheet sheet = workbook.getSheetAt(0);

			DataFormatter formatter = new DataFormatter();

			Row headerRow = sheet.getRow(0);

			if (headerRow == null) {

				logger.warn("No header row found in migration Excel: {}", excelFile.getName());

				return;
			}

			int customerIdColumn = -1;

			for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {

				String header = formatter.formatCellValue(headerRow.getCell(cellIndex)).trim();

				if ("customer_id".equalsIgnoreCase(header) || "customer id".equalsIgnoreCase(header)) {

					customerIdColumn = cellIndex;

					break;
				}
			}

			if (customerIdColumn == -1) {

				logger.error("Customer ID column not found in migration Excel: {}", excelFile.getAbsolutePath());

				return;
			}

			for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {

				Row row = sheet.getRow(rowNum);

				if (row == null) {

					continue;
				}

				String customerId = formatter.formatCellValue(row.getCell(customerIdColumn)).trim();

				if (!customerId.isEmpty()) {

					customerIds.add(customerId);
				}
			}

			logger.info("Migration Excel processed: {} | Unique IDs so far: {}", excelFile.getName(),
					customerIds.size());

		} catch (Exception e) {

			logger.error("Error reading migration Excel for customer count: {}", excelFile.getAbsolutePath(), e);
		}
	}

	// ==============================================================
	// GENERATE EXCEL REPORT
	// ==============================================================

	private static void generateExcel(String outputPath, List<FolderDetails> originalFolders,
			List<FolderDetails> duplicateFolders, List<ErrorDetails> errorFolders,
			List<FolderDetails> excelErrorFolders, File originalMigrateFolder, File excelValidationErrorFolder,
			File errorFolder, int totalWindowsFolders, int totalMigrationExcelCustomers, int totalMigrationErrorFolders,
			int totalExcelValidationErrors) {

		logger.info("====================================================");

		logger.info("STARTING FOLDER VALIDATION EXCEL GENERATION");

		logger.info("Output Excel: {}", outputPath);

		logger.info("====================================================");

		try (Workbook workbook = new XSSFWorkbook()) {

			// ======================================================
			// SUMMARY SHEET
			// ======================================================

			logger.info("Creating Excel sheet: Summary");

			Sheet summarySheet = workbook.createSheet("Summary");

			Row summaryHeader = summarySheet.createRow(0);

			summaryHeader.createCell(0).setCellValue("Validation Metric");

			summaryHeader.createCell(1).setCellValue("Count");

			summaryHeader.createCell(2).setCellValue("Description");

			int summaryRow = 1;

			addSummaryRow(summarySheet, summaryRow++, "Initial Windows Folders", totalWindowsFolders,
					"Total folders found in source folder before validation");

			addSummaryRow(summarySheet, summaryRow++, "Migration Excel Customers", totalMigrationExcelCustomers,
					"Unique Customer IDs found in CB and PB migration Excel files");

			addSummaryRow(summarySheet, summaryRow++, "Excel Validation Error Folders", totalExcelValidationErrors,
					"Customer folders moved because of Excel pre-validation errors");

			addSummaryRow(summarySheet, summaryRow++, "Original-Migrate Folders", originalFolders.size(),
					"Valid unique Customer ID folders");

			addSummaryRow(summarySheet, summaryRow++, "Duplicate Folders", duplicateFolders.size(),
					"Folders having duplicate Customer IDs");

			addSummaryRow(summarySheet, summaryRow++, "Invalid Folder Errors", errorFolders.size(),
					"Folders having missing Customer ID or Customer Name");

			addSummaryRow(summarySheet, summaryRow++, "Total Windows Validation Errors", totalMigrationErrorFolders,
					"Duplicate folders plus invalid folder errors");

			addSummaryRow(summarySheet, summaryRow++, "Total Error Folders",
					totalMigrationErrorFolders + totalExcelValidationErrors,
					"Windows validation errors plus Excel pre-validation errors");

			// ======================================================
			// ORIGINAL-MIGRATE SHEET
			// ======================================================

			logger.info("Creating Excel sheet: Original-Migrate");

			Sheet originalSheet = workbook.createSheet("Original-Migrate");

			Row originalHeader = originalSheet.createRow(0);

			originalHeader.createCell(0).setCellValue("Customer ID");

			originalHeader.createCell(1).setCellValue("Customer Name");

			originalHeader.createCell(2).setCellValue("Folder Name");

			originalHeader.createCell(3).setCellValue("Folder Path");

			int rowNumber = 1;

			for (FolderDetails folder : originalFolders) {

				Row row = originalSheet.createRow(rowNumber++);

				row.createCell(0).setCellValue(folder.customerId);

				row.createCell(1).setCellValue(folder.customerName);

				row.createCell(2).setCellValue(folder.folderName);

				row.createCell(3).setCellValue(new File(originalMigrateFolder, folder.folderName).getAbsolutePath());
			}

			// ======================================================
			// ERROR-FOLDERS SHEET
			// ======================================================

			logger.info("Creating Excel sheet: Error-Folders");

			Sheet errorSheet = workbook.createSheet("Error-Folders");

			Row errorHeader = errorSheet.createRow(0);

			errorHeader.createCell(0).setCellValue("Customer ID");

			errorHeader.createCell(1).setCellValue("Customer Name");

			errorHeader.createCell(2).setCellValue("Folder Name");

			errorHeader.createCell(3).setCellValue("Folder Path");

			errorHeader.createCell(4).setCellValue("Error Reason");

			rowNumber = 1;

			for (FolderDetails folder : duplicateFolders) {

				Row row = errorSheet.createRow(rowNumber++);

				row.createCell(0).setCellValue(folder.customerId);

				row.createCell(1).setCellValue(folder.customerName);

				row.createCell(2).setCellValue(folder.folderName);

				row.createCell(3).setCellValue(new File(errorFolder, folder.folderName).getAbsolutePath());

				row.createCell(4).setCellValue(folder.duplicateReason);
			}

			for (ErrorDetails error : errorFolders) {

				Row row = errorSheet.createRow(rowNumber++);

				row.createCell(0).setCellValue(error.customerId);

				row.createCell(1).setCellValue(error.customerName);

				row.createCell(2).setCellValue(error.folderName);

				row.createCell(3).setCellValue(new File(errorFolder, error.folderName).getAbsolutePath());

				row.createCell(4).setCellValue(error.errorReason);
			}

			// ======================================================
			// EXCEL VALIDATION ERROR SHEET
			// ======================================================

			logger.info("Creating Excel sheet: Excel-Validation-Errors");

			Sheet excelErrorSheet = workbook.createSheet("Excel-Validation-Errors");

			Row excelErrorHeader = excelErrorSheet.createRow(0);

			excelErrorHeader.createCell(0).setCellValue("Customer ID");

			excelErrorHeader.createCell(1).setCellValue("Customer Name");

			excelErrorHeader.createCell(2).setCellValue("Folder Name");

			excelErrorHeader.createCell(3).setCellValue("Folder Path");

			excelErrorHeader.createCell(4).setCellValue("Validation");

			excelErrorHeader.createCell(5).setCellValue("Error Reason");

			rowNumber = 1;

			for (FolderDetails folder : excelErrorFolders) {

				Row row = excelErrorSheet.createRow(rowNumber++);

				row.createCell(0).setCellValue(folder.customerId);

				row.createCell(1).setCellValue(folder.customerName);

				row.createCell(2).setCellValue(folder.folderName);

				row.createCell(3)
						.setCellValue(new File(excelValidationErrorFolder, folder.folderName).getAbsolutePath());

				row.createCell(4).setCellValue("Excel Pre-Validation");

				row.createCell(5).setCellValue("Excel Pre-Validation Failed");
			}

			// ======================================================
			// AUTO SIZE COLUMNS
			// ======================================================

			logger.info("Auto-sizing Excel columns.");

			for (int i = 0; i < 3; i++) {

				summarySheet.autoSizeColumn(i);
			}

			for (int i = 0; i < 4; i++) {

				originalSheet.autoSizeColumn(i);
			}

			for (int i = 0; i < 5; i++) {

				errorSheet.autoSizeColumn(i);
			}

			for (int i = 0; i < 6; i++) {

				excelErrorSheet.autoSizeColumn(i);
			}

			// ======================================================
			// CREATE OUTPUT DIRECTORY
			// ======================================================

			File outputFile = new File(outputPath);

			File parent = outputFile.getParentFile();

			if (parent != null && !parent.exists()) {

				logger.info("Creating Excel output directory: {}", parent.getAbsolutePath());

				if (!parent.mkdirs() && !parent.exists()) {

					throw new IOException("Unable to create Excel output directory: " + parent.getAbsolutePath());
				}
			}

			// ======================================================
			// WRITE EXCEL
			// ======================================================

			logger.info("Writing Folder Validation Excel file.");

			try (FileOutputStream fos = new FileOutputStream(outputFile)) {

				workbook.write(fos);
			}

			logger.info("Folder Validation Excel created successfully: {}", outputFile.getAbsolutePath());

			logger.info("Excel sheets generated: Summary, Original-Migrate, Error-Folders, Excel-Validation-Errors");

			logger.info("Excel report generation SUCCESS.");

		} catch (IOException e) {

			logger.error("ERROR creating Folder Validation Excel: {}", outputPath, e);

		} catch (Exception e) {

			logger.error("UNEXPECTED ERROR during Excel generation: {}", outputPath, e);
		}

		logger.info("FOLDER VALIDATION EXCEL GENERATION COMPLETED.");
	}

	// ==============================================================
	// ADD SUMMARY ROW
	// ==============================================================

	private static void addSummaryRow(Sheet sheet, int rowNumber, String metric, int count, String description) {

		Row row = sheet.createRow(rowNumber);

		row.createCell(0).setCellValue(metric);

		row.createCell(1).setCellValue(count);

		row.createCell(2).setCellValue(description);
	}

	// ==============================================================
	// CHECK BLANK
	// ==============================================================

	private static boolean isBlank(String value) {

		return value == null || value.trim().isEmpty();
	}

	// ==============================================================
	// FOLDER DETAILS
	// ==============================================================

	static class FolderDetails {

		String customerId;

		String customerName;

		String folderName;

		String folderPath;

		String duplicateReason;

		FolderDetails(String customerId, String customerName, String folderName, String folderPath) {

			this.customerId = customerId;

			this.customerName = customerName;

			this.folderName = folderName;

			this.folderPath = folderPath;
		}
	}

	// ==============================================================
	// ERROR DETAILS
	// ==============================================================

	static class ErrorDetails {

		String customerId;

		String customerName;

		String folderName;

		String folderPath;

		String errorReason;

		ErrorDetails(String customerId, String customerName, String folderName, String folderPath, String errorReason) {

			this.customerId = customerId;

			this.customerName = customerName;

			this.folderName = folderName;

			this.folderPath = folderPath;

			this.errorReason = errorReason;
		}
	}
}