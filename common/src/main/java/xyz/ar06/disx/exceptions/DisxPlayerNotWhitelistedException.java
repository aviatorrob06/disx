package xyz.ar06.disx.exceptions;

public class DisxPlayerNotWhitelistedException extends DisxException{
    public DisxPlayerNotWhitelistedException(String message) {
        super(message);
    }

    public DisxPlayerNotWhitelistedException(String message, Throwable cause) {
        super(message, cause);
    }
}
