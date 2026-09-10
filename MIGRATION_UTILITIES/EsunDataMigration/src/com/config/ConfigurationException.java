package com.config;

/**
 * Thrown when the application configuration cannot be loaded or is invalid.
 */
public class ConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}
