package xyz.ar06.disx.exceptions;

public class DisxDimensionBlacklistedException extends DisxException{
    public DisxDimensionBlacklistedException(String message) {
        super(message);
    }

    public DisxDimensionBlacklistedException(String message, Throwable cause) {
        super(message, cause);
    }
}
