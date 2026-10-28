package xyz.ar06.disx.exceptions;

public class DisxException extends RuntimeException{
    public DisxException(String message){
        super(message);
    }

    public DisxException(String message, Throwable cause){
        super(message, cause);
    }
}
