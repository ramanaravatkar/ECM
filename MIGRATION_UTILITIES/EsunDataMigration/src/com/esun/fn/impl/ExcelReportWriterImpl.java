package com.esun.fn.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.config.ConfigReader;
import com.esun.fn.datamodel.ReportData;

public class ExcelReportWriterImpl {

	public void writeReport(String excelPath, List<ReportData> reportList) {

		try {

		//	File excelFile = new File(excelPath);
			String reportPath = ConfigReader.getProperty("report.path");

			File excelFile = new File(reportPath);
			FileInputStream fis = new FileInputStream(excelFile);

			XSSFWorkbook workbook = new XSSFWorkbook(fis);

			String sheetName = "Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

			XSSFSheet reportSheet = workbook.createSheet(sheetName);

			// Header

			Row header = reportSheet.createRow(0);

			header.createCell(0).setCellValue("Customer ID");
			header.createCell(1).setCellValue("Customer Name");
			header.createCell(2).setCellValue("Folder Counts");
			header.createCell(3).setCellValue("Documents Count");
			header.createCell(4).setCellValue("Status");
			header.createCell(5).setCellValue("Error");

			int rowNum = 1;

			for (ReportData report : reportList) {

				Row row = reportSheet.createRow(rowNum++);

				row.createCell(0).setCellValue(report.getCustomerId());
				row.createCell(1).setCellValue(report.getCustomerName());
				row.createCell(2).setCellValue(report.getFolderCount());
				row.createCell(3).setCellValue(report.getDocumentCount());
				row.createCell(4).setCellValue(report.getStatus());
				row.createCell(5).setCellValue(report.getError());

			}

			for (int i = 0; i < 6; i++) {

				reportSheet.autoSizeColumn(i);

			}

			fis.close();

			FileOutputStream fos = new FileOutputStream(excelFile);

			workbook.write(fos);

			fos.close();

			workbook.close();

			System.out.println("Report Sheet Created Successfully : " + sheetName);

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

}