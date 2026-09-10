package com.esun.fn.impl;

import java.io.File;

import org.apache.logging.log4j.Logger;

import com.esun.fn.operation.DocumentMigration;
import com.esun.fn.operation.FolderMigration;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;

public class FolderMigrationImpl implements FolderMigration {

	private static final Logger logger = Logger.getLogger(FolderMigrationImpl.class);

	private DocumentMigrationImpl documentMigration = new DocumentMigrationImpl();

	@Override
	public void migrate(ObjectStore os, String sourceRoot, String targetFolder) {

		try {

			logger.info("======================================");
			logger.info("Folder Migration Started");
			logger.info("Source : " + sourceRoot);
			logger.info("Target : " + targetFolder);
			logger.info("======================================");

			File root = new File(sourceRoot);

			if (!root.exists()) {

				logger.error("Source Folder Not Found : " + sourceRoot);
				return;

			}

			Folder fnRoot = Factory.Folder.fetchInstance(os, targetFolder, null);

			processFolder(os, root, fnRoot);

			logger.info("Folder Migration Completed.");

		} catch (Exception e) {

			logger.error("Migration Failed.", e);

		}

	}

	private void processFolder(ObjectStore os, File localFolder, Folder parentFolder) {

		try {

			File[] list = localFolder.listFiles();

			if (list == null)
				return;

			for (File file : list) {

				if (file.isDirectory()) {

					Folder childFolder = createFolder(parentFolder, file.getName());

					if (childFolder != null) {

						processFolder(os, file, childFolder);

					}

				} else {

					documentMigration.uploadDocument(os, parentFolder, file);

				}

			}

		} catch (Exception e) {

			logger.error("Error Processing Folder : " + localFolder.getAbsolutePath(), e);

		}

	}

	private Folder createFolder(Folder parentFolder, String folderName) {

		try {

			logger.info("--------------------------------------");
			logger.info("Folder Name : " + folderName);
			logger.info("Parent      : " + parentFolder.get_PathName());

			if (folderExists(parentFolder, folderName)) {

				logger.info("Folder Already Exists : " + folderName);

				return Factory.Folder.fetchInstance(parentFolder.getObjectStore(),
						parentFolder.get_PathName() + "/" + folderName, null);

			}

			Folder folder = parentFolder.createSubFolder(folderName);

			folder.save(RefreshMode.REFRESH);

			logger.info("Folder Created Successfully : " + folder.get_PathName());

			return folder;

		} catch (Exception e) {

			logger.error("Folder Creation Failed : " + folderName, e);

			return null;

		}

	}

	private boolean folderExists(Folder parentFolder, String folderName) {

		try {

			Factory.Folder.fetchInstance(parentFolder.getObjectStore(), parentFolder.get_PathName() + "/" + folderName,null);

			return true;

		} catch (Exception e) {

			return false;

		}

	}

}