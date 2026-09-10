package com.esun.local;

import java.io.File;

public class Folder_Validation {

	// ========================= CONFIGURATION =========================

	private static final String INPUT_PATH = "D:\\ESUN MIGRATION\\Data Transition Test Sample Cases_PB_20260720";

	private static final String OUTPUT_PATH = "D:\\ProcessedFiles";

	

	private static int totalFolders = 0;
	private static int totalFiles = 0;

	private static int successFolders = 0;
	private static int failureFolders = 0;

	private static int successFiles = 0;
	private static int failureFiles = 0;

	// ==============================================================

	public static void main(String[] args) {

		System.out.println("==============================================");
		System.out.println("      ESUN PRE MIGRATION VALIDATION");
		System.out.println("==============================================");

		File inputRoot = new File(INPUT_PATH);

		if (!inputRoot.exists()) {

			System.out.println("Input Folder Not Found.");
			return;

		}

		File[] customerFolders = inputRoot.listFiles();

		if (customerFolders == null) {

			System.out.println("No folders found.");
			return;

		}

		for (File customerFolder : customerFolders) {

			if (!customerFolder.isDirectory()) {

				continue;

			}

			System.out.println();
			System.out.println("==================================================");
			System.out.println("Customer Folder : " + customerFolder.getName());
			System.out.println("==================================================");

			boolean result = validateFolder(customerFolder);

			try {

				if (result) {

					successFolders++;

					System.out.println();
					System.out.println("=====================================");
					System.out.println("FINAL RESULT : SUCCESS");
					System.out.println("=====================================");

					File successFolder = new File(
							OUTPUT_PATH + File.separator + "Success" + File.separator + customerFolder.getName());

					moveCustomerFolder(customerFolder, successFolder);

				} else {

					failureFolders++;

					System.out.println();
					System.out.println("=====================================");
					System.out.println("FINAL RESULT : FAILURE");
					System.out.println("=====================================");

					File failureFolder = new File(
							OUTPUT_PATH + File.separator + "Failure" + File.separator + customerFolder.getName());

					moveCustomerFolder(customerFolder, failureFolder);

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

		}

		printSummary();

	}

	// ==============================================================

	private static boolean validateFolder(File folder) {

		totalFolders++;

		boolean folderStatus = true;

		System.out.println();
		System.out.println("--------------------------------------");
		System.out.println("Folder : " + folder.getAbsolutePath());
		System.out.println("--------------------------------------");

		if (!folder.exists()) {

			System.out.println("Folder Exists      : FAIL");

			return false;

		}

		System.out.println("Folder Exists      : PASS");

		if (!folder.canRead()) {

			System.out.println("Readable           : FAIL");

			folderStatus = false;

		} else {

			System.out.println("Readable           : PASS");

		}

		File[] contents = folder.listFiles();

		if (contents == null || contents.length == 0) {

			System.out.println("Folder Empty       : YES");

			folderStatus = false;

			return folderStatus;

		}

		System.out.println("Folder Empty       : NO");

		for (File file : contents) {

			if (file.isDirectory()) {

				boolean childStatus = validateFolder(file);

				if (!childStatus) {

					folderStatus = false;

				}

			} else {

				boolean fileStatus = validateFile(file);

				if (!fileStatus) {

					folderStatus = false;

				}

			}

		}

		return folderStatus;

	}

	// ==============================================================

	private static boolean validateFile(File file) {

		totalFiles++;

		System.out.println();
		System.out.println("File : " + file.getName());

		boolean status = true;

		if (!file.exists()) {

			System.out.println("Exists             : FAIL");

			failureFiles++;

			return false;

		}

		System.out.println("Exists             : PASS");

		if (!file.canRead()) {

			System.out.println("Readable           : FAIL");

			status = false;

		} else {

			System.out.println("Readable           : PASS");

		}

		if (file.length() == 0) {

			System.out.println("Size               : 0 Bytes");

			status = false;

		} else {

			System.out.println("Size               : " + file.length() + " Bytes");

		}

		if (file.isHidden()) {

			System.out.println("Hidden             : YES");

		} else {

			System.out.println("Hidden             : NO");

		}

		// Part 2
		String mime = getMimeType(file);

		System.out.println("Mime Type          : " + mime);

		if ("application/octet-stream".equals(mime)) {

			System.out.println("Mime Validation    : DEFAULT");

		} else {

			System.out.println("Mime Validation    : VALID");

		}

		if (status) {

			successFiles++;

			System.out.println("Validation         : SUCCESS");

		} else {

			failureFiles++;

			System.out.println("Validation         : FAILURE");

		}

		return status;

	}
	// ==============================================================
	// MIME TYPE
	// ==============================================================

	private static String getMimeType(File file) {

		String name = file.getName().toLowerCase();

		if (name.endsWith(".pdf"))
			return "application/pdf";

		if (name.endsWith(".doc"))
			return "application/msword";

		if (name.endsWith(".docx"))
			return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

		if (name.endsWith(".xls"))
			return "application/vnd.ms-excel";

		if (name.endsWith(".xlsx"))
			return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

		if (name.endsWith(".ppt"))
			return "application/vnd.ms-powerpoint";

		if (name.endsWith(".pptx"))
			return "application/vnd.openxmlformats-officedocument.presentationml.presentation";

		if (name.endsWith(".txt"))
			return "text/plain";

		if (name.endsWith(".csv"))
			return "text/csv";

		if (name.endsWith(".xml"))
			return "application/xml";

		if (name.endsWith(".jpg"))
			return "image/jpeg";

		if (name.endsWith(".jpeg"))
			return "image/jpeg";

		if (name.endsWith(".png"))
			return "image/png";

		if (name.endsWith(".gif"))
			return "image/gif";

		if (name.endsWith(".bmp"))
			return "image/bmp";

		if (name.endsWith(".zip"))
			return "application/zip";

		if (name.endsWith(".rar"))
			return "application/x-rar-compressed";

		return "application/octet-stream";

	}

	// ==============================================================
	// MOVE CUSTOMER FOLDER
	// ==============================================================

	private static void moveCustomerFolder(File source, File destination) throws Exception {

		System.out.println();
		System.out.println("Moving Folder");
		System.out.println("FROM : " + source.getAbsolutePath());
		System.out.println("TO   : " + destination.getAbsolutePath());

		copyFolder(source, destination);

		deleteFolder(source);

		System.out.println("Move Completed.");

	}

	// ==============================================================
	// COPY FOLDER
	// ==============================================================

	private static void copyFolder(File source, File destination) throws Exception {

		if (source.isDirectory()) {

			if (!destination.exists()) {

				destination.mkdirs();

			}

			File[] files = source.listFiles();

			if (files != null) {

				for (File file : files) {

					copyFolder(file, new File(destination, file.getName()));

				}

			}

		} else {

			java.nio.file.Files.copy(source.toPath(), destination.toPath(),
					java.nio.file.StandardCopyOption.REPLACE_EXISTING);

			System.out.println("Copied : " + source.getName());

		}

	}

	// ==============================================================
	// DELETE SOURCE
	// ==============================================================

	private static void deleteFolder(File file) throws Exception {

		if (file.isDirectory()) {

			File[] children = file.listFiles();

			if (children != null) {

				for (File child : children) {

					deleteFolder(child);

				}

			}

		}

		if (file.delete()) {

			System.out.println("Deleted : " + file.getAbsolutePath());

		}

	}

	// ==============================================================
	// SUMMARY
	// ==============================================================

	private static void printSummary() {

		System.out.println();
		System.out.println();
		System.out.println("==========================================");
		System.out.println("             FINAL SUMMARY");
		System.out.println("==========================================");

		System.out.println("Folders Checked      : " + totalFolders);

		System.out.println("Files Checked        : " + totalFiles);

		System.out.println();

		System.out.println("Success Folders      : " + successFolders);

		System.out.println("Failure Folders      : " + failureFolders);

		System.out.println();

		System.out.println("Success Files        : " + successFiles);

		System.out.println("Failure Files        : " + failureFiles);

		System.out.println("==========================================");

	}
}