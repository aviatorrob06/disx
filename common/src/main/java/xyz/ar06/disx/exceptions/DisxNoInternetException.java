package xyz.ar06.disx.exceptions;

public class DisxNoInternetException extends DisxException{
    public DisxNoInternetException(String message) {
        super(message);
    }

    public DisxNoInternetException(String message, Throwable cause) {
        super(message, cause);
    }
}
