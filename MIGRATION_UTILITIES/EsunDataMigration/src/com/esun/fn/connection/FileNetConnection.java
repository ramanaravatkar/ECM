package com.esun.fn.connection;

import javax.security.auth.Subject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.config.ConfigReader;
import com.config.ESUNPasswordEncryption;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;

public class FileNetConnection {
	private static final Logger logger = LogManager.getLogger(FileNetConnection.class);

	public ObjectStore getFileNetConnection() {
		try {
			String ceUri = ConfigReader.getProperty("ce.uri");
			String username = ConfigReader.getProperty("username");
			String password = ESUNPasswordEncryption.decodeSLM(ConfigReader.getProperty("password"));
			String stanza = ConfigReader.getProperty("stanza");
			String objectStoreName = ConfigReader.getProperty("objectstore");

			logger.info("FileNet connection initiated");
			logger.info("Connecting to CE URI: {}", ceUri);
			logger.info("Target Object Store: {}", objectStoreName);

			if (ceUri != null && ceUri.regionMatches(true, 0, "http://", 0, "http://".length())) {
				logger.warn("ce.uri uses plain HTTP; the FileNet username/password and document "
						+ "content will be sent unencrypted. Use an HTTPS endpoint if the CE server offers one.");
			}

			Connection connection = Factory.Connection.getConnection(ceUri);
			Subject subject = UserContext.createSubject(connection, username, password, stanza);
			UserContext.get().pushSubject(subject);
			Domain domain = Factory.Domain.fetchInstance(connection, null, null);
			ObjectStore objectStore = Factory.ObjectStore.fetchInstance(domain, objectStoreName, null);

			logger.info("Connected to FileNet successfully");
			logger.info("Domain: {}", domain.get_Name());
			logger.info("Object Store: {}", objectStore.get_DisplayName());

			return objectStore;

		} catch (Exception e) {
			logger.error("FileNet connection failed", e);
			throw new FileNetConnectionException("FileNet Connection Failed", e);
		}
	}

	public void disconnect() {
		try {
			UserContext.get().popSubject();
			logger.info("FileNet session disconnected successfully");
		} catch (Exception e) {
			logger.error("Failed to disconnect FileNet session", e);
		}
	}
}
