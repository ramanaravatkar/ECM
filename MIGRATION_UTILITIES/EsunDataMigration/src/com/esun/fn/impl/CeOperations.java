package com.esun.fn.impl;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;

import com.config.ConfigReader;
import com.esun.fn.datamodel.ESUNMetaData;
import com.filenet.api.collection.ContentElementList;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.ReferentialContainmentRelationship;
import com.filenet.api.util.Id;

public class CeOperations {
	public Folder getFolderInstance(ObjectStore osInstance, String folderName) {
		Folder folder = null;

		try {

			folder = Factory.Folder.fetchInstance(osInstance, folderName, null);

		} catch (Exception e) {
			// logger.info("Error occured in the ceFolderVerification :::{},e);

		}
		return folder;

	}




	public Folder fetchFolderInstance(ObjectStore osInstance, String folderId) {
		Folder folder = null;
		try {

			folder = Factory.Folder.fetchInstance(osInstance, new Id(folderId), null);

		} catch (Exception e) {
			// logger.info("Error occured in the ceFolderVerification :::{},e);
			System.out.println("Error occured in the fetchFolderInstance :::" + e);

		}
		return folder;

	}

	public String createFolderInCe(ObjectStore osInst, Folder rootFolder, String subfolderName) {
		String folderId = "";
		try {
			Folder folder = Factory.Folder.createInstance(osInst, "ESUNDataFolder");
			folder.set_FolderName(subfolderName);
			folder.set_Parent(rootFolder);

			folder.save(RefreshMode.REFRESH);
			folderId = String.valueOf(folder.get_Id());
			System.out.println("Successfulyl  created the subfolder of :::" + subfolderName);
		} catch (Exception e) {
			System.out.println("Error occured in the createFolderInCe:: " + e);
		}
		return folderId;
	}

	public String createSubfolderHiracy(ObjectStore osInstance, String subFolderName, Folder ceRootFolder) {

		CeOperations ceOps = new CeOperations();

		// Build complete FileNet folder path
		String folderPath = ceRootFolder.get_PathName() + "/" + subFolderName;

		Folder existingFolder = ceOps.getFolderInstance(osInstance, folderPath);

		if (existingFolder != null) {

			System.out.println("Folder already exists : " + folderPath);

			return existingFolder.get_Id().toString();

		}

		System.out.println("Creating Folder : " + folderPath);

		return ceOps.createFolderInCe(osInstance, ceRootFolder, subFolderName);
	}

	public void isCeFOlderExists(ObjectStore objInst, String folderName)
	{
		try
		{

		}catch(Exception e)
		{
			System.out.println("isCeFOlderExists ::"+e);
		}
	}



	public String getMimeType(File file) {

		try {

			String mimeType = Files.probeContentType(file.toPath());

			if (mimeType == null) {

				String fileName = file.getName().toLowerCase();

				if (fileName.endsWith(".pdf"))
					mimeType = "application/pdf";
				else if (fileName.endsWith(".tif") || fileName.endsWith(".tiff"))
					mimeType = "image/tiff";
				else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
					mimeType = "image/jpeg";
				else if (fileName.endsWith(".png"))
					mimeType = "image/png";
				else if (fileName.endsWith(".gif"))
					mimeType = "image/gif";
				else if (fileName.endsWith(".bmp"))
					mimeType = "image/bmp";
				else if (fileName.endsWith(".doc"))
					mimeType = "application/msword";
				else if (fileName.endsWith(".docx"))
					mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
				else if (fileName.endsWith(".xls"))
					mimeType = "application/vnd.ms-excel";
				else if (fileName.endsWith(".xlsx"))
					mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
				else if (fileName.endsWith(".txt"))
					mimeType = "text/plain";
				else
					mimeType = "application/octet-stream";
			}

			return mimeType;

		} catch (Exception e) {

			e.printStackTrace();
			return "application/octet-stream";
		}
	}

	public Document createDocumentInstance(ObjectStore osInst, ESUNMetaData metadata, File documentFile,
			Folder folderInstance) throws Exception {

		String documentClass = ConfigReader.getProperty("document.class");

		try (FileInputStream fis = new FileInputStream(documentFile)) {

			// Create Document
			Document document = Factory.Document.createInstance(osInst, documentClass);

			// System Properties
			document.getProperties().putValue("DocumentTitle", documentFile.getName());

			// Custom Metadata
			document.getProperties().putValue("PassportNo", metadata.getPassportNo());

			//			
			//			  document.getProperties().putValue("CustomerID", metadata.getCustomerId());
			//			  document.getProperties().putValue("CustomerName",
			//			  metadata.getCustomerName());
			//			  document.getProperties().putValue("AccountNumber",
			//			  metadata.getAccountNumber());
			//			  document.getProperties().putValue("IdentityNumber",
			//			  metadata.getIdentityNumber()); document.getProperties().putValue("W8BenDate",
			//			  metadata.getW8BenDate());
			//			  document.getProperties().putValue("BusinessRegistrationCertificate",
			//			  metadata.getBusinessRegistrationCertificate());
			//			  document.getProperties().putValue("MainDocumentType",
			//			  metadata.getMainDocumentType());
			//			  document.getProperties().putValue("SubDocumentType",
			//			  metadata.getSubDocumentType());
			//			  document.getProperties().putValue("AccountOpeningDate",
			//			  metadata.getAccountOpeningDate());
			//			  document.getProperties().putValue("BusinessUnit",
			//			  metadata.getBusinessUnit()); document.getProperties().putValue("RMID",
			//			  metadata.getRmId());
			//			  document.getProperties().putValue("CountryOfIncorporationBirth",
			//			  metadata.getCountryOfIncorporationBirth());
			//			  document.getProperties().putValue("AccountClosed",
			//			  metadata.getAccountClosed());
			//			  document.getProperties().putValue("ShareholderName",
			//			  metadata.getShareholderName());
			//			  document.getProperties().putValue("TypeOfUpdate",
			//			  metadata.getTypeOfUpdate());
			//			 

			// dev properties--

			
			  document.getProperties().putValue("CustomerID", metadata.getCustomerId());
			  document.getProperties().putValue("CustomerName",
			  metadata.getCustomerName());
			  document.getProperties().putValue("AccountNumber",
			  metadata.getAccountNumber());
			  document.getProperties().putValue("Identity_Number",
			  metadata.getIdentityNumber()); document.getProperties().putValue("W8_BEN_DATE",
			  metadata.getW8BenDate());
			  document.getProperties().putValue("BusinessRegistrationCertificate",
			  metadata.getBusinessRegistrationCertificate());
			  document.getProperties().putValue("MainDocumentType",
			  metadata.getMainDocumentType());
			  document.getProperties().putValue("SubDocumentType1",
			  metadata.getSubDocumentType());
			  document.getProperties().putValue("Account_Opening_Date",
			  metadata.getAccountOpeningDate());
			  document.getProperties().putValue("BusinessUnit",
			  metadata.getBusinessUnit()); document.getProperties().putValue("RM_ID",
			  metadata.getRmId());
			  document.getProperties().putValue("CountryOfIncorporationBirth",
			  metadata.getCountryOfIncorporationBirth());
			  document.getProperties().putValue("AccountClosed",
			  metadata.getAccountClosed());
			  document.getProperties().putValue("Shareholder_Name",
			  metadata.getShareholderName());
			  document.getProperties().putValue("TypeOfUpdate",
			  metadata.getTypeOfUpdate());
			 

			ContentTransfer contenttransfer = Factory.ContentTransfer.createInstance();
			contenttransfer.setCaptureSource(fis);
			contenttransfer.set_RetrievalName(documentFile.getName());

			ContentElementList contentelementlist = Factory.ContentElement.createList();
			contentelementlist.add(contenttransfer);

			document.set_ContentElements(contentelementlist);

			String mimeType = getMimeType(documentFile);

			System.out.println("Detected Mime Type : " + mimeType);

			document.set_MimeType(mimeType);

			document.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);

			document.save(RefreshMode.REFRESH);

			return document;
		}
	}

	public boolean uploadDocument(ObjectStore osInstance, Folder ceRootFolder, File processingData,
			ESUNMetaData metadata) {
		boolean docCreationStatus=false;


		System.out.println("Uploading File : " + processingData.getAbsolutePath());

		// Create CeOperations object
		CeOperations ceOps = new CeOperations();

		// Create FileNet Document
		/*if (ceOps.documentExists(
			        osInstance,
			        "PassportNo",
			        metadata.getPassportNo())) {

			    logger.info("Document already exists. Skipping upload.");

			    return;
			}*/


		String documentClass = ConfigReader.getProperty("document.class");

		try (FileInputStream fis = new FileInputStream(processingData)) {

			// Create Document
			Document document = Factory.Document.createInstance(osInstance, documentClass);

			// System Properties
			document.getProperties().putValue("DocumentTitle", processingData.getName());

			// Custom Metadata
			document.getProperties().putValue("PassportNo", metadata.getPassportNo());

			//				
			//				  document.getProperties().putValue("CustomerID", metadata.getCustomerId());
			//				  document.getProperties().putValue("CustomerName",
			//				  metadata.getCustomerName());
			//				  document.getProperties().putValue("AccountNumber",
			//				  metadata.getAccountNumber());
			//				  document.getProperties().putValue("IdentityNumber",
			//				  metadata.getIdentityNumber()); document.getProperties().putValue("W8BenDate",
			//				  metadata.getW8BenDate());
			//				  document.getProperties().putValue("BusinessRegistrationCertificate",
			//				  metadata.getBusinessRegistrationCertificate());
			//				  document.getProperties().putValue("MainDocumentType",
			//				  metadata.getMainDocumentType());
			//				  document.getProperties().putValue("SubDocumentType",
			//				  metadata.getSubDocumentType());
			//				  document.getProperties().putValue("AccountOpeningDate",
			//				  metadata.getAccountOpeningDate());
			//				  document.getProperties().putValue("BusinessUnit",
			//				  metadata.getBusinessUnit()); document.getProperties().putValue("RMID",
			//				  metadata.getRmId());
			//				  document.getProperties().putValue("CountryOfIncorporationBirth",
			//				  metadata.getCountryOfIncorporationBirth());
			//				  document.getProperties().putValue("AccountClosed",
			//				  metadata.getAccountClosed());
			//				  document.getProperties().putValue("ShareholderName",
			//				  metadata.getShareholderName());
			//				  document.getProperties().putValue("TypeOfUpdate",
			//				  metadata.getTypeOfUpdate());
			//				 

			// dev properties--
		//	document.getProperties().putValue("AccountOpeningDate1",metadata.getAccountOpeningDate());

			/*
			 * document.getProperties().putValue("CustomerID", metadata.getCustomerId());
			 * document.getProperties().putValue("CustomerName",
			 * metadata.getCustomerName());
			 * document.getProperties().putValue("AccountNumber",
			 * metadata.getAccountNumber());
			 * document.getProperties().putValue("IdentityNumber",
			 * metadata.getIdentityNumber()); document.getProperties().putValue("W8BenDate",
			 * metadata.getW8BenDate());
			 * document.getProperties().putValue("BusinessRegistrationCertificate",
			 * metadata.getBusinessRegistrationCertificate());
			 * document.getProperties().putValue("MainDocumentType",
			 * metadata.getMainDocumentType());
			 * document.getProperties().putValue("SubDocumentType",
			 * metadata.getSubDocumentType());
			 * document.getProperties().putValue("AccountOpeningDate",
			 * metadata.getAccountOpeningDate());
			 * document.getProperties().putValue("BusinessUnit",
			 * metadata.getBusinessUnit()); document.getProperties().putValue("RMID",
			 * metadata.getRmId());
			 * document.getProperties().putValue("CountryOfIncorporationBirth",
			 * metadata.getCountryOfIncorporationBirth());
			 * document.getProperties().putValue("AccountClosed",
			 * metadata.getAccountClosed());
			 * document.getProperties().putValue("ShareholderName",
			 * metadata.getShareholderName());
			 * document.getProperties().putValue("TypeOfUpdate",
			 * metadata.getTypeOfUpdate());
			 */
			System.out.println("Metadata updated successfulyl ::");
			ContentTransfer contenttransfer = Factory.ContentTransfer.createInstance();
			contenttransfer.setCaptureSource(fis);
			contenttransfer.set_RetrievalName(processingData.getName());

			ContentElementList contentelementlist = Factory.ContentElement.createList();
			contentelementlist.add(contenttransfer);

			document.set_ContentElements(contentelementlist);

			String mimeType = getMimeType(processingData);

			System.out.println("Detected Mime Type : " + mimeType);

			document.set_MimeType(mimeType);

			document.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);

			document.save(RefreshMode.REFRESH);





			// File the document into the folder
			ReferentialContainmentRelationship rcr = ceRootFolder.file(document, AutoUniqueName.AUTO_UNIQUE,
					processingData.getName(), DefineSecurityParentage.DEFINE_SECURITY_PARENTAGE);

			// Save the filing relationship
			rcr.save(RefreshMode.REFRESH);

			System.out.println("Document Uploaded Successfully : " + processingData.getName());
			System.out.println("document id:::" + document.get_Id());
			docCreationStatus=true;
		} catch (Exception e) {

			System.out.println("Document Upload Failed : " + processingData.getAbsolutePath());

			e.printStackTrace();
		}

		return docCreationStatus;
	}

	public boolean documentExists(ObjectStore osInstance, String propertyName, String passportNo) {
		// TODO Auto-generated method stub
		return false;
	}

}
