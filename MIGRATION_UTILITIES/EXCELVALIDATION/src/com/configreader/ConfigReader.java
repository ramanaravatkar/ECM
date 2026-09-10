package com.configreader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {

	private static final Properties properties = new Properties();

	private static final String CONFIG_PATH = "resources\\config.properties";

	static {

		System.out.println("Loading configuration file from: " + CONFIG_PATH);

		try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {

			properties.load(fis);

			System.out.println("Configuration file loaded successfully.");

		} catch (IOException e) {

			System.err.println("Failed to load configuration file: " + CONFIG_PATH);

			e.printStackTrace();

			throw new RuntimeException("Unable to load configuration file.", e);
		}
	}

	public static String getProperty(String key) {
		return properties.getProperty(key);
	}
}