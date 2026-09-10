package com.esun.fn.operation;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

	private static final Logger logger = LogManager.getLogger(CeOperations.class);

	private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
	private static final Map<String, String> EXTENSION_TO_MIME_TYPE = createExtensionToMimeTypeMap();

	private static Map<String, String> createExtensionToMimeTypeMap() {
		Map<String, String> map = new HashMap<>();
		map.put("pdf", "application/pdf");
		map.put("tif", "image/tiff");
		map.put("tiff", "image/tiff");
		map.put("jpg", "image/jpeg");
		map.put("jpeg", "image/jpeg");
		map.put("png", "image/png");
		map.put("gif", "image/gif");
		map.put("bmp", "image/bmp");
		map.put("doc", "application/msword");
		map.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		map.put("xls", "application/vnd.ms-excel");
		map.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		map.put("txt", "text/plain");
		return Collections.unmodifiableMap(map);
	}

	public Folder getFolderInstance(ObjectStore osInstance, String folderName) {

	    try {

	        return Factory.Folder.fetchInstance(osInstance, folderName, null);

	    } catch (Exception e) {

	        logger.info("Folder does not exist: {}", folderName);

	        return null;
	    }
	}

	public Folder fetchFolderInstance(ObjectStore osInstance, String folderId) {

		try {

			return Factory.Folder.fetchInstance(osInstance, new Id(folderId), null);

		} catch (Exception e) {

			logger.error("Unable to fetch folder by id: {}", folderId, e);
			return null;
		}
	}

	public String createFolderInCe(ObjectStore osInst, Folder rootFolder, String subfolderName) {

		try {

			Folder folder = Factory.Folder.createInstance(osInst, "ESUNDataFolder");

			folder.set_FolderName(subfolderName);
			folder.set_Parent(rootFolder);

			folder.save(RefreshMode.REFRESH);

			String folderId = String.valueOf(folder.get_Id());

			logger.info("Folder created successfully: {} | Folder ID: {}", subfolderName, folderId);

			return folderId;

		} catch (Exception e) {

			logger.error("Failed to create folder: {}", subfolderName, e);
			return "";
		}
	}

	public String createSubfolderHiracy(ObjectStore osInstance, String subFolderName, Folder ceRootFolder) {

		String folderPath = ceRootFolder.get_PathName() + "/" + subFolderName;

		Folder existingFolder = getFolderInstance(osInstance, folderPath);

		if (existingFolder != null) {

			logger.info("Folder already exists: {}", folderPath);

			return existingFolder.get_Id().toString();
		}

		logger.info("Creating subfolder: {}", folderPath);

		return createFolderInCe(osInstance, ceRootFolder, subFolderName);
	}

	public String getMimeType(File file) {

		try {

			String mimeType = Files.probeContentType(file.toPath());

			if (mimeType == null) {

				String fileName = file.getName().toLowerCase();
				int lastDot = fileName.lastIndexOf('.');
				String extension = lastDot >= 0 ? fileName.substring(lastDot + 1) : "";

				mimeType = EXTENSION_TO_MIME_TYPE.getOrDefault(extension, DEFAULT_MIME_TYPE);
			}

			logger.info("Detected MIME type: {} | File: {}", mimeType, file.getName());

			return mimeType;

		} catch (Exception e) {

			logger.error("Failed to determine MIME type for file: {}", file.getAbsolutePath(), e);

			return DEFAULT_MIME_TYPE;
		}
	}

	public boolean uploadDocument(ObjectStore osInstance, Folder ceRootFolder, File processingData,
			ESUNMetaData metadata) {

		logger.info("Document upload started: {}", processingData.getAbsolutePath());

		String documentClass = ConfigReader.getProperty("document.class");

		try (FileInputStream fis = new FileInputStream(processingData)) {

			Document document = Factory.Document.createInstance(osInstance, documentClass);

			String documentTitlePropertyName = ConfigReader.getProperty("symbol.documentTitle");
			document.getProperties().putValue(documentTitlePropertyName, processingData.getName());

			String dateFormat = ConfigReader.getProperty("date.format");

			SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

			Date accountOpeningDate = null;
			Date w8BenDate = null;

			if (metadata.getAccountOpeningDate() != null && !metadata.getAccountOpeningDate().trim().isEmpty()) {
				accountOpeningDate = sdf.parse(metadata.getAccountOpeningDate());
			}

			if (metadata.getW8BenDate() != null && !metadata.getW8BenDate().trim().isEmpty()) {
				w8BenDate = sdf.parse(metadata.getW8BenDate());
			}

			if (accountOpeningDate != null) {
				String accountOpeningDatePropertyName = ConfigReader.getProperty("symbol.accountOpeningDate");
				document.getProperties().putValue(accountOpeningDatePropertyName, accountOpeningDate);
			}

			if (w8BenDate != null) {
				String w8BenDatePropertyName = ConfigReader.getProperty("symbol.w8BenDate");
				document.getProperties().putValue(w8BenDatePropertyName, w8BenDate);
			}


			String customerNamePropertyName = ConfigReader.getProperty("symbol.customerName");
			document.getProperties().putValue(customerNamePropertyName, metadata.getCustomerName());


			String customerIDPropertyName = ConfigReader.getProperty("symbol.customerID");
			document.getProperties().putValue(customerIDPropertyName, Integer.parseInt(metadata.getCustomerId()));


			String accountNumberPropertyName = ConfigReader.getProperty("symbol.accountNumber");
			document.getProperties().putValue(accountNumberPropertyName, metadata.getAccountNumber());


			String countryOfIncorporationBirthPropertyName = ConfigReader.getProperty("symbol.countryOfIncorporationBirth");
			document.getProperties().putValue(countryOfIncorporationBirthPropertyName,
					metadata.getCountryOfIncorporationBirth());


			String businessRegistrationCertificatePropertyName = ConfigReader
					.getProperty("symbol.businessRegistrationCertificate");

			document.getProperties().putValue(
					businessRegistrationCertificatePropertyName,
					metadata.getBusinessRegistrationCertificate()
			);


			String passportNoPropertyName = ConfigReader.getProperty("symbol.passportNo");
			document.getProperties().putValue(passportNoPropertyName, metadata.getPassportNo());


			String identityNumberPropertyName = ConfigReader.getProperty("symbol.identityNumber");
			document.getProperties().putValue(identityNumberPropertyName, metadata.getIdentityNumber());


			String businessUnitPropertyName = ConfigReader.getProperty("symbol.businessUnit");
			document.getProperties().putValue(businessUnitPropertyName, metadata.getBusinessUnit());


			String rmIDPropertyName = ConfigReader.getProperty("symbol.rmID");
			document.getProperties().putValue(rmIDPropertyName, metadata.getRmId());


			String accountClosedPropertyName = ConfigReader.getProperty("symbol.accountClosed");
			document.getProperties().putValue(accountClosedPropertyName, metadata.getAccountClosed());
			logger.info("AccountClosed value being sent to FileNet = [{}]",
			        metadata.getAccountClosed());

			String mainDocumentTypePropertyName = ConfigReader.getProperty("symbol.mainDocumentType");
			document.getProperties().putValue(mainDocumentTypePropertyName, metadata.getMainDocumentType());


			String subDocumentTypePropertyName = ConfigReader.getProperty("symbol.subDocumentType");
			document.getProperties().putValue(subDocumentTypePropertyName, metadata.getSubDocumentType());


			String shareholderNamePropertyName = ConfigReader.getProperty("symbol.shareholderName");
			document.getProperties().putValue(shareholderNamePropertyName, metadata.getShareholderName());


			String typeOfUpdatePropertyName = ConfigReader.getProperty("symbol.typeOfUpdate");
			document.getProperties().putValue(typeOfUpdatePropertyName, metadata.getTypeOfUpdate());

			logger.info("Metadata populated for document: {}", processingData.getName());

			ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();

			contentTransfer.setCaptureSource(fis);
			contentTransfer.set_RetrievalName(processingData.getName());

			ContentElementList contentElementList = Factory.ContentElement.createList();

			contentElementList.add(contentTransfer);

			document.set_ContentElements(contentElementList);

			String mimeType = getMimeType(processingData);

			document.set_MimeType(mimeType);

			document.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);

			document.save(RefreshMode.REFRESH);

			ReferentialContainmentRelationship rcr = ceRootFolder.file(document, AutoUniqueName.AUTO_UNIQUE,
					processingData.getName(), DefineSecurityParentage.DEFINE_SECURITY_PARENTAGE);

			rcr.save(RefreshMode.REFRESH);

			logger.info("Document uploaded successfully: {} | Document ID: {}", processingData.getName(),
					document.get_Id());

			return true;

		} catch (Exception e) {

			logger.error("Document upload failed: {}", processingData.getAbsolutePath(), e);

			return false;
		}
	}
}