package net.chariothy.db.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Henry Tian
 */
@Configuration
@ComponentScan(basePackages = {"net.chariothy.db"})
@ImportResource("classpath:spring.xml")
public class AppConfig {
    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

}
