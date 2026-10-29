package xyz.ar06.disx.exceptions;

public class DisxTooLongException extends DisxException{
    public DisxTooLongException(String message) {
        super(message);
    }

    public DisxTooLongException(String message, Throwable cause) {
        super(message, cause);
    }
}
