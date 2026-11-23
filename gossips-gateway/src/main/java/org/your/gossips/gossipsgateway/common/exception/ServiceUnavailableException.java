package org.your.gossips.gossipsgateway.common.exception;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String service, Throwable cause) {
        super(service + " service is unavailable", cause);
    }
}
