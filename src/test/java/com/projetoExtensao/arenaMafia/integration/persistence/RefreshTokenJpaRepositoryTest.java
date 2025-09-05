package com.projetoExtensao.arenaMafia.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.RefreshTokenEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@DisplayName("Testes de integração de persistência para RefreshTokenRepository")
public class RefreshTokenJpaRepositoryTest {

  private static final AtomicInteger userCounter = new AtomicInteger(0);

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

  @Nested
  @DisplayName("Testes para o método deleteAllByUserIn")
  class DeleteAllByUserInTests {

    @Test
    @DisplayName("Deve deletar todos os tokens associados a uma lista de usuários")
    void deleteAllByUserIn_shouldDeleteAllTokensFromUserList() {
      // Arrange
      RefreshTokenEntity refreshToken1 = createAndPersistRefreshToken("token1");
      RefreshTokenEntity refreshToken2 = createAndPersistRefreshToken("token2");
      RefreshTokenEntity refreshToken3 = createAndPersistRefreshToken("token3");

      UserEntity user1 = refreshToken1.getUser();
      UserEntity user2 = refreshToken2.getUser();

      // Act
      // Deleta os tokens associados apenas aos usuários 1 e 2
      refreshTokenJpaRepository.deleteAllByUserIn(List.of(user1, user2));
      testEntityManager.flush();
      testEntityManager.clear();

      // Assert
      // Verifica se os tokens dos usuários 1 e 2 foram deletados
      Optional<RefreshTokenEntity> deletedToken1 =
          refreshTokenJpaRepository.findById(refreshToken1.getId());
      Optional<RefreshTokenEntity> deletedToken2 =
          refreshTokenJpaRepository.findById(refreshToken2.getId());

      // Verifica se o token do usuário 3 NÃO foi deletado
      Optional<RefreshTokenEntity> remainingToken3 =
          refreshTokenJpaRepository.findById(refreshToken3.getId());

      assertThat(deletedToken1).isEmpty();
      assertThat(deletedToken2).isEmpty();
      assertThat(remainingToken3).isPresent();
      assertThat(remainingToken3.get().getId()).isEqualTo(refreshToken3.getId());
    }

    @Test
    @DisplayName("Não deve fazer nada quando a lista de usuários for vazia")
    void deleteAllByUserIn_shouldDoNothingWhenUserListIsEmpty() {
      // Arrange
      RefreshTokenEntity refreshToken = createAndPersistRefreshToken("token");
      long countBefore = refreshTokenJpaRepository.count();

      // Act
      refreshTokenJpaRepository.deleteAllByUserIn(List.of());

      // Assert
      long countAfter = refreshTokenJpaRepository.count();
      Optional<RefreshTokenEntity> notDeletedToken =
          refreshTokenJpaRepository.findById(refreshToken.getId());

      assertThat(countAfter).isEqualTo(countBefore).isEqualTo(1);
      assertThat(notDeletedToken).isPresent();
    }
  }

  private RefreshTokenEntity createAndPersistRefreshToken(String token) {
    UserEntity userEntity = createAndPersistUser();
    var refreshToken = new RefreshTokenEntity();
    refreshToken.setToken(token);
    refreshToken.setUser(userEntity);
    refreshToken.setCreatedAt(Instant.now());
    refreshToken.setExpiryDate(Instant.now().plus(7L, ChronoUnit.DAYS));
    return testEntityManager.persist(refreshToken);
  }

  private UserEntity createAndPersistUser() {
    Instant now = Instant.now();
    int count = userCounter.incrementAndGet();

    UserEntity userEntity = new UserEntity();
    userEntity.setId(UUID.randomUUID());
    userEntity.setUsername("user_" + count); // Gera user_1, user_2, etc.
    userEntity.setFullName("Test User " + count);
    userEntity.setPhone("+551190000000" + count);
    userEntity.setPasswordHash("hashedPassword");
    userEntity.setRole(RoleEnum.ROLE_USER);
    userEntity.setCreatedAt(now);
    userEntity.setUpdatedAt(now);
    userEntity.setStatus(AccountStatus.ACTIVE);
    return testEntityManager.persistAndFlush(userEntity);
  }
}
