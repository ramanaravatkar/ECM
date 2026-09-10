package com.esun.fn.operation;

import java.io.File;
import java.util.List;

import com.config.ConfigReader;
import com.esun.fn.datamodel.ESUNMetaData;
import com.esun.fn.datamodel.ReportData;
import com.esun.fn.impl.ExcelOperations;
import com.esun.fn.impl.ExcelReportWriterImpl;

public class ReadExcelOperation {

	/**
	 * Read Metadata Excel
	 */
	public List<ESUNMetaData> readExcel() throws Exception {

		File excelFile = new File(ConfigReader.getProperty("excel.path"));

		ExcelOperations excelReader = new ExcelOperations();

		List<ESUNMetaData> metadataList = excelReader.readMetaDataInfoFrmExcel(excelFile);

		System.out.println("---------------------------------------------");
		System.out.println("Total Records Read : " + metadataList.size());
		System.out.println("---------------------------------------------");

		for (ESUNMetaData metadata : metadataList) {

			System.out.println(metadata);

		}

		return metadataList;
	}

	
	public void writeReport(List<ReportData> reportList) throws Exception {

		String excelPath = ConfigReader.getProperty("excel.path");

		ExcelReportWriterImpl writer = new ExcelReportWriterImpl();

		writer.writeReport(excelPath, reportList);


		System.out.println("Migration Report Generated Successfully.");


	}

}