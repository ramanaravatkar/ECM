package com.esun.fn.operation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

public class ExcelReportWriterImpl {
	private static final Logger logger = LogManager.getLogger(ExcelReportWriterImpl.class);

	private static final String KEY_SUCCESS_FILES_COUNT = "SuccessFilesCount";

	private CellStyle createFolderStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setAlignment(HorizontalAlignment.LEFT);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setWrapText(true);
		return style;
	}

	private CellStyle createHeaderStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		Font font = workbook.createFont();
		font.setBold(true);
		font.setFontHeightInPoints((short) 11);
		style.setFont(font);
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setWrapText(true);
		return style;
	}

	private CellStyle createDataStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setWrapText(true);
		return style;
	}

	public void writeReport(String excelPath, JSONArray customerArray) {
		logger.info("Excel report generation started. Customer count: {}", customerArray.length());
		File excelFile = new File(excelPath);
		if (!excelFile.exists()) {
			logger.error("Report Excel file not found: {}", excelPath);
			return;
		}
		try (FileInputStream fis = new FileInputStream(excelFile); XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
			String sheetName = "Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			XSSFSheet reportSheet = workbook.createSheet(sheetName);
			CellStyle headerStyle = createHeaderStyle(workbook);
			CellStyle dataStyle = createDataStyle(workbook);
			CellStyle folderStyle = createFolderStyle(workbook);
			Row header = reportSheet.createRow(0);
			header.setHeightInPoints(28);
			reportSheet.createFreezePane(0, 1);
			reportSheet.setDisplayGridlines(true);
			createCell(header, 0, "Customer ID", headerStyle);
			createCell(header, 1, "Customer Name", headerStyle);
			createCell(header, 2, "Documents Count", headerStyle);
			createCell(header, 3, "Success", headerStyle);
			createCell(header, 4, "Failure", headerStyle);
			createCell(header, 5, "Sub Folder Path", headerStyle);
			createCell(header, 6, "Documents Count", headerStyle);
			createCell(header, 7, "Success", headerStyle);
			createCell(header, 8, "Failure", headerStyle);
			createCell(header, 9, "Total Success Documents", headerStyle);
			int rowNum = 1;
			for (int i = 0; i < customerArray.length(); i++) {
				JSONObject customerObject = customerArray.getJSONObject(i);
				JSONObject customerInfo = customerObject.getJSONObject("CustomerInfo");
				JSONArray folderInfo = customerObject.getJSONArray("folderInfo");
				int startRow = rowNum;
				if (folderInfo.length() == 0) {
					Row row = reportSheet.createRow(rowNum);
					row.setHeightInPoints(22);
					createCell(row, 0, customerInfo.getString("customerid"), dataStyle);
					createCell(row, 1, customerInfo.getString("customerName"), dataStyle);
					createCell(row, 2, customerInfo.getInt("totalFiles"), dataStyle);
					createCell(row, 3, customerInfo.getInt(KEY_SUCCESS_FILES_COUNT), dataStyle);
					createCell(row, 4, customerInfo.getInt("FailureFilesCount"), dataStyle);
					createCell(row, 5, "", dataStyle);
					createCell(row, 6, "", dataStyle);
					createCell(row, 7, "", dataStyle);
					createCell(row, 8, "", dataStyle);
					createCell(row, 9, customerInfo.getInt(KEY_SUCCESS_FILES_COUNT), dataStyle);
					rowNum++;
				} else {
					int totalSuccessDocuments = customerInfo.getInt(KEY_SUCCESS_FILES_COUNT);
					for (int j = 0; j < folderInfo.length(); j++) {
						JSONObject folder = folderInfo.getJSONObject(j);
						totalSuccessDocuments += folder.getInt("successFiles");
					}
					for (int j = 0; j < folderInfo.length(); j++) {
						Row row = reportSheet.createRow(rowNum);
						row.setHeightInPoints(22);
						JSONObject folder = folderInfo.getJSONObject(j);
						if (j == 0) {
							createCell(row, 0, customerInfo.getString("customerid"), dataStyle);
							createCell(row, 1, customerInfo.getString("customerName"), dataStyle);
							createCell(row, 2, customerInfo.getInt("totalFiles"), dataStyle);
							createCell(row, 3, customerInfo.getInt(KEY_SUCCESS_FILES_COUNT), dataStyle);
							createCell(row, 4, customerInfo.getInt("FailureFilesCount"), dataStyle);
							createCell(row, 9, totalSuccessDocuments, dataStyle);
						}
						createCell(row, 5, folder.getString("FolderName"), folderStyle);
						createCell(row, 6, folder.getInt("totalFilesCount"), dataStyle);
						createCell(row, 7, folder.getInt("successFiles"), dataStyle);
						createCell(row, 8, folder.getInt("failureFiles"), dataStyle);
						rowNum++;
					}
					int endRow = rowNum - 1;
					if (folderInfo.length() > 1) {
						merge(reportSheet, startRow, endRow, 0);
						merge(reportSheet, startRow, endRow, 1);
						merge(reportSheet, startRow, endRow, 2);
						merge(reportSheet, startRow, endRow, 3);
						merge(reportSheet, startRow, endRow, 4);
						merge(reportSheet, startRow, endRow, 9);
					}
				}
			}
			reportSheet.setColumnWidth(0, 3400);
			reportSheet.setColumnWidth(1, 7000);
			reportSheet.setColumnWidth(2, 3000);
			reportSheet.setColumnWidth(3, 2400);
			reportSheet.setColumnWidth(4, 2400);
			reportSheet.setColumnWidth(5, 17000);
			reportSheet.setColumnWidth(6, 3000);
			reportSheet.setColumnWidth(7, 2400);
			reportSheet.setColumnWidth(8, 2400);
			reportSheet.setColumnWidth(9, 4200);
			try (FileOutputStream fos = new FileOutputStream(excelFile)) {
				workbook.write(fos);
			}
			logger.info("Excel report generated successfully. Sheet: {}", sheetName);
		} catch (Exception e) {
			logger.error("Failed to generate Excel report for file: {}", excelPath, e);
		}
	}

	private void merge(XSSFSheet sheet, int firstRow, int lastRow, int column) {
		sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, column, column));
	}

	private void createCell(Row row, int column, Object value, CellStyle style) {
		Cell cell = row.createCell(column);
		if (value instanceof String stringValue) {
			cell.setCellValue(stringValue);
		} else if (value instanceof Integer integerValue) {
			cell.setCellValue(integerValue);
		} else if (value instanceof Long longValue) {
			cell.setCellValue(longValue);
		} else if (value instanceof Double doubleValue) {
			cell.setCellValue(doubleValue);
		}
		cell.setCellStyle(style);
	}
}