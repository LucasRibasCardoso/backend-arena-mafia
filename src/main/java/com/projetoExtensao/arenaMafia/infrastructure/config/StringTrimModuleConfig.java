package com.projetoExtensao.arenaMafia.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;

public class StringTrimModuleConfig extends SimpleModule {

  public StringTrimModuleConfig() {
    super("StringTrimModule");
    addDeserializer(
        String.class,
        new StdDeserializer<String>(String.class) {
          @Override
          public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            return value != null ? value.trim() : null;
          }
        });
  }
}
