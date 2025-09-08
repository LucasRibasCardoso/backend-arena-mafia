package com.projetoExtensao.arenaMafia.infrastructure.web.auth.util;

import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToRefreshTokenVOConverter implements Converter<String, RefreshTokenVO> {

  @Override
  public RefreshTokenVO convert(String source) {
    if (source.isBlank()) return null;
    return RefreshTokenVO.fromString(source);
  }
}
