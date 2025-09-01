package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.projetoExtensao.arenaMafia.application.auth.port.gateway.SmsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsAdapter implements SmsPort {

  private static final Logger logger = LoggerFactory.getLogger(SmsAdapter.class);

  @Override
  public void send(String phoneNumber, String message) {
    // TODO: Implementar a integração real com um gateway de SMS
    logger.info("--- SIMULANDO ENVIO DE SMS ---");
    logger.info("Para: {}", phoneNumber);
    logger.info("Mensagem: {}", message);
    logger.info("-----------------------------");
  }
}
