package xyz.ar06.disx.exceptions;

public class DisxPlayerBlacklistedException extends DisxException{
    public DisxPlayerBlacklistedException(String message) {
        super(message);
    }

    public DisxPlayerBlacklistedException(String message, Throwable cause) {
        super(message, cause);
    }
}
