package com.esun.fn.operation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.config.ConfigReader;

public class FileOperations {

	private static final Logger logger = LogManager.getLogger(FileOperations.class);

	public boolean moveFolder(File sourceFolder, String destinationRoot) {

		try {

			if (sourceFolder == null || !sourceFolder.exists()) {

				logger.error("Source folder does not exist: {}", sourceFolder);

				return false;
			}

			Files.move(sourceFolder.toPath(), Paths.get(destinationRoot), StandardCopyOption.REPLACE_EXISTING);

			logger.info("Folder moved successfully from {} to {}", sourceFolder.getAbsolutePath(), destinationRoot);

			return true;

		} catch (IOException e) {

			logger.error("Failed to move folder from {} to {}", sourceFolder.getAbsolutePath(), destinationRoot, e);

			return false;
		}
	}

	public void moveErrorCustomerFolders(File excelFile) {

		String sourceRootPath = ConfigReader.getProperty("source.root");
		String errorRootPath = ConfigReader.getProperty("error.location");

		File sourceRoot = new File(sourceRootPath);
		File errorRoot = new File(errorRootPath);

		if (!sourceRoot.exists()) {
			logger.error("Source root folder does not exist: {}", sourceRoot.getAbsolutePath());
			return;
		}

		if (!errorRoot.exists()) {
			if (errorRoot.mkdirs()) {
				logger.info("Created error folder: {}", errorRoot.getAbsolutePath());
			} else {
				logger.error("Unable to create error folder: {}", errorRoot.getAbsolutePath());
				return;
			}
		}

		logger.info("Starting automatic error customer folder movement");
		logger.info("Source: {}", sourceRoot.getAbsolutePath());
		logger.info("Error destination: {}", errorRoot.getAbsolutePath());

		try (FileInputStream fis = new FileInputStream(excelFile); XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

			XSSFSheet errorSheet = workbook.getSheet("Migration_Error_Data");

			if (errorSheet == null) {
				logger.error("Migration_Error_Data sheet not found in Excel: {}", excelFile.getAbsolutePath());
				return;
			}

			DataFormatter formatter = new DataFormatter();

			XSSFRow headerRow = errorSheet.getRow(0);

			if (headerRow == null) {
				logger.error("Migration_Error_Data sheet has no header row");
				return;
			}

			int customerIdColumn = -1;
			int customerNameColumn = -1;

			for (int cellIndex = 0; cellIndex < headerRow.getLastCellNum(); cellIndex++) {

				String header = formatter.formatCellValue(headerRow.getCell(cellIndex)).trim();

				if (header.equalsIgnoreCase("Customer ID") || header.equalsIgnoreCase("customer_id")) {

					customerIdColumn = cellIndex;
				}

				if (header.equalsIgnoreCase("Customer Name") || header.equalsIgnoreCase("customer_name")) {

					customerNameColumn = cellIndex;
				}
			}

			if (customerIdColumn == -1 || customerNameColumn == -1) {

				logger.error("Customer ID or Customer Name column not found in Migration_Error_Data");

				return;
			}

			Set<String> processedCustomers = new HashSet<>();

			for (int rowNum = 1; rowNum <= errorSheet.getLastRowNum(); rowNum++) {

				XSSFRow row = errorSheet.getRow(rowNum);

				if (row == null) {
					continue;
				}

				String customerId = formatter.formatCellValue(row.getCell(customerIdColumn)).trim();

				String customerName = formatter.formatCellValue(row.getCell(customerNameColumn)).trim();

				if (customerId.isEmpty() || customerName.isEmpty()) {

					logger.warn("Skipping error row {} because Customer ID or Customer Name is empty", rowNum + 1);

					continue;
				}

				String customerKey = customerId + "|" + customerName;

				// Same customer may appear multiple times in error report.
				// Move its folder only once.
				if (!processedCustomers.add(customerKey)) {

					logger.info("Customer already processed in error movement: {} - {}", customerId, customerName);

					continue;
				}

				String folderName = customerId + " - " + customerName;

				File sourceCustomerFolder = new File(sourceRoot, folderName);

				File destinationCustomerFolder = new File(errorRoot, folderName);

				logger.info("Checking error customer folder: {}", sourceCustomerFolder.getAbsolutePath());

				if (!sourceCustomerFolder.exists()) {

					logger.error("ERROR CUSTOMER FOLDER NOT FOUND | Customer ID: {} | Folder: {}", customerId,
							sourceCustomerFolder.getAbsolutePath());

					continue;
				}

				if (!sourceCustomerFolder.isDirectory()) {

					logger.error("ERROR CUSTOMER PATH IS NOT A DIRECTORY | Customer ID: {} | Path: {}", customerId,
							sourceCustomerFolder.getAbsolutePath());

					continue;
				}

				// If destination already exists, do not overwrite it.
				if (destinationCustomerFolder.exists()) {

					logger.warn("Error destination folder already exists. Folder will NOT be overwritten: {}",
							destinationCustomerFolder.getAbsolutePath());

					continue;
				}

				moveErrorCustomerFolder(customerId, sourceCustomerFolder, destinationCustomerFolder);
			}

			logger.info("Error customer folder movement completed");

		} catch (Exception e) {

			logger.error("Failed while processing Migration_Error_Data for folder movement", e);
		}
	}

	private void moveErrorCustomerFolder(String customerId, File sourceCustomerFolder, File destinationCustomerFolder)
			throws IOException {

		try {

			Files.move(sourceCustomerFolder.toPath(), destinationCustomerFolder.toPath(),
					StandardCopyOption.ATOMIC_MOVE);

			logger.info("ERROR CUSTOMER FOLDER MOVED SUCCESSFULLY | {} -> {}", sourceCustomerFolder.getAbsolutePath(),
					destinationCustomerFolder.getAbsolutePath());

		} catch (AtomicMoveNotSupportedException e) {

			// Fallback when atomic move is not supported. Any failure here is handled by the
			// caller's try-with-resources, matching the pre-existing behavior for this rare case.
			moveNonAtomically(sourceCustomerFolder, destinationCustomerFolder);

		} catch (Exception e) {

			logger.error("FAILED TO MOVE ERROR CUSTOMER FOLDER | Customer ID: {} | Folder: {}", customerId,
					sourceCustomerFolder.getAbsolutePath(), e);
		}
	}

	private void moveNonAtomically(File sourceCustomerFolder, File destinationCustomerFolder) throws IOException {

		Files.move(sourceCustomerFolder.toPath(), destinationCustomerFolder.toPath());

		logger.info("ERROR CUSTOMER FOLDER MOVED SUCCESSFULLY (fallback) | {} -> {}",
				sourceCustomerFolder.getAbsolutePath(), destinationCustomerFolder.getAbsolutePath());
	}

	public static int getTotalFileDetails(File folderFile) {

		int filesCount = 0;

		try {

			File[] files = folderFile.listFiles();

			if (files == null) {

				return 0;
			}

			for (File processingFile : files) {

				if (processingFile.isDirectory()) {

					filesCount += getTotalFileDetails(processingFile);

				} else {

					filesCount++;
				}
			}

		} catch (Exception e) {

			logger.error("Error while calculating total file count for folder: {}", folderFile.getAbsolutePath(), e);
		}

		return filesCount;
	}

	public static int getRootFolderFileCount(File folder) {

		int count = 0;

		try {

			File[] files = folder.listFiles();

			if (files == null) {

				return 0;
			}

			for (File file : files) {

				if (file.isFile()) {

					count++;
				}
			}

		} catch (Exception e) {

			logger.error("Error while calculating root folder file count for: {}", folder.getAbsolutePath(), e);
		}

		return count;
	}

	public boolean createLocalDataFolder(String folderPath) {

		try {

			File folder = new File(folderPath);

			if (folder.exists()) {

				return true;
			}

			boolean created = folder.mkdirs();

			if (created) {

				logger.info("Local folder created successfully: {}", folderPath);

			} else {

				logger.error("Failed to create local folder: {}", folderPath);
			}

			return created;

		} catch (Exception e) {

			logger.error("Error while creating local folder: {}", folderPath, e);

			return false;
		}
	}

	public boolean moveProcessedFile(File sourceFile, File customerRootFolder, String destinationRoot) {

		try {

			String relativePath = customerRootFolder.toPath().relativize(sourceFile.toPath()).toString();

			File destinationFile = new File(destinationRoot, relativePath);

			File parentFolder = destinationFile.getParentFile();

			if (!parentFolder.exists()) {

				parentFolder.mkdirs();
			}

			Files.move(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

			logger.info("File moved successfully from {} to {}", sourceFile.getAbsolutePath(),
					destinationFile.getAbsolutePath());

			return true;

		} catch (Exception e) {

			logger.error("Failed to move processed file: {}", sourceFile.getAbsolutePath(), e);

			return false;
		}
	}

	public boolean deleteFolder(File folder) {

		try {

			if (folder == null || !folder.exists()) {

				return true;
			}

			File[] files = folder.listFiles();

			if (files != null) {

				for (File file : files) {

					if (file.isDirectory()) {

						if (!deleteFolder(file)) {

							return false;
						}

					} else {

						Files.delete(file.toPath());
					}
				}
			}

			Files.delete(folder.toPath());

			logger.info("Folder deleted successfully: {}", folder.getAbsolutePath());

			return true;

		} catch (IOException e) {

			logger.error("Error while deleting folder: {}", folder.getAbsolutePath(), e);

			return false;
		}
	}
}