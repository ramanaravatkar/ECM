package com.esun.fn.operation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Dependency-free verification for ExcelOperations.sanitizeForFileSystemAndLog(), which neutralizes
 * Excel-sourced customer ID/name values before they are used to build filesystem paths or written to
 * the log. Same plain main()-driven style as ESUNPasswordEncryptionTest - no test framework exists
 * in this project.
 */
public final class ExcelOperationsSanitizeTest {

	private static final Logger logger = LogManager.getLogger(ExcelOperationsSanitizeTest.class);

	private ExcelOperationsSanitizeTest() {
		throw new IllegalStateException("Utility class");
	}

	public static void main(String[] args) {
		int failures = 0;

		failures += check("plain value is left unchanged",
				"Acme Corp".equals(ExcelOperations.sanitizeForFileSystemAndLog("Acme Corp")));

		failures += check("backslash path traversal cannot escape a parent directory",
				!ExcelOperations.sanitizeForFileSystemAndLog("..\\..\\Windows\\System32").contains("\\")
						&& !ExcelOperations.sanitizeForFileSystemAndLog("..\\..\\Windows\\System32").contains(".."));

		failures += check("forward-slash path traversal cannot escape a parent directory",
				!ExcelOperations.sanitizeForFileSystemAndLog("../../etc/passwd").contains("/")
						&& !ExcelOperations.sanitizeForFileSystemAndLog("../../etc/passwd").contains(".."));

		failures += check("embedded newline cannot forge a fake log line",
				!ExcelOperations.sanitizeForFileSystemAndLog("Acme\n2026-01-01 ERROR fake line").contains("\n"));

		failures += check("embedded carriage return is stripped",
				!ExcelOperations.sanitizeForFileSystemAndLog("Acme\rCorp").contains("\r"));

		failures += check("null input is handled safely",
				"".equals(ExcelOperations.sanitizeForFileSystemAndLog(null)));

		failures += check("a value that is only unsafe characters becomes empty or underscores, not null",
				ExcelOperations.sanitizeForFileSystemAndLog("///") != null);

		// Different raw customer IDs can sanitize to the same string (e.g. "ABC/123" and "ABC\123"
		// both become "ABC_123"). Customer ID is the unique/dedup key, so ExcelOperations must never
		// silently accept a sanitized ID that differs from the original - it must reject the row
		// instead. These checks verify the exact guard condition used for that: comparing a value
		// against its own sanitized form to decide whether to reject it.
		String idWithSlash = "ABC/123";
		String idWithBackslash = "ABC\\123";
		failures += check("two different unsafe customer IDs would sanitize to the same string "
				+ "(confirms why they must be rejected, not silently accepted)",
				ExcelOperations.sanitizeForFileSystemAndLog(idWithSlash)
						.equals(ExcelOperations.sanitizeForFileSystemAndLog(idWithBackslash)));

		failures += check("an unsafe customer ID is detected as needing rejection (sanitized form differs)",
				!idWithSlash.equals(ExcelOperations.sanitizeForFileSystemAndLog(idWithSlash)));

		failures += check("a normal customer ID is detected as safe to accept (sanitized form is identical)",
				"CUST00123".equals(ExcelOperations.sanitizeForFileSystemAndLog("CUST00123")));

		if (failures == 0) {
			logger.info("All ExcelOperations sanitization checks passed.");
		} else {
			logger.error("{} ExcelOperations sanitization check(s) failed.", failures);
			System.exit(1);
		}
	}

	private static int check(String description, boolean passed) {
		if (passed) {
			logger.info("[PASS] {}", description);
			return 0;
		}
		logger.error("[FAIL] {}", description);
		return 1;
	}
}
