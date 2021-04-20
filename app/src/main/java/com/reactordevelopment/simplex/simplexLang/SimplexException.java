package com.reactordevelopment.simplex.simplexLang;
public class SimplexException extends Exception{
    private final String error;
    private final int code;
    public SimplexException(String errorMessage, int code) {
        super(errorMessage);
        error = errorMessage;
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String getError() {
        return error;
    }
}
