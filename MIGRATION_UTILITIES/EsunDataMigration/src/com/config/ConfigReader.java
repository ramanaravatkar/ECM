package com.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ConfigReader {
	private static final Logger logger = LogManager.getLogger(ConfigReader.class);
	private static final Properties properties = new Properties();
	// Relative to the application's working directory; the config path itself cannot come from
	// configuration, so this bootstrap location is intentionally fixed.
	private static final String CONFIG_PATH = "Configfile\\EsunConfig.properties";

	static {
		logger.info("Loading configuration file from path: {}", CONFIG_PATH);
		try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
			properties.load(fis);
			logger.info("Configuration file loaded successfully");
		} catch (IOException e) {
			logger.error("Failed to load configuration file from path: {}", CONFIG_PATH, e);
			throw new ConfigurationException("Unable to load configuration file.", e);
		}
	}

	private ConfigReader() {
		throw new IllegalStateException("Utility class");
	}

	public static String getProperty(String key) {
		return properties.getProperty(key);
	}
}
