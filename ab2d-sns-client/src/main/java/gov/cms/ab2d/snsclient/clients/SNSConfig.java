package gov.cms.ab2d.snsclient.clients;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class SNSConfig {

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.end-point.uri}")
    private String url;


    @Bean
    public AmazonSNSClient amazonSNS() {
        log.info("Locakstack url " + url);
        return (AmazonSNSClient) (
                null != url
                        ? AmazonSNSClient.builder()
                            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(url, region))
                        : AmazonSNSClientBuilder.standard()
        ).build();
    }

    @Bean
    public SNSClient snsClient(AmazonSNSClient amazonSNS, Ab2dEnvironment environment) {
        return new SNSClientImpl(amazonSNS, environment);
    }

}
