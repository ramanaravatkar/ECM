package com.esun.fn.connection;

/**
 * Thrown when a connection to the FileNet Content Engine cannot be established.
 */
public class FileNetConnectionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FileNetConnectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
