package com.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Lightweight, dependency-free verification for {@link ESUNPasswordEncryption}.
 *
 * The project has no test framework (no JUnit dependency in lib/), so this follows the same
 * plain main()-driven style already used by ESUNPasswordEncryption.main() and
 * PasswordEncryption.main(). Run with the same classpath as the application; a non-zero exit
 * code means at least one check failed.
 */
public final class ESUNPasswordEncryptionTest {

	private static final Logger logger = LogManager.getLogger(ESUNPasswordEncryptionTest.class);

	private ESUNPasswordEncryptionTest() {
		throw new IllegalStateException("Utility class");
	}

	public static void main(String[] args) {
		int failures = 0;

		failures += check("round trip returns original plaintext", ESUNPasswordEncryptionTest::roundTripSucceeds);
		failures += check("two encryptions of the same plaintext differ",
				ESUNPasswordEncryptionTest::twoEncryptionsProduceDifferentCiphertext);
		failures += check("tampered ciphertext fails to decrypt",
				ESUNPasswordEncryptionTest::tamperedCiphertextIsRejected);
		failures += check("malformed input is handled safely, not thrown",
				ESUNPasswordEncryptionTest::malformedInputReturnsNull);
		failures += check("null input is handled safely", ESUNPasswordEncryptionTest::nullInputReturnsNull);
		failures += check("legacy AES/CBC ciphertext still decrypts",
				ESUNPasswordEncryptionTest::legacyCiphertextStillDecodes);

		if (failures == 0) {
			logger.info("All ESUNPasswordEncryption checks passed.");
		} else {
			logger.error("{} ESUNPasswordEncryption check(s) failed.", failures);
			System.exit(1);
		}
	}

	private static boolean roundTripSucceeds() {
		String plainText = "S0meTestP@ssw0rd!";
		String encoded = ESUNPasswordEncryption.encodeSLM(plainText);
		String decoded = ESUNPasswordEncryption.decodeSLM(encoded);
		return plainText.equals(decoded);
	}

	private static boolean twoEncryptionsProduceDifferentCiphertext() {
		String plainText = "S0meTestP@ssw0rd!";
		String first = ESUNPasswordEncryption.encodeSLM(plainText);
		String second = ESUNPasswordEncryption.encodeSLM(plainText);
		return !first.equals(second);
	}

	private static boolean tamperedCiphertextIsRejected() {
		String encoded = ESUNPasswordEncryption.encodeSLM("S0meTestP@ssw0rd!");
		char[] chars = encoded.toCharArray();
		int lastChar = chars.length - 2;
		chars[lastChar] = chars[lastChar] == 'A' ? 'B' : 'A';
		String tampered = new String(chars);
		return ESUNPasswordEncryption.decodeSLM(tampered) == null;
	}

	private static boolean malformedInputReturnsNull() {
		return ESUNPasswordEncryption.decodeSLM("not-valid-base64!@#") == null;
	}

	private static boolean nullInputReturnsNull() {
		return ESUNPasswordEncryption.decodeSLM(null) == null;
	}

	private static boolean legacyCiphertextStillDecodes() {
		// Produced by the pre-remediation AES/CBC (static IV, MD5 key) implementation for the
		// plaintext "LegacyTestPwd1". Confirms values encrypted before this fix are still readable.
		String legacyEncoded = "Jgcbu+BYS36HsGSGU5Exog==";
		return "LegacyTestPwd1".equals(ESUNPasswordEncryption.decodeSLM(legacyEncoded));
	}

	private static int check(String description, java.util.function.BooleanSupplier assertion) {
		boolean passed;
		try {
			passed = assertion.getAsBoolean();
		} catch (RuntimeException e) {
			logger.error("[FAIL] {} - threw {}", description, e.toString());
			return 1;
		}
		if (passed) {
			logger.info("[PASS] {}", description);
			return 0;
		}
		logger.error("[FAIL] {}", description);
		return 1;
	}
}
