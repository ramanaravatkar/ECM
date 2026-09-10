package com.config;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Encrypts/decrypts configuration values (e.g. the FileNet connection password stored in
 * EsunConfig.properties).
 *
 * Current format: AES-256/GCM/NoPadding with a fresh random 96-bit nonce per call, prefixed with
 * {@link #GCM_PREFIX} and Base64 encoded: base64(IV || ciphertext || authTag).
 *
 * Values produced by the previous implementation (AES/CBC/PKCS5Padding with a fixed, hard-coded
 * IV and an MD5-derived key) have no prefix. decodeSLM still accepts that legacy format so
 * existing encrypted values keep working; encodeSLM always produces the new secure format.
 */
public final class ESUNPasswordEncryption {

	private static final Logger logger = LogManager.getLogger(ESUNPasswordEncryption.class);

	private static final String ALGORITHM = "AES";

	private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";
	private static final String KEY_DIGEST = "SHA-256";
	private static final byte[] KEY_SEED = "ESUNPasswordEncryption".getBytes(StandardCharsets.UTF_8);
	private static final int GCM_IV_LENGTH_BYTES = 12;
	private static final int GCM_TAG_LENGTH_BITS = 128;
	private static final String GCM_PREFIX = "$AESGCM$";

	// Legacy (pre-remediation) format, kept only so values encrypted before this fix remain readable.
	private static final String LEGACY_TRANSFORMATION = "AES/CBC/PKCS5Padding";
	private static final String LEGACY_DIGEST = "MD5";
	private static final byte[] LEGACY_KEY_SEED = "ESUNPasswordEncryption".getBytes(StandardCharsets.UTF_8);
	private static final byte[] LEGACY_IV_SEED = "ESUNPassword".getBytes(StandardCharsets.UTF_8);

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private ESUNPasswordEncryption() {
		throw new IllegalStateException("Utility class");
	}

	public static String encodeSLM(String password) {
		try {
			SecretKey secretKey = deriveKey();

			byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
			SECURE_RANDOM.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

			byte[] cipherText = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

			byte[] combined = new byte[iv.length + cipherText.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

			return GCM_PREFIX + Base64.getEncoder().encodeToString(combined);

		} catch (GeneralSecurityException e) {
			logger.error("Failed to encrypt value", e);
			return null;
		}
	}

	public static String decodeSLM(String encodedPassword) {
		if (encodedPassword == null) {
			return null;
		}
		try {
			if (encodedPassword.startsWith(GCM_PREFIX)) {
				return decodeGcm(encodedPassword.substring(GCM_PREFIX.length()));
			}

			logger.warn("Decrypting a value stored in the legacy AES/CBC format. "
					+ "Re-encode it with encodeSLM() and update the configuration to migrate to AES/GCM.");
			return decodeLegacyCbc(encodedPassword);

		} catch (GeneralSecurityException | IllegalArgumentException e) {
			logger.error("Failed to decrypt value", e);
			return null;
		}
	}

	private static String decodeGcm(String base64Payload) throws GeneralSecurityException {
		byte[] combined = Base64.getDecoder().decode(base64Payload);

		if (combined.length < GCM_IV_LENGTH_BYTES) {
			throw new IllegalArgumentException("Encrypted payload is too short to contain a valid nonce.");
		}

		byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
		byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH_BYTES];
		System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
		System.arraycopy(combined, GCM_IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

		SecretKey secretKey = deriveKey();
		Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

		byte[] plainText = cipher.doFinal(cipherText);
		return new String(plainText, StandardCharsets.UTF_8);
	}

	private static SecretKey deriveKey() throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(KEY_DIGEST);
		return new SecretKeySpec(digest.digest(KEY_SEED), ALGORITHM);
	}

	@Deprecated
	private static String decodeLegacyCbc(String encodedPassword) throws GeneralSecurityException {
		MessageDigest digest = MessageDigest.getInstance(LEGACY_DIGEST);
		SecretKey secretKey = new SecretKeySpec(digest.digest(LEGACY_KEY_SEED), ALGORITHM);
		byte[] ivHash = digest.digest(LEGACY_IV_SEED);

		Cipher cipher = Cipher.getInstance(LEGACY_TRANSFORMATION);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivHash));

		byte[] decoded = Base64.getDecoder().decode(encodedPassword);
		byte[] plainText = cipher.doFinal(decoded);
		return new String(plainText, StandardCharsets.UTF_8);
	}

	public static void main(String[] args) {
		String sample = "Laxmi";
		String encrypted = encodeSLM(sample);
		logger.info("Encrypted value: {}", encrypted);
		// Do not log the decrypted value: this demo exists to sanity-check the round trip, not to
		// print secrets. Anyone adapting this to test a real password must not add plaintext logging.
		boolean roundTripOk = sample.equals(decodeSLM(encrypted));
		logger.info("Round trip successful: {}", roundTripOk);
	}
}
