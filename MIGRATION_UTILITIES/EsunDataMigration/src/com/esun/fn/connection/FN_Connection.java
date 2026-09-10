package com.esun.fn.connection;

import javax.security.auth.Subject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.config.ConfigReader;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;

public class FN_Connection {

	private static final Logger logger = LogManager.getLogger(FN_Connection.class);

	private Connection connection;
	private Domain domain;
	private ObjectStore objectStore;

	public ObjectStore getFileNetConnection() 
	{

		try {

			String ceUri = ConfigReader.getProperty("ce.uri");
			String username = ConfigReader.getProperty("username");
			String password = ConfigReader.getProperty("password");
			String stanza = ConfigReader.getProperty("stanza");
			String objectStoreName = ConfigReader.getProperty("objectstore");

			
			logger.info("Connecting to FileNet...");
			logger.info("CE URI       : {}");
			logger.info("Object Store : {}");
			Connection connection = Factory.Connection.getConnection(ceUri);

			Subject subject = UserContext.createSubject(connection, username, password, stanza);

			UserContext.get().pushSubject(subject);

			Domain domain = Factory.Domain.fetchInstance(connection, null, null);

			ObjectStore objectStore = Factory.ObjectStore.fetchInstance(domain, objectStoreName, null);

			
			logger.info("CE URI       : " + ceUri);
			logger.info("Object Store : " + objectStoreName);
			logger.info("Domain       : " + domain.get_Name());
			logger.info("Object Store : " + objectStore.get_DisplayName());
			logger.info("User         : " + username);

			return objectStore;

		} catch (Exception e) {

			logger.error("Failed to connect to FileNet.", e);

			throw new RuntimeException("FileNet Connection Failed", e);

		}

	}

	public void disconnect() {

		try {

			UserContext.get().popSubject();

			logger.info("Disconnected from FileNet.");

		} catch (Exception e) {

			logger.error("Error while disconnecting FileNet session.", e);

		}

	}

}