package io.github.nooshhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

/**
 * @author neals
 * @since 5/31/2022
 */
@Configuration
public class JacksonConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'"));
        return objectMapper;
    }
}
