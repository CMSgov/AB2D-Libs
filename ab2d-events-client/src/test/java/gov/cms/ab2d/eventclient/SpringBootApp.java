package gov.cms.ab2d.eventclient;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(
        scanBasePackages = {"gov.cms.ab2d.eventclient"}
//        exclude = {
//                io.awspring.cloud.autoconfigure.context.ContextInstanceDataAutoConfiguration.class,
//                io.awspring.cloud.autoconfigure.context.ContextStackAutoConfiguration.class,
//                io.awspring.cloud.autoconfigure.context.ContextRegionProviderAutoConfiguration.class,
//                io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration.class
//
//        }
)
public class SpringBootApp {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }

    @Bean
    public Ab2dEnvironment getEnvironment() {
        return Ab2dEnvironment.fromName("local");
    }
}

