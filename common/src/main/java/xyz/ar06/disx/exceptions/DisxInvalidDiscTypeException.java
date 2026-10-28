package xyz.ar06.disx.exceptions;

public class DisxInvalidDiscTypeException extends DisxException{
    public DisxInvalidDiscTypeException(String message) {
        super(message);
    }

    public DisxInvalidDiscTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
