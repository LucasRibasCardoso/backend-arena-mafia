package com.projetoExtensao.arenaMafia.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.valueObjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.RefreshTokenEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@DisplayName("Testes de integração de persistência para RefreshTokenRepository")
public class RefreshTokenJpaRepositoryTest {

  @Autowired private TestEntityManager testEntityManager;

  @Autowired private RefreshTokenJpaRepository refreshTokenJpaRepository;

  @Nested
  @DisplayName("Testes para o método findByToken")
  class FindByTokenTests {

    @Test
    @DisplayName("Deve encontrar um token com sucesso")
    void findByToken_shouldReturnTokenWhenExists() {
      // Arrange
      String token = RefreshTokenVO.generate().toString();
      createAndPersistRefreshToken(token);

      // Act
      Optional<RefreshTokenEntity> findByToken = refreshTokenJpaRepository.findByToken(token);

      // Assert
      assertThat(findByToken).isPresent();
      assertThat(findByToken.get().getToken()).isEqualTo(token);
    }

    @Test
    @DisplayName("Deve retornar vazio quando o token não existir")
    void findByToken_shouldReturnEmptyWhenTokenDoesNotExist() {
      // Act
      Optional<RefreshTokenEntity> findByToken =
          refreshTokenJpaRepository.findByToken("nonExistentToken");

      // Assert
      assertThat(findByToken).isEmpty();
    }
  }

  @Nested
  @DisplayName("Testes para o método deleteByUser")
  class DeleteByUserTests {

    @Test
    @DisplayName("Deve deletar o refresh token associado ao usuário com sucesso")
    void deleteByUser_shouldDeleteTokenWhenUserExists() {
      // Arrange
      String token = RefreshTokenVO.generate().toString();
      RefreshTokenEntity refreshToken = createAndPersistRefreshToken(token);

      // Act
      refreshTokenJpaRepository.deleteByUser(refreshToken.getUser());

      // Assert
      Optional<RefreshTokenEntity> deletedToken =
          refreshTokenJpaRepository.findById(refreshToken.getId());

      assertThat(deletedToken).isEmpty();
    }
  }

  private RefreshTokenEntity createAndPersistRefreshToken(String token) {
    UserEntity userEntity = new UserEntity();
    userEntity.setId(UUID.randomUUID());
    userEntity.setUsername("testUser");
    userEntity.setFullName("Test User");
    userEntity.setPhone("5547912345678");
    userEntity.setPasswordHash("hashedPassword");
    userEntity.setRole(RoleEnum.ROLE_USER);
    userEntity.setCreatedAt(Instant.now());
    userEntity.setAccountNonLocked(true);
    userEntity.setEnabled(true);
    testEntityManager.persistAndFlush(userEntity);

    var refreshToken = new RefreshTokenEntity();
    refreshToken.setToken(token);
    refreshToken.setUser(userEntity);
    refreshToken.setCreatedAt(java.time.Instant.now());
    refreshToken.setExpiryDate(java.time.Instant.now().plus(7L, ChronoUnit.DAYS));
    return testEntityManager.persist(refreshToken);
  }
}
