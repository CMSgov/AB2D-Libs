package gov.cms.ab2d.properties.client;

public class PropertiesClientException extends RuntimeException {
    public PropertiesClientException(String message) {
        super(message);
    }

    public PropertiesClientException(String message, Throwable ex) {
        super(message, ex);
    }
}
