package com.esun.fn.main;

import java.lang.System.Logger;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.config.ConfigReader;
import com.esun.fn.connection.FN_Connection;
import com.esun.fn.datamodel.ESUNMetaData;
import com.esun.fn.datamodel.ReportData;
import com.esun.fn.impl.FolderMigrationImpl;
import com.esun.fn.operation.FolderMigration;
import com.esun.fn.operation.ReadExcelOperation;
import com.filenet.api.core.ObjectStore;

public class Main {

	private static final Logger logger = Logger.getLogger(Main.class);

	public static void main(String[] args) {

		URL url = Main.class.getClassLoader().getResource("log4j.properties");

		if (url == null) {
			throw new RuntimeException("log4j.properties not found.");
		}

		PropertyConfigurator.configure(url);

		logger.info("======================================");
		logger.info(" ESUN FileNet Migration Started ");
		logger.info("======================================");

		FN_Connection connection = new FN_Connection();

		try {

			// Connect to FileNet
			ObjectStore os = connection.connect();

			if (os == null) {

				logger.error("ObjectStore is NULL. Unable to continue migration.");
				return;

			}

			// Read Metadata Excel
			ReadExcelOperation excelOperation = new ReadExcelOperation();

			List<ESUNMetaData> metadataList = excelOperation.readExcel();

			// Prepare Report
			List<ReportData> reportList = new ArrayList<>();

			for (ESUNMetaData data : metadataList) {

				ReportData report = new ReportData();

				report.setCustomerId(data.getCustomerId());
				report.setCustomerName(data.getCustomerName());
				report.setFolderCount(0);
				report.setDocumentCount(0);
				report.setStatus("Pending");
				report.setError("");

				reportList.add(report);

			}

			// Write Initial Report
			excelOperation.writeReport(reportList);

			// Create Migration Object
			FolderMigration migration = new FolderMigrationImpl();

			// Start Migration
			migration.migrate(os, ConfigReader.getProperty("source.root"), ConfigReader.getProperty("target.folder"));

			logger.info("Migration Completed Successfully.");

		} catch (Exception e) {

			logger.error("Main Error : ", e);

		} finally {

			try {

				connection.disconnect();

				logger.info("Disconnected from FileNet.");

			} catch (Exception ex) {

				logger.error("Error while disconnecting.", ex);

			}

			logger.info("======================================");
			logger.info(" ESUN FileNet Migration Finished ");
			logger.info("======================================");

		}

	}

}