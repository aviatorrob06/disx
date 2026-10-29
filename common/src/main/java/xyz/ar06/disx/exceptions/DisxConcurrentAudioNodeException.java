package xyz.ar06.disx.exceptions;

public class DisxConcurrentAudioNodeException extends DisxException{
    public DisxConcurrentAudioNodeException(String message) {
        super(message);
    }

    public DisxConcurrentAudioNodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
