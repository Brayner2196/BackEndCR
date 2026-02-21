package com.backendcr.residentialcomplex.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorResponse {
    
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
		private LocalDateTime timestamp;
		private int status;
		private String error;
		private String message;
		private String path;
		private Map<String, String> validationErrors;

		public Builder timestamp(LocalDateTime timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public Builder status(int status) {
			this.status = status;
			return this;
		}

		public Builder error(String error) {
			this.error = error;
			return this;
		}

		public Builder message(String message) {
			this.message = message;
			return this;
		}

		public Builder path(String path) {
			this.path = path;
			return this;
		}

		public Builder validationErrors(Map<String, String> validationErrors) {
			this.validationErrors = validationErrors;
			return this;
		}

		public ErrorResponse build() {
			ErrorResponse errorResponse = new ErrorResponse();
			errorResponse.setTimestamp(this.timestamp);
			errorResponse.setStatus(this.status);
			errorResponse.setError(this.error);
			errorResponse.setMessage(this.message);
			errorResponse.setPath(this.path);
			errorResponse.setValidationErrors(this.validationErrors);
			return errorResponse;
		}
	}
	public LocalDateTime getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public Map<String, String> getValidationErrors() {
		return validationErrors;
	}
	public void setValidationErrors(Map<String, String> validationErrors) {
		this.validationErrors = validationErrors;
	}
    
     
    
    
}
