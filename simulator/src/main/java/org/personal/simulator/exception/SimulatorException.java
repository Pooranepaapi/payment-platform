package org.personal.simulator.exception;

/**
 * Custom exception for simulator errors.
 */
public class SimulatorException extends RuntimeException {

    private final String errorCode;

    public SimulatorException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SimulatorException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
