package xyz.ar06.disx.exceptions;

public class DisxMalformedURLException extends DisxException{
    public DisxMalformedURLException(String message) {
        super(message);
    }

    public DisxMalformedURLException(String message, Throwable cause) {
        super(message, cause);
    }
}
