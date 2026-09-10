//package com.esun.fn.impl;
//
//import java.io.File;
//import java.util.ArrayList;
//
//import org.apache.log4j.Logger;
//
//import com.config.ConfigReader;
//import com.esun.fn.connection.FN_Connection;
//import com.esun.fn.datamodel.ESUNMetaData;
//import com.filenet.api.constants.RefreshMode;
//import com.filenet.api.core.Document;
//import com.filenet.api.core.Factory;
//import com.filenet.api.core.ObjectStore;
//
//public class MetadataIngestion {
//
//	private static final Logger logger = Logger.getLogger(MetadataIngestion.class);
//
//	public static void main(String[] args) {
//
//		try {
//
//			FN_Connection connection = new FN_Connection();
//
//			ObjectStore os = connection.getFileNetConnection();
//
//			ExcelOperations reader = new ExcelOperations();
//			ArrayList<ESUNMetaData> list = reader
//					.readMetaDataInfoFrmExcel(new File(ConfigReader.getProperty("excel.path")));
//
//			if (list.isEmpty()) {
//
//				System.out.println("No metadata found.");
//				return;
//
//			}
//
//			ESUNMetaData data = list.get(0);
//
//			Document document = Factory.Document.createInstance(os, "ESUN1");
//
//			mapMetadata(document, data);
//
//			document.save(RefreshMode.REFRESH);
//
//			System.out.println("Metadata uploaded successfully.");
//
//		} catch (Exception e) {
//
//			e.printStackTrace();
//
//		}
//
//	}
//
//	public static void mapMetadata(Document document, ESUNMetaData data) {
//
//		document.getProperties().putValue("CustomerID", data.getCustomerId());
//
//		document.getProperties().putValue("CustomerName", data.getCustomerName());
//
//		document.getProperties().putValue("AccountNumber", data.getAccountNumber());
//
//		document.getProperties().putValue("IdentityNumber", data.getIdentityNumber());
//
//		document.getProperties().putValue("PassportNumber", data.getPassportNo());
//
//		document.getProperties().putValue("MainDocumentType", data.getMainDocumentType());
//
//		document.getProperties().putValue("SubDocumentType", data.getSubDocumentType());
//
//		document.getProperties().putValue("BusinessUnit", data.getBusinessUnit());
//
//		document.getProperties().putValue("RMID", data.getRmId());
//
//		document.getProperties().putValue("ShareholderName", data.getShareholderName());
//
//		document.getProperties().putValue("TypeOfUpdate", data.getTypeOfUpdate());
//
//	}
//
//}