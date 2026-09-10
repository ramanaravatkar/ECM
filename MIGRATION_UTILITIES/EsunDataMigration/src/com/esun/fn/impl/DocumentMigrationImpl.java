package com.esun.fn.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.config.ConfigReader;
import com.esun.fn.connection.FileNetConnection;
import com.esun.fn.datamodel.ESUNMetaData;
import com.esun.fn.operation.CeOperations;
import com.esun.fn.operation.ExcelOperations;
import com.esun.fn.operation.ExcelReportWriterImpl;
import com.esun.fn.operation.FileOperations;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;

public class DocumentMigrationImpl {

	private static final Logger logger = LogManager.getLogger(DocumentMigrationImpl.class);

	public void mainLuncher() {

		logger.info("Document migration started");

		FileNetConnection fnConnection = new FileNetConnection();

		try {

			ExcelOperations xlOps = new ExcelOperations();
			ExcelReportWriterImpl excelReportWriterImpl = new ExcelReportWriterImpl();
			CeOperations ceOps = new CeOperations();
			FileOperations fileOperation = new FileOperations();

			// ==========================================================
			// FILENET CONNECTION
			// ==========================================================

			ObjectStore osInstance = fnConnection.getFileNetConnection();

			if (osInstance == null) {

				logger.error("Unable to connect to FileNet Object Store.");
				return;
			}

			logger.info("Connected to Object Store: {}", osInstance.get_DisplayName());

			// ==========================================================
			// EXCEL FILE PATHS
			// ==========================================================

			String cbExcelPath = ConfigReader.getProperty("cb.excel.path");
			String pbExcelPath = ConfigReader.getProperty("pb.excel.path");
			logger.info("CB migration Excel path: {}", cbExcelPath);
			logger.info("PB migration Excel path: {}", pbExcelPath);

			if (cbExcelPath == null || cbExcelPath.trim().isEmpty()) {

				logger.error("cb.excel.path is missing in configuration.");
				return;
			}

			if (pbExcelPath == null || pbExcelPath.trim().isEmpty()) {

				logger.error("pb.excel.path is missing in configuration.");
				return;
			}

			File cbExcelFile = new File(cbExcelPath.trim());
			File pbExcelFile = new File(pbExcelPath.trim());

			// ==========================================================
			// CHECK CB EXCEL
			// ==========================================================

			if (!cbExcelFile.exists()) {

				logger.error("CB Excel file not found: {}", cbExcelFile.getAbsolutePath());

			} else {

				logger.info("CB Excel file found: {}", cbExcelFile.getAbsolutePath());
			}

			// ==========================================================
			// CHECK PB EXCEL
			// ==========================================================

			if (!pbExcelFile.exists()) {

				logger.error("PB Excel file not found: {}", pbExcelFile.getAbsolutePath());

			} else {

				logger.info("PB Excel file found: {}", pbExcelFile.getAbsolutePath());
			}

			if (!cbExcelFile.exists() && !pbExcelFile.exists()) {

				logger.error("Neither CB nor PB migration Excel file exists.");
				return;
			}

			// ==========================================================
			// READ CB METADATA
			// ==========================================================

			List<ESUNMetaData> cbMetadata = new ArrayList<>();

			if (cbExcelFile.exists()) {

				logger.info("Reading CB metadata Excel...");

				cbMetadata = xlOps.readMetaDataInfoFrmExcel(cbExcelFile);

				logger.info("CB customers loaded from Excel: {}", cbMetadata.size());

			} else {

				logger.warn("Skipping CB Excel because file does not exist.");
			}

			// ==========================================================
			// READ PB METADATA
			// ==========================================================

			List<ESUNMetaData> pbMetadata = new ArrayList<>();

			if (pbExcelFile.exists()) {

				logger.info("Reading PB metadata Excel...");

				pbMetadata = xlOps.readMetaDataInfoFrmExcel(pbExcelFile);

				logger.info("PB customers loaded from Excel: {}", pbMetadata.size());

			} else {

				logger.warn("Skipping PB Excel because file does not exist.");
			}

			// ==========================================================
			// COMBINE CB + PB
			// REMOVE DUPLICATE CUSTOMER IDs
			// ==========================================================

			Map<String, ESUNMetaData> uniqueCustomerMap = new LinkedHashMap<>();

			// -----------------------------
			// Add CB customers
			// -----------------------------

			for (ESUNMetaData metadata : cbMetadata) {

				if (metadata == null) {
					continue;
				}

				String customerId = metadata.getCustomerId();

				if (customerId == null || customerId.trim().isEmpty()) {

					logger.warn("Skipping CB customer because Customer ID is empty.");
					continue;
				}

				customerId = customerId.trim();

				if (uniqueCustomerMap.containsKey(customerId)) {

					logger.error(
							"Duplicate Customer ID found in CB metadata: {}. " + "Customer will not be added twice.",
							customerId);

				} else {

					uniqueCustomerMap.put(customerId, metadata);

					logger.info("CB customer added to migration list: {} - {}", customerId, metadata.getCustomerName());
				}
			}

			// -----------------------------
			// Add PB customers
			// -----------------------------

			for (ESUNMetaData metadata : pbMetadata) {

				if (metadata == null) {
					continue;
				}

				String customerId = metadata.getCustomerId();

				if (customerId == null || customerId.trim().isEmpty()) {

					logger.warn("Skipping PB customer because Customer ID is empty.");
					continue;
				}

				customerId = customerId.trim();

				if (uniqueCustomerMap.containsKey(customerId)) {

					logger.error("Duplicate Customer ID found between CB and PB Excel files: {}. "
							+ "Customer will NOT be processed twice.", customerId);

				} else {

					uniqueCustomerMap.put(customerId, metadata);

					logger.info("PB customer added to migration list: {} - {}", customerId, metadata.getCustomerName());
				}
			}

			// ==========================================================
			// FINAL UNIQUE CUSTOMER LIST
			// ==========================================================

			List<ESUNMetaData> metaDataList = new ArrayList<>(uniqueCustomerMap.values());

			logger.info("Total UNIQUE customers loaded from CB + PB Excel files: {}", metaDataList.size());

			if (metaDataList.isEmpty()) {

				logger.warn("No customer records available for FileNet migration.");

				logger.warn(
						"Please verify that the CB/PB Migration_Data.xlsx files " + "contain valid customer records.");

				return;
			}

			// ==========================================================
			// TARGET FILENET ROOT FOLDER
			// ==========================================================

			String rootCeFolder = ConfigReader.getProperty("target.folder");

			if (rootCeFolder == null || rootCeFolder.trim().isEmpty()) {

				logger.error("target.folder is missing in configuration.");
				return;
			}

			logger.info("Target CE root folder: {}", rootCeFolder);

			// ==========================================================
			// SOURCE ROOT
			// ==========================================================

			String sourceRoot = ConfigReader.getProperty("source.root");

			if (sourceRoot == null || sourceRoot.trim().isEmpty()) {

				logger.error("source.root is missing in configuration.");
				return;
			}

			logger.info("Source root: {}", sourceRoot);

			// ==========================================================
			// PROCESSED LOCATION
			// ==========================================================

			String processedLocation = ConfigReader.getProperty("processed.location");

			if (processedLocation == null || processedLocation.trim().isEmpty()) {

				logger.error("processed.location is missing in configuration.");
				return;
			}

			if (!processedLocation.endsWith(File.separator)) {

				processedLocation = processedLocation + File.separator;
			}

			// ==========================================================
			// COMPLETED MIGRATION FOLDER
			// CB-PROCESSED
			// |
			// +-- Completed Migration
			// |
			// +-- Customer 1
			// +-- Customer 2
			// ==========================================================

			String completedMigrationLocation = processedLocation + "Completed Migration";

			logger.info("Completed migration location: {}", completedMigrationLocation);

			fileOperation.createLocalDataFolder(completedMigrationLocation);

			// ==========================================================
			// MIGRATION
			// ==========================================================

			JSONArray totalCustomerData = new JSONArray();

			List<File> foldersToDelete = new ArrayList<>();

			for (ESUNMetaData rowData : metaDataList) {

				int folderCount = 0;
				int documentCount = 0;

				logger.info("==============================================");
				logger.info("Processing customer: {} - {}", rowData.getCustomerId(), rowData.getCustomerName());
				logger.info("==============================================");

				// ======================================================
				// SOURCE CUSTOMER FOLDER
				// ======================================================

				File dataFolder = new File(
						sourceRoot + File.separator + rowData.getCustomerId() + " - " + rowData.getCustomerName());

				logger.info("Customer source folder: {}", dataFolder.getAbsolutePath());

				if (!dataFolder.exists()) {

					logger.warn("Customer source folder not found: {}", dataFolder.getAbsolutePath());

					continue;
				}

				if (!dataFolder.isDirectory()) {

					logger.warn("Customer source path is not a directory: {}", dataFolder.getAbsolutePath());

					continue;
				}

				// ======================================================
				// COMPLETED MIGRATION CUSTOMER LOCATION
				// ======================================================

				String dataFolderLocation = completedMigrationLocation + File.separator + dataFolder.getName();

				String successFolderLocation = dataFolderLocation + File.separator + "SUCCESS";

				String failureFolderLocation = dataFolderLocation + File.separator + "FAILURE";

				logger.info("Completed customer location: {}", dataFolderLocation);

				logger.info("Success folder location: {}", successFolderLocation);

				logger.info("Failure folder location: {}", failureFolderLocation);

				fileOperation.createLocalDataFolder(dataFolderLocation);

				fileOperation.createLocalDataFolder(successFolderLocation);

				fileOperation.createLocalDataFolder(failureFolderLocation);

				// ======================================================
				// GET FILENET ROOT FOLDER
				// ======================================================

				Folder ceRootFolder = ceOps.getFolderInstance(osInstance, rootCeFolder);

				if (ceRootFolder == null) {

					logger.error("Unable to locate CE root folder: {}", rootCeFolder);

					continue;
				}

				// ======================================================
				// CUSTOMER FILENET FOLDER
				// ======================================================

				String customerFolderPath = rootCeFolder + "/" + dataFolder.getName();

				logger.info("Customer CE folder path: {}", customerFolderPath);

				Folder customerFolder = ceOps.getFolderInstance(osInstance, customerFolderPath);

				String createdFolderId;

				if (customerFolder != null) {

					logger.info("Customer folder already exists in FileNet: {}", customerFolderPath);

					createdFolderId = customerFolder.get_Id().toString();

				} else {

					logger.info("Creating customer folder in FileNet: {}", dataFolder.getName());

					createdFolderId = ceOps.createFolderInCe(osInstance, ceRootFolder, dataFolder.getName());
				}

				if (createdFolderId == null || createdFolderId.trim().isEmpty()) {

					logger.error("Unable to create or locate customer folder in FileNet: {}", dataFolder.getName());

					continue;
				}

				logger.info("Customer CE folder ID: {}", createdFolderId);

				Folder parentFolder = ceOps.fetchFolderInstance(osInstance, createdFolderId);

				if (parentFolder == null) {

					logger.error("Unable to fetch customer CE folder instance: {}", createdFolderId);

					continue;
				}

				// ======================================================
				// READ CUSTOMER SOURCE FOLDER
				// ======================================================

				logger.info("Actual customer source path: {}", dataFolder.getAbsolutePath());

				File[] dataFolderList = dataFolder.listFiles();

				if (dataFolderList == null) {

					logger.warn("Unable to read contents of customer folder: {}", dataFolder.getAbsolutePath());

					continue;
				}

				logger.info("Items directly under customer folder: {}", dataFolderList.length);

				for (File item : dataFolderList) {

					logger.info("Source item -> Name: {} | Directory: {} | File: {}", item.getName(),
							item.isDirectory(), item.isFile());
				}

				// ======================================================
				// FILE COUNTS
				// ======================================================

				int rootSuccessCount = 0;
				int rootFailureCount = 0;

				int totalRootFiles = FileOperations.getRootFolderFileCount(dataFolder);

				int totalFiles = FileOperations.getTotalFileDetails(dataFolder);

				logger.info("Root level files found: {}", totalRootFiles);

				logger.info("Total documents under customer folder: {}", totalFiles);

				// ======================================================
				// CUSTOMER REPORT OBJECT
				// ======================================================

				JSONObject singleCustomerInfo = new JSONObject();

				JSONObject customerInfo = new JSONObject();

				JSONArray foldersInfo = new JSONArray();

				customerInfo.put("customerid", rowData.getCustomerId());

				customerInfo.put("customerName", rowData.getCustomerName());

				// IMPORTANT:
				// Use ALL documents, not only root-level files.
				customerInfo.put("totalFiles", totalFiles);

				// ======================================================
				// PROCESS CUSTOMER CONTENT
				// ======================================================

				for (File processingData : dataFolderList) {

					// ==================================================
					// SUBFOLDER
					// ==================================================

					if (processingData.isDirectory()) {

						logger.info("Processing source subfolder: {}", processingData.getName());

						String folderPath = customerFolderPath + "/" + processingData.getName();

						logger.info("Target CE subfolder path: {}", folderPath);

						Folder existingFolder = ceOps.getFolderInstance(osInstance, folderPath);

						String subfolderId;

						if (existingFolder != null) {

							subfolderId = existingFolder.get_Id().toString();

							logger.info("Subfolder already exists in FileNet: {}", processingData.getName());

						} else {

							logger.info("Creating FileNet subfolder: {}", processingData.getName());

							subfolderId = ceOps.createFolderInCe(osInstance, parentFolder, processingData.getName());
						}

						if (subfolderId == null || subfolderId.trim().isEmpty()) {

							logger.error("Unable to create or locate FileNet subfolder: {}", processingData.getName());

							continue;
						}

						Folder createdFolderInstance = ceOps.fetchFolderInstance(osInstance, subfolderId);

						if (createdFolderInstance == null) {

							logger.error("Unable to fetch FileNet subfolder instance: {}", processingData.getName());

							continue;
						}

						folderCount++;

						logger.info("Created/found CE subfolder: {}", createdFolderInstance.get_FolderName());

						int uploadedDocuments = xlOps.iterateDirectory(processingData.getName(), osInstance,
								processingData.getAbsolutePath(), createdFolderInstance, rowData, foldersInfo,
								successFolderLocation, failureFolderLocation,
								dataFolder.getName() + "/" + processingData.getName());

						documentCount += uploadedDocuments;

						logger.info("Documents processed from subfolder {}: {}", processingData.getName(),
								uploadedDocuments);
					}

					// ==================================================
					// ROOT LEVEL DOCUMENT
					// ==================================================

					else if (processingData.isFile()) {

						logger.info("Uploading root level document: {}", processingData.getName());

						boolean docCreationStatus = ceOps.uploadDocument(osInstance, parentFolder, processingData,
								rowData);

						if (docCreationStatus) {

							fileOperation.moveProcessedFile(processingData, dataFolder, successFolderLocation);

							rootSuccessCount++;
							documentCount++;

							logger.info("Root level document uploaded successfully: {}", processingData.getName());

						} else {

							fileOperation.moveProcessedFile(processingData, dataFolder, failureFolderLocation);

							rootFailureCount++;

							logger.error("Root level document upload failed: {}", processingData.getName());
						}
					}
				}

				// ======================================================
				// CUSTOMER REPORT DATA
				// ======================================================

				customerInfo.put("SuccessFilesCount", rootSuccessCount);

				customerInfo.put("FailureFilesCount", rootFailureCount);

				singleCustomerInfo.put("CustomerInfo", customerInfo);

				singleCustomerInfo.put("folderInfo", foldersInfo);

				totalCustomerData.put(singleCustomerInfo);

				// ======================================================
				// DELETE ORIGINAL SOURCE AFTER PROCESSING
				//
				// The processed copy remains in:
				//
				// CB-PROCESSED
				// |
				// +-- Completed Migration
				//
				// Only queue the source folder for deletion once every file under it has actually
				// been relocated (moveProcessedFile can fail per-file, e.g. a locked file); leaving
				// any file behind cancels deletion so nothing is silently lost.
				// ======================================================

				int remainingFileCount = FileOperations.getTotalFileDetails(dataFolder);

				if (remainingFileCount == 0) {

					foldersToDelete.add(dataFolder);

				} else {

					logger.error(
							"Not deleting source folder for customer {} - {} files were not moved out of {}. "
									+ "Check for locked/failed files before removing it manually.",
							rowData.getCustomerId(), remainingFileCount, dataFolder.getAbsolutePath());
				}

				logger.info("Customer processing completed: {} - folders: {}, documents: {}", rowData.getCustomerId(),
						folderCount, documentCount);
			}

			// ==========================================================
			// REPORT
			// ==========================================================

			logger.info("Report data prepared for {} unique customers", totalCustomerData.length());

			String reportExcelPath = ConfigReader.getProperty("cb.excel.path");

			if (reportExcelPath != null && !reportExcelPath.trim().isEmpty()) {

				excelReportWriterImpl.writeReport(reportExcelPath, totalCustomerData);

				logger.info("Excel report generated successfully: {}", reportExcelPath);

			} else {

				logger.warn("CB Excel report path is not configured. " + "Report was not generated.");
			}

			// ==========================================================
			// DELETE ORIGINAL SOURCE CUSTOMER FOLDERS
			//
			// IMPORTANT:
			// This deletes ONLY the original source folder.
			//
			// The processed folder is already stored under:
			//
			// CB-PROCESSED/Completed Migration/
			//
			// ==========================================================

			for (File customerFolder : foldersToDelete) {

				boolean status = fileOperation.deleteFolder(customerFolder);

				if (status) {

					logger.info("Deleted original source customer folder: {}", customerFolder.getAbsolutePath());

				} else {

					logger.error("Failed to delete original source customer folder: {}",
							customerFolder.getAbsolutePath());
				}
			}

			logger.info("Document migration completed successfully");

		} catch (Exception e) {

			logger.error("Document migration failed", e);

		} finally {

			// Delegates to FileNetConnection.disconnect(), which already guards popSubject() with
			// its own try/catch: safe to call even if the connection was never established, and
			// won't mask the original failure above with a stack-pop error.
			fnConnection.disconnect();
		}
	}

	// ==============================================================
	// CREATE SUBFOLDER HIERARCHY
	// ==============================================================

	public String createSubfolderHiracy(ObjectStore osInstance, String subFolderName, String subFolderDirName,
			Folder ceRootFolder) {

		try {

			CeOperations ceOps = new CeOperations();

			logger.info("Verifying subfolder in FileNet: {}", subFolderDirName);

			Folder existingFolder = ceOps.getFolderInstance(osInstance, subFolderDirName);

			if (existingFolder != null) {

				logger.info("Subfolder already exists: {}", subFolderDirName);

				return existingFolder.get_Id().toString();
			}

			logger.info("Creating subfolder: {}", subFolderName);

			String subFolderId = ceOps.createFolderInCe(osInstance, ceRootFolder, subFolderName);

			if (subFolderId != null && !subFolderId.isEmpty()) {

				logger.info("Subfolder created successfully: {}", subFolderName);

			} else {

				logger.error("Failed to create subfolder: {}", subFolderName);
			}

			return subFolderId;

		} catch (Exception e) {

			logger.error("Error while creating subfolder: {}", subFolderName, e);

			return "";
		}
	}

	// ==============================================================
	// MAIN
	// ==============================================================

	public static void main(String[] args) {

		logger.info("ESUN Document Migration application started");

		try {

			DocumentMigrationImpl impl = new DocumentMigrationImpl();

			impl.mainLuncher();

			logger.info("ESUN Document Migration application completed successfully");

		} catch (Exception e) {

			logger.error("ESUN Document Migration application terminated with error", e);
		}
	}
}