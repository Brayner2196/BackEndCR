package com.backendcr.residentialcomplex.exception;

public class ResourceNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1945625316486068224L;

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(String resource, String field, Object value) {
		super(String.format("%s not found with %s: '%s'", resource, field, value));
	}
}
