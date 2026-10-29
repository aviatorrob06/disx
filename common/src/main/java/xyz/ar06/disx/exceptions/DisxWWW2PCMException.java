package xyz.ar06.disx.exceptions;

import xyz.ar06.disx.DisxLogger;

public class DisxWWW2PCMException extends DisxException{
    public int statusCode;
    public boolean resourceNotFound = false;
    public DisxWWW2PCMException(String message) {
        super(message);
    }

    public DisxWWW2PCMException(String message, Throwable cause) {
        super(message, cause);
    }

    public DisxWWW2PCMException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public DisxWWW2PCMException(String message, int statusCode, boolean wasResourceNotFound) {
        super(message);
        this.statusCode = statusCode;
        this.resourceNotFound = wasResourceNotFound;
    }

    @Override
    public String toString() {
        return super.toString() + " (HTTP Status: " + statusCode + ")";
    }

    public boolean wasInternalServerError(){
        return (statusCode == 500);
    }

    public boolean wasUnreachableServer(){
        return (statusCode == 502 || statusCode == 503);
    }

    public boolean serverTookTooLong(){
        return (statusCode == 504);
    }

    public boolean wasBadRequest(){
        return (statusCode == 400);
    }

    public boolean wasEndpointNotFound(){
        return (statusCode == 404);
    }

    public boolean wasForbiddenRequest(){
        return (statusCode == 400);
    }

    public boolean wasResourceNotFound(){
        return resourceNotFound;
    }

    public boolean wasTooManyRequests(){
        return (statusCode == 429);
    }

}
