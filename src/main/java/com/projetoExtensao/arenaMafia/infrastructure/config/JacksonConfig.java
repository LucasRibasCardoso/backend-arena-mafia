package com.projetoExtensao.arenaMafia.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

public class JacksonConfig {

  private static final String DATE_FORMAT = "dd/MM/yyyy";

  @Bean
  public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(formatter));
    javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(formatter));

    return new Jackson2ObjectMapperBuilder()
        .modules(javaTimeModule, new StringTrimModuleConfig())
        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .simpleDateFormat(DATE_FORMAT);
  }

  @Bean
  public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
    return builder.build();
  }
}
