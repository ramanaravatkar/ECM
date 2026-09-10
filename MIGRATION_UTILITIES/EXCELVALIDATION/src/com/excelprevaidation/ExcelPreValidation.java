package com.excelprevaidation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.configreader.ConfigReader;

public class ExcelPreValidation {

	private static final Logger logger = LogManager.getLogger(ExcelPreValidation.class);

	private static final DataFormatter FORMATTER = new DataFormatter();

	public static void main(String[] args) {

		System.out.println("====================================================");
		System.out.println("       ESUN Excel Pre-Validation Started");
		System.out.println("====================================================");

		boolean cbSuccess = false;
		boolean pbSuccess = false;

		try {

			ExcelPreValidation validator = new ExcelPreValidation();

			/*
			 * ============================================================ CB
			 * PRE-VALIDATION ============================================================
			 */

			System.out.println();
			System.out.println("====================================================");
			System.out.println("       CORPORATE BANKING (CB) PRE-VALIDATION");
			System.out.println("====================================================");

			String cbInputPath = ConfigReader.getProperty("cb.input.excel.path");
			String cbMigrationPath = ConfigReader.getProperty("cb.migration.excel.path");
			String cbErrorPath = ConfigReader.getProperty("cb.error.excel.path");

			validateConfigurationProperty(cbInputPath, "cb.input.excel.path");
			validateConfigurationProperty(cbMigrationPath, "cb.migration.excel.path");
			validateConfigurationProperty(cbErrorPath, "cb.error.excel.path");

			File cbInputFile = new File(cbInputPath);
			File cbMigrationFile = new File(cbMigrationPath);
			File cbErrorFile = new File(cbErrorPath);

			System.out.println("CB Input Excel     : " + cbInputFile.getAbsolutePath());
			System.out.println("CB Migration Excel : " + cbMigrationFile.getAbsolutePath());
			System.out.println("CB Error Excel     : " + cbErrorFile.getAbsolutePath());

			if (!cbInputFile.exists()) {

				throw new IllegalArgumentException("CB Input Excel file not found: " + cbInputFile.getAbsolutePath());
			}

			validator.validateExcel(cbInputFile, cbMigrationFile, cbErrorFile, "CB");

			cbSuccess = true;

			System.out.println();
			System.out.println("CB Excel pre-validation completed successfully.");

		} catch (Exception e) {

			System.err.println();
			System.err.println("CB Excel pre-validation FAILED");
			e.printStackTrace();
		}

		try {

			ExcelPreValidation validator = new ExcelPreValidation();

			/*
			 * ============================================================ PB
			 * PRE-VALIDATION ============================================================
			 */

			System.out.println();
			System.out.println("====================================================");
			System.out.println("       PRIVATE BANKING (PB) PRE-VALIDATION");
			System.out.println("====================================================");

			String pbInputPath = ConfigReader.getProperty("pb.input.excel.path");
			String pbMigrationPath = ConfigReader.getProperty("pb.migration.excel.path");
			String pbErrorPath = ConfigReader.getProperty("pb.error.excel.path");

			validateConfigurationProperty(pbInputPath, "pb.input.excel.path");
			validateConfigurationProperty(pbMigrationPath, "pb.migration.excel.path");
			validateConfigurationProperty(pbErrorPath, "pb.error.excel.path");

			File pbInputFile = new File(pbInputPath);
			File pbMigrationFile = new File(pbMigrationPath);
			File pbErrorFile = new File(pbErrorPath);

			System.out.println("PB Input Excel     : " + pbInputFile.getAbsolutePath());
			System.out.println("PB Migration Excel : " + pbMigrationFile.getAbsolutePath());
			System.out.println("PB Error Excel     : " + pbErrorFile.getAbsolutePath());

			if (!pbInputFile.exists()) {

				throw new IllegalArgumentException("PB Input Excel file not found: " + pbInputFile.getAbsolutePath());
			}

			validator.validateExcel(pbInputFile, pbMigrationFile, pbErrorFile, "PB");

			pbSuccess = true;

			System.out.println();
			System.out.println("PB Excel pre-validation completed successfully.");

		} catch (Exception e) {

			System.err.println();
			System.err.println("PB Excel pre-validation FAILED");
			e.printStackTrace();
		}

		/*
		 * ============================================================ FINAL RESULT
		 * ============================================================
		 */

		System.out.println();
		System.out.println("====================================================");
		System.out.println("       ESUN Excel Pre-Validation Completed");
		System.out.println("====================================================");

		System.out.println("CB Pre-Validation : " + (cbSuccess ? "SUCCESS" : "FAILED"));
		System.out.println("PB Pre-Validation : " + (pbSuccess ? "SUCCESS" : "FAILED"));

		System.out.println("====================================================");
	}

	private static void validateConfigurationProperty(String value, String propertyName) {

		if (value == null || value.trim().isEmpty()) {

			throw new IllegalArgumentException(propertyName + " is missing in config.properties");
		}
	}

	public void validateExcel(File inputFile, File migrationFile, File errorFile, String businessUnitType)
			throws Exception {

		logger.info("Starting {} Excel pre-validation: {}", businessUnitType, inputFile.getAbsolutePath());

		try (FileInputStream fis = new FileInputStream(inputFile); Workbook workbook = new XSSFWorkbook(fis)) {

			Sheet sheet = workbook.getSheetAt(0);

			Map<String, Integer> headers = readHeaders(sheet);

			validateRequiredHeaders(headers);

			Map<String, List<Row>> customerGroups = groupByCustomerId(sheet, headers);

			List<Row> validRows = new ArrayList<>();

			List<ErrorRecord> errorRecords = new ArrayList<>();

			for (Map.Entry<String, List<Row>> entry : customerGroups.entrySet()) {

				String customerId = entry.getKey();

				List<Row> rows = entry.getValue();

				List<String> errors = validateCustomer(customerId, rows, headers);

				if (errors.isEmpty()) {

					validRows.addAll(rows);

				} else {

					String reason = String.join("; ", errors);

					for (Row row : rows) {

						errorRecords.add(createErrorRecord(row, headers, reason));
					}
				}
			}

			/*
			 * Write valid migration records.
			 */

			writeMigrationData(sheet, validRows, migrationFile);

			/*
			 * Write validation errors.
			 */

			writeErrorData(errorRecords, errorFile);

			int totalCustomers = customerGroups.size();

			int errorCustomers = countErrorCustomers(errorRecords);

			int validCustomers = totalCustomers - errorCustomers;

			System.out.println();
			System.out.println("--------------------------------------------");
			System.out.println(businessUnitType + " VALIDATION SUMMARY");
			System.out.println("--------------------------------------------");

			System.out.println("Total Customers : " + totalCustomers);

			System.out.println("Valid Customers : " + validCustomers);

			System.out.println("Error Customers : " + errorCustomers);

			System.out.println("Migration Rows  : " + validRows.size());

			System.out.println("Error Rows      : " + errorRecords.size());

			System.out.println("Migration Excel : " + migrationFile.getAbsolutePath());

			System.out.println("Error Excel     : " + errorFile.getAbsolutePath());

			System.out.println("--------------------------------------------");

			logger.info("{} Excel validation completed. Total customers={}, valid customers={}, error customers={}",
					businessUnitType, totalCustomers, validCustomers, errorCustomers);
		}
	}

	private Map<String, Integer> readHeaders(Sheet sheet) {

		Map<String, Integer> headers = new LinkedHashMap<>();

		Row headerRow = sheet.getRow(0);

		if (headerRow == null) {

			throw new IllegalArgumentException("Excel header row is missing.");
		}

		for (Cell cell : headerRow) {

			String header = FORMATTER.formatCellValue(cell).trim();

			if (!header.isEmpty()) {

				headers.put(normalize(header), cell.getColumnIndex());
			}
		}

		return headers;
	}

	private void validateRequiredHeaders(Map<String, Integer> headers) {

		String[] requiredHeaders = {

				"customer_id", "customer_name", "place_of_birth_country", "country_of_incorporation", "rm_staff_no",
				"cash_management_staff_no", "wm_pb_staff_no", "Business Unit", "Account Number",
				"Account Opening Date" };

		for (String header : requiredHeaders) {

			if (!headers.containsKey(normalize(header))) {

				throw new IllegalArgumentException("Required Excel header not found: " + header);
			}
		}
	}

	private Map<String, List<Row>> groupByCustomerId(Sheet sheet, Map<String, Integer> headers) {

		Map<String, List<Row>> customerGroups = new LinkedHashMap<>();

		int customerIdColumn = headers.get(normalize("customer_id"));

		for (int i = 1; i <= sheet.getLastRowNum(); i++) {

			Row row = sheet.getRow(i);

			if (row == null) {
				continue;
			}

			String customerId = getCellValue(row, customerIdColumn);

			if (customerId.isEmpty()) {
				continue;
			}

			customerGroups.computeIfAbsent(customerId, key -> new ArrayList<>()).add(row);
		}

		return customerGroups;
	}

	private List<String> validateCustomer(String customerId, List<Row> rows, Map<String, Integer> headers) {

		List<String> errors = new ArrayList<>();

		if (customerId == null || customerId.trim().isEmpty()) {

			errors.add("Customer ID is missing");
		}

		String firstCustomerName = getValue(rows.get(0), headers, "customer_name");

		if (firstCustomerName.isEmpty()) {

			errors.add("Customer Name is missing");
		}

		/*
		 * Same Customer ID + different Customer Name
		 */

		for (Row row : rows) {

			String customerName = getValue(row, headers, "customer_name");

			if (!customerName.isEmpty() && !customerName.equals(firstCustomerName)) {

				errors.add("Different Customer Name found for Customer ID " + customerId + ": " + firstCustomerName
						+ " / " + customerName);

				break;
			}
		}

		/*
		 * Same Customer ID + different Account Number
		 */

		String firstAccountNumber = getValue(rows.get(0), headers, "Account Number");

		for (Row row : rows) {

			String accountNumber = getValue(row, headers, "Account Number");

			if (!accountNumber.isEmpty() && !firstAccountNumber.isEmpty()
					&& !accountNumber.equals(firstAccountNumber)) {

				errors.add("Different Account Number found for Customer ID " + customerId);

				break;
			}
		}

		/*
		 * Place of birth country and country of incorporation cannot both be populated
		 * in the same row.
		 */

		for (Row row : rows) {

			String birthCountry = getValue(row, headers, "place_of_birth_country");

			String incorporationCountry = getValue(row, headers, "country_of_incorporation");

			if (!birthCountry.isEmpty() && !incorporationCountry.isEmpty()) {

				errors.add("Both place_of_birth_country and " + "country_of_incorporation are populated");

				break;
			}
		}

		/*
		 * Business Unit validation.
		 */

		String businessUnit = getValue(rows.get(0), headers, "Business Unit");

		if (businessUnit.isEmpty()) {

			errors.add("Business Unit is missing");
		}

		boolean rmStaffPresent = false;

		boolean cashManagementPresent = false;

		boolean wmPbStaffPresent = false;

		for (Row row : rows) {

			String rmStaff = getValue(row, headers, "rm_staff_no");

			String cashManagement = getValue(row, headers, "cash_management_staff_no");

			String wmPbStaff = getValue(row, headers, "wm_pb_staff_no");

			if (!rmStaff.isEmpty()) {
				rmStaffPresent = true;
			}

			if (!cashManagement.isEmpty()) {
				cashManagementPresent = true;
			}

			if (!wmPbStaff.isEmpty()) {
				wmPbStaffPresent = true;
			}
		}

		/*
		 * Corporate Banking validation.
		 */

		if (businessUnit.equalsIgnoreCase("Corporate Banking")) {

			int staffTypesPresent = 0;

			if (rmStaffPresent) {
				staffTypesPresent++;
			}

			if (cashManagementPresent) {
				staffTypesPresent++;
			}

			if (wmPbStaffPresent) {
				staffTypesPresent++;
			}

			if (staffTypesPresent == 0) {

				errors.add("RM ID is missing for Corporate Banking customer");
			}

			if (staffTypesPresent > 1) {

				errors.add("More than one staff number is populated for Corporate Banking customer " + customerId
						+ " (only one of rm_staff_no, " + "cash_management_staff_no or "
						+ "wm_pb_staff_no is allowed)");
			}

			if (wmPbStaffPresent && !rmStaffPresent && !cashManagementPresent) {

				errors.add("wm_pb_staff_no is populated for Corporate Banking customer");
			}
		}

		/*
		 * Private Banking validation.
		 */

		if (businessUnit.equalsIgnoreCase("Private Banking")) {

			int staffTypesPresent = 0;

			if (rmStaffPresent) {
				staffTypesPresent++;
			}

			if (cashManagementPresent) {
				staffTypesPresent++;
			}

			if (wmPbStaffPresent) {
				staffTypesPresent++;
			}

			if (staffTypesPresent == 0) {

				errors.add("RM ID is missing for Private Banking customer");
			}

			if (staffTypesPresent > 1) {

				errors.add("More than one staff number is populated for Private Banking customer " + customerId
						+ " (only one of rm_staff_no, " + "cash_management_staff_no or "
						+ "wm_pb_staff_no is allowed)");
			}

			if (!wmPbStaffPresent) {

				errors.add("wm_pb_staff_no is missing for Private Banking customer");
			}
		}

		/*
		 * All three staff numbers populated.
		 */

		if (rmStaffPresent && cashManagementPresent && wmPbStaffPresent) {

			errors.add("rm_staff_no, cash_management_staff_no and " + "wm_pb_staff_no are all populated");
		}

		return removeDuplicateErrors(errors);
	}

	private ErrorRecord createErrorRecord(Row row, Map<String, Integer> headers, String reason) {

		String customerId = getValue(row, headers, "customer_id");

		String customerName = getValue(row, headers, "customer_name");

		return new ErrorRecord(customerId, customerName, reason);
	}

	private void writeMigrationData(Sheet sourceSheet, List<Row> validRows, File outputFile) throws Exception {

		File parent = outputFile.getParentFile();

		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}

		try (Workbook workbook = new XSSFWorkbook()) {

			Sheet sheet = workbook.createSheet("Migration Data");

			copyHeader(sourceSheet.getRow(0), sheet);

			int outputRow = 1;

			for (Row sourceRow : validRows) {

				copyRow(sourceRow, sheet.createRow(outputRow++));
			}

			try (FileOutputStream fos = new FileOutputStream(outputFile)) {

				workbook.write(fos);
			}
		}
	}

	private void writeErrorData(List<ErrorRecord> errorRecords, File outputFile) throws Exception {

		File parent = outputFile.getParentFile();

		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}

		try (Workbook workbook = new XSSFWorkbook()) {

			Sheet sheet = workbook.createSheet("Migration Error Data");

			Row header = sheet.createRow(0);

			header.createCell(0).setCellValue("Customer ID");

			header.createCell(1).setCellValue("Customer Name");

			header.createCell(2).setCellValue("Error Reason");

			int rowNum = 1;

			for (ErrorRecord error : errorRecords) {

				Row row = sheet.createRow(rowNum++);

				row.createCell(0).setCellValue(error.customerId);

				row.createCell(1).setCellValue(error.customerName);

				row.createCell(2).setCellValue(error.reason);
			}

			try (FileOutputStream fos = new FileOutputStream(outputFile)) {

				workbook.write(fos);
			}
		}
	}

	private void copyHeader(Row sourceHeader, Sheet destinationSheet) {

		Row destinationHeader = destinationSheet.createRow(0);

		for (Cell cell : sourceHeader) {

			destinationHeader.createCell(cell.getColumnIndex()).setCellValue(FORMATTER.formatCellValue(cell));
		}
	}

	private void copyRow(Row sourceRow, Row destinationRow) {

		for (Cell cell : sourceRow) {

			destinationRow.createCell(cell.getColumnIndex()).setCellValue(FORMATTER.formatCellValue(cell));
		}
	}

	private String getValue(Row row, Map<String, Integer> headers, String headerName) {

		Integer column = headers.get(normalize(headerName));

		if (column == null) {
			return "";
		}

		return getCellValue(row, column);
	}

	private String getCellValue(Row row, int column) {

		Cell cell = row.getCell(column);

		if (cell == null) {
			return "";
		}

		return FORMATTER.formatCellValue(cell).trim();
	}

	private String normalize(String value) {

		if (value == null) {
			return "";
		}

		return value.trim().replaceAll("\\s+", " ").toLowerCase();
	}

	private List<String> removeDuplicateErrors(List<String> errors) {

		return new ArrayList<>(new java.util.LinkedHashSet<>(errors));
	}

	private int countErrorCustomers(List<ErrorRecord> errorRecords) {

		return (int) errorRecords.stream().map(error -> error.customerId).distinct().count();
	}

	private static class ErrorRecord {

		private final String customerId;

		private final String customerName;

		private final String reason;

		ErrorRecord(String customerId, String customerName, String reason) {

			this.customerId = customerId;

			this.customerName = customerName;

			this.reason = reason;
		}
	}
}