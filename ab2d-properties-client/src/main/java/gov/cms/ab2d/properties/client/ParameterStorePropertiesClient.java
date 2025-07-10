package gov.cms.ab2d.properties.client;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.ParameterType;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

import java.util.List;

import static java.lang.String.format;

@Slf4j
public class ParameterStorePropertiesClient implements PropertiesClient {

    public enum Ab2dEnvironment {
        DEV,
        TEST,
        PROD
    }

    private final SsmClient ssmClient;
    private final Ab2dEnvironment environment;

    // Derive AB2D environment based on `AB2D_EXECUTION_ENV` environment variable
    public ParameterStorePropertiesClient() {
        this(deriveAb2dEnvironment());
    }

    public ParameterStorePropertiesClient(@Nonnull Ab2dEnvironment environment) {
        this(
            SsmClient.builder().region(Region.US_EAST_1).build(),
            environment
        );
    }

    public ParameterStorePropertiesClient(SsmClient ssmClient, Ab2dEnvironment environment) {
        this.ssmClient = ssmClient;
        this.environment = environment;
        log.info("Configured properties client for ab2d-{}", this.environment.name().toLowerCase());
    }

    protected static Ab2dEnvironment deriveAb2dEnvironment() {
        var env = System.getenv("AB2D_EXECUTION_ENV");
        if (env == null) {
            throw new PropertiesClientException("Environment variable 'AB2D_EXECUTION_ENV' not set");
        }
        env = env.toLowerCase();
        if (env.contains("dev")) {
            return Ab2dEnvironment.DEV;
        } if (env.contains("impl") || env.contains("test")) {
            return Ab2dEnvironment.TEST;
        } if (env.contains("sandbox")) {
            return Ab2dEnvironment.SANDBOX;
        } if (env.contains("prod")) {
            return Ab2dEnvironment.PROD;
        }
        throw new PropertiesClientException("Unable to determine environment based on 'AB2D_EXECUTION_ENV'");
    }

    protected String buildAbsolutePathForKey(String key) {
        return format(
            "/ab2d/%s/properties/%s",
            this.environment.name().toLowerCase(),
            key
        );
    }

    protected String getValue(String key) {
        val path = buildAbsolutePathForKey(key);
        var getParameterRequest = GetParameterRequest.builder()
                .name(path)
                .withDecryption(true)
                .build();

        var parameterResponse = ssmClient.getParameter(getParameterRequest);
        return parameterResponse.parameter().value();
    }

    protected void setParameter(String key, String value) {
        val path = buildAbsolutePathForKey(key);
        var putParameterRequest = PutParameterRequest.builder()
                .name(path)
                .type(ParameterType.STRING)
                .value(value)
                .build();
        ssmClient.putParameter(putParameterRequest);
    }

    @Override
    public List<Property> getAllProperties() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Property getProperty(String key) {
        try {
            return new Property(key, getValue(key));
        } catch (ParameterNotFoundException e) {
            throw new PropertyNotFoundException(format("Property '%s' is not defined", buildAbsolutePathForKey(key)), e);
        } catch (Exception e) {
            throw new PropertyNotFoundException(format("Cannot find the property '%s'", buildAbsolutePathForKey(key)), e);
        }
    }

    @Override
    public Property setProperty(String key, String value) {
        try {
            setParameter(key, value);
        } catch (Exception e) {
            throw new PropertyNotFoundException(format("Cannot save the property '%s'", buildAbsolutePathForKey(key)), e);
        }
        return new Property(key, value);
    }

    @Override
    public void deleteProperty(String key) {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) {
        /**

         Set the following environment variables before running:

         AB2D_EXECUTION_ENV
         AWS_DEFAULT_REGION
         AWS_ACCESS_KEY_ID
         AWS_SECRET_ACCESS_KEY
         AWS_SESSION_TOKEN
         */

        PropertiesClient propertiesClient = new ParameterStorePropertiesClient();
        // Try to find parameter at e.g. `/ab2d/dev/properties/testing` assuming AB2D_EXECUTION_ENV=ab2d-dev
        System.out.println(propertiesClient.getProperty("testing"));
        System.out.println(propertiesClient.getProperty("maintenance.mode"));
    }

}
