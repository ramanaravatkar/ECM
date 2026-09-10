package com.config;

import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PasswordEncryption {

	private static final Logger logger = LogManager.getLogger(PasswordEncryption.class);
	
	public void getPasswordFromCMD() {
		try (Scanner sc = new Scanner(System.in)) {
			logger.info("Enter password");
			String enterPassword = sc.next();
			String encodePassword = ESUNPasswordEncryption.encodeSLM(enterPassword);
			logger.info("Encoded password: {}", encodePassword);
		} catch (Exception e) {
			logger.error("Error occurred while encoding password", e);
		}
	}

	public static void main(String[] args) {
		PasswordEncryption passwordEncryption= new PasswordEncryption();
		passwordEncryption.getPasswordFromCMD();
	}

}
