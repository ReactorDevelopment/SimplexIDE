package com.reactordevelopment.simplex.simplexLang;
/**Exception class that stores the string value of the description for use*/
public class SimplexException extends Exception{
    /**The error description that is displayed*/
    private final String error;
    /**The numeric error code*/
    private final int code;
    /**Constructor
     * Initializes fields*/
    public SimplexException(String errorMessage, int code) {
        super(errorMessage);
        error = errorMessage;
        this.code = code;
    }
    /**Returns the error code*/
    public int getCode() { return code; }
    /**Returns the error description*/
    public String getError() { return error; }
}
