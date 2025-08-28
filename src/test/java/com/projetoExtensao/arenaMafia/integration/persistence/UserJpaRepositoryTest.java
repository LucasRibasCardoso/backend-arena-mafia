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
  @DisplayName("Testes para o método findByPhone")
  class FindByPhoneTests {

    @Test
    @DisplayName("Deve encontrar um usuário com sucesso pelo seu telefone")
    void findByUsername_shouldReturnUserWhenUsernameExists() {
      // Arrange
      createAndPersistUser("usernameTest", "+5547912345678");

      // Act
      Optional<UserEntity> foundUser = userJpaRepository.findByPhone("+5547912345678");

      // Assert
      assertThat(foundUser).isPresent();
      assertThat(foundUser.get().getPhone()).isEqualTo("+5547912345678");
    }

    @Test
    @DisplayName("Deve retornar vazio quando o telefone não existir")
    void findByUsername_shouldReturnEmptyWhenUsernameDoesNotExist() {
      // Act
      Optional<UserEntity> foundUser = userJpaRepository.findByPhone("+5547999999999");

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
  @DisplayName("Testes para o método existsByUsername")
  class ExistsByUsernameTests {

    @Test
    @DisplayName("Deve retornar true quando o username existe no banco de dados")
    void existsByUsername_shouldReturnTrue_whenUsernameExists() {
      // Arrange
      createAndPersistUser("usernameTest", "+5547988887777");

      // Act
      boolean exists = userJpaRepository.existsByUsername("usernameTest");

      // Assert
      assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false quando o username não existe no banco de dados")
    void existsByUsername_shouldReturnFalse_whenUsernameDoesNotExist() {
      // Arrange (banco de dados está limpo)

      // Act
      boolean exists = userJpaRepository.existsByUsername("nonexistentuser");

      // Assert
      assertThat(exists).isFalse();
    }
  }

  @Nested
  @DisplayName("Testes para o método existsByPhone")
  class ExistsByPhoneTests {

    @Test
    @DisplayName("Deve retornar true quando o telefone existe no banco de dados")
    void existsByPhone_shouldReturnTrue_whenPhoneExists() {
      // Arrange
      createAndPersistUser("usernameTest", "+5547988887777");

      // Act
      boolean exists = userJpaRepository.existsByPhone("+5547988887777");

      // Assert
      assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false quando o telefone não existe no banco de dados")
    void existsByPhone_shouldReturnFalse_whenPhoneDoesNotExist() {
      // Arrange (banco de dados está limpo)

      // Act
      boolean exists = userJpaRepository.existsByPhone("+5547911112222");

      // Assert
      assertThat(exists).isFalse();
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
