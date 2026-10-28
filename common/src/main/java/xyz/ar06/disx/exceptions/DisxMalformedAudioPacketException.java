package xyz.ar06.disx.exceptions;

public class DisxMalformedAudioPacketException extends DisxException{
    public DisxMalformedAudioPacketException(String message) {
        super(message);
    }

    public DisxMalformedAudioPacketException(String message, Throwable cause) {
        super(message, cause);
    }
}
