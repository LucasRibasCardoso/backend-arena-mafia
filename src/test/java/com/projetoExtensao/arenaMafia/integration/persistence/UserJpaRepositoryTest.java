package com.projetoExtensao.arenaMafia.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@DisplayName("Testes de integração de persistência para UserJpaRepository")
public class UserJpaRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private UserJpaRepository userJpaRepository;

  @Nested
  @DisplayName("Testes para o método findByUsername")
  class FindByUsernameTests {

    @Test
    @DisplayName("Deve encontrar um usuário com sucesso pelo seu username")
    void findByUsername_shouldReturnUserWhenUsernameExists() {
      // Arrange
      createAndPersistUser("usernameTest", "5547912345678");

      // Act
      Optional<UserEntity> foundUser = userJpaRepository.findByUsername("usernameTest");

      // Assert
      assertThat(foundUser).isPresent();
      assertThat(foundUser.get().getUsername()).isEqualTo("usernameTest");
    }

    @Test
    @DisplayName("Deve retornar vazio quando o username não existir")
    void findByUsername_shouldReturnEmptyWhenUsernameDoesNotExist() {
      // Act
      Optional<UserEntity> foundUser = userJpaRepository.findByUsername("nonExistentUsername");

      // Assert
      assertThat(foundUser).isEmpty();
    }
  }

  @Nested
  @DisplayName("Testes para o método findById")
  class FindByIdTests {

    @Test
    @DisplayName("Deve encontrar um usuário com sucesso pelo seu ID")
    void findById_shouldReturnUserWhenIdExists() {
      // Arrange
      UserEntity userEntity = createAndPersistUser("usernameTest", "5547912345678");

      // Act
      Optional<UserEntity> foundUser = userJpaRepository.findById(userEntity.getId());

      // Assert
      assertThat(foundUser).isPresent();
      assertThat(foundUser.get().getId()).isEqualTo(userEntity.getId());
    }

    @Test
    @DisplayName("Deve retornar vazio quando o ID não existir")
    void findById_shouldReturnEmptyWhenIdDoesNotExist() {
      // Act
      Optional<UserEntity> foundUser = userJpaRepository.findById(UUID.randomUUID());

      // Assert
      assertThat(foundUser).isEmpty();
    }
  }

  @Nested
  @DisplayName("Testes para o método existsByUsernameOrPhone")
  class ExistsByTests {

    @ParameterizedTest(name = "Quando {5}, deve retornar {4}")
    @CsvSource({
      "'user-db',   '5547988887777',  'user-db',   '5547911112222', true,  'username existe e telefone não'",
      "'user-db',   '5547988887777',  'otheruser', '5547988887777', true,  'telefone existe e username não'",
      "'user-db',   '5547988887777',  'user-db',   '5547988887777', true,  'ambos existem no mesmo registro'",
      "'null',      'null',           'any-user',  '5547911112222', false, 'nenhum existe (banco de dados vazio)'"
    })
    @DisplayName("Deve retornar o resultado esperado para a existência de username OU telefone")
    void existsByUsernameOrPhone_shouldReturnExpectedResult(
        String usernameToPersist,
        String phoneToPersist,
        String usernameToQuery,
        String phoneToQuery,
        boolean expectedResult,
        String description) {

      // Persistir o usuário no banco de dados apenas se usernameToPersist não for "null
      if (!"null".equals(usernameToPersist)) {
        createAndPersistUser(usernameToPersist, phoneToPersist);
      }

      // Act
      boolean exists = userJpaRepository.existsByUsernameOrPhone(usernameToQuery, phoneToQuery);

      // Assert
      assertThat(exists).isEqualTo(expectedResult);
    }
  }

  // Metodo auxiliar para criar e persistir um usuário no banco de dados
  private UserEntity createAndPersistUser(String username, String phone) {
    UserEntity userEntity = new UserEntity();
    userEntity.setId(UUID.randomUUID());
    userEntity.setUsername(username);
    userEntity.setFullName("Test User");
    userEntity.setPhone(phone);
    userEntity.setPasswordHash("hashedPassword");
    userEntity.setRole(RoleEnum.ROLE_USER);
    userEntity.setCreatedAt(Instant.now());
    userEntity.setStatus(AccountStatus.ACTIVE);
    return entityManager.persistAndFlush(userEntity);
  }
}
