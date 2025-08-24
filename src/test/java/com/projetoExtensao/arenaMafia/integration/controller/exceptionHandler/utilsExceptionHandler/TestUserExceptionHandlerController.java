package com.projetoExtensao.arenaMafia.integration.controller.exceptionHandler.utilsExceptionHandler;

import com.projetoExtensao.arenaMafia.domain.exception.user.BadPhoneNumberException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/exceptions/user/")
public class TestUserExceptionHandlerController {

  @GetMapping("/bad-phone-number")
  public void throwBadPhoneNumber() {
    throw new BadPhoneNumberException("Número de telefone inválido");
  }
}
