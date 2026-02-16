package isa.jutjub.exception;

public class PremiereException extends RuntimeException {

    public PremiereException(String message) {
        super(message);
    }

    public PremiereException(String message, Throwable cause) {
        super(message, cause);
    }
}