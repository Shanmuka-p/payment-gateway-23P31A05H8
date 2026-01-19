package com.gateway.dto;

public class ErrorResponse {
    private final ErrorBody error;

    public ErrorResponse(String code, String description) {
        this.error = new ErrorBody(code, description);
    }

    public ErrorBody getError() {
        return error;
    }

    public static class ErrorBody {
        private final String code;
        private final String description;

        public ErrorBody(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
}
