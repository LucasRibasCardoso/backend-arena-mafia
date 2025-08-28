package com.projetoExtensao.arenaMafia.unit.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.repository.UserRepositoryAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.UserMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para UserRepositoryAdapter")
public class UserRepositoryAdapterTest {

  @Mock private UserJpaRepository userJpaRepository;

  @Mock private UserMapper userMapper;

  @InjectMocks private UserRepositoryAdapter userRepositoryAdapter;

  @Nested
  @DisplayName("Testes para o método save")
  class SaveTests {

    @Test
    @DisplayName("Deve mapear corretamente um User para userEntity, salvar e retornar um User")
    void save_shouldMapperUserAndSave() {
      // Arrange
      User userToSave = User.create("usernameTest", "User Test", "55912345678", "password_hash");
      UserEntity userEntityMapped = new UserEntity();
      UserEntity userEntitySaved = new UserEntity();
      User userReturned = userToSave;

      when(userMapper.toEntity(userToSave)).thenReturn(userEntityMapped);
      when(userJpaRepository.save(userEntityMapped)).thenReturn(userEntitySaved);
      when(userMapper.toDomain(userEntitySaved)).thenReturn(userReturned);

      // Act
      User result = userRepositoryAdapter.save(userToSave);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(userToSave.getId());
      assertThat(result.getUsername()).isEqualTo(userToSave.getUsername());
      assertThat(result.getFullName()).isEqualTo(userToSave.getFullName());
      assertThat(result.getPhone()).isEqualTo(userToSave.getPhone());
      assertThat(result.getPasswordHash()).isEqualTo(userToSave.getPasswordHash());

      verify(userMapper, times(1)).toEntity(userToSave);
      verify(userJpaRepository, times(1)).save(userEntityMapped);
      verify(userMapper, times(1)).toDomain(userEntitySaved);
    }
  }

  @Nested
  @DisplayName("Testes para o método findByUsername")
  class FindByUsernameTests {

    @Test
    @DisplayName("Deve encontrar um userEntity pelo username e retornar um User mapeado")
    void findByUsername_shouldReturnMappedUser() {
      // Arrange
      String username = "usernameTest";
      UserEntity userEntity = new UserEntity();
      User userMapped = User.create("usernameTest", "User Test", "55912345678", "password_hash");

      when(userJpaRepository.findByUsername(username)).thenReturn(Optional.of(userEntity));
      when(userMapper.toDomain(userEntity)).thenReturn(userMapped);

      // Act
      Optional<User> result = userRepositoryAdapter.findByUsername(username);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isPresent();
      assertThat(result.get().getUsername()).isEqualTo(username);

      verify(userJpaRepository, times(1)).findByUsername(username);
      verify(userMapper, times(1)).toDomain(userEntity);
    }

    @Test
    @DisplayName("Deve retornar um optional vazio quando não encontrar um User pelo username")
    void findByUsername_shouldReturnEmptyOptionalWhenNotFound() {
      // Arrange
      String username = "nonExistentUser";

      when(userJpaRepository.findByUsername(username)).thenReturn(Optional.empty());

      // Act
      var result = userRepositoryAdapter.findByUsername(username);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();

      verify(userJpaRepository, times(1)).findByUsername(username);
      verify(userMapper, never()).toDomain(any());
    }
  }

  @Nested
  @DisplayName("Testes para o método findByPhone")
  class FindByPhoneTests {

    @Test
    @DisplayName("Deve encontrar um userEntity pelo telefone e retornar um User mapeado")
    void findByPhone_shouldReturnMappedUser() {
      // Arrange
      String phone = "+55912345678";
      UserEntity userEntity = new UserEntity();
      User userMapped = User.create("usernameTest", "User Test", "+55912345678", "password_hash");

      when(userJpaRepository.findByPhone(phone)).thenReturn(Optional.of(userEntity));
      when(userMapper.toDomain(userEntity)).thenReturn(userMapped);

      // Act
      Optional<User> result = userRepositoryAdapter.findByPhone(phone);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isPresent();
      assertThat(result.get().getPhone()).isEqualTo(phone);

      verify(userJpaRepository, times(1)).findByPhone(phone);
      verify(userMapper, times(1)).toDomain(userEntity);
    }

    @Test
    @DisplayName("Deve retornar um optional vazio quando não encontrar um User pelo telefone")
    void findByPhone_shouldReturnEmptyOptionalWhenNotFound() {
      // Arrange
      String phone = "+55900000000";

      when(userJpaRepository.findByPhone(phone)).thenReturn(Optional.empty());

      // Act
      var result = userRepositoryAdapter.findByPhone(phone);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();

      verify(userJpaRepository, times(1)).findByPhone(phone);
      verify(userMapper, never()).toDomain(any());
    }
  }

  @Nested
  @DisplayName("Testes para o método findById")
  class FindByIdTests {

    @Test
    @DisplayName("Deve encontrar um userEntity pelo ID e retornar um optional de User mapeado")
    void findById_shouldReturnOptionalUser() {
      // Arrange
      User userMapped = User.create("usernameTest", "User Test", "55912345678", "password_hash");
      UUID userId = userMapped.getId();
      UserEntity userEntity = new UserEntity();

      when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
      when(userMapper.toDomain(userEntity)).thenReturn(userMapped);

      // Act
      Optional<User> result = userRepositoryAdapter.findById(userId);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(userId);
      assertThat(result.get().getUsername()).isEqualTo(userMapped.getUsername());

      verify(userJpaRepository, times(1)).findById(userId);
      verify(userMapper, times(1)).toDomain(userEntity);
    }

    @Test
    @DisplayName("Deve retornar um optional vazio quando não encontrar um User pelo ID")
    void findById_shouldReturnEmptyOptionalWhenNotFound() {
      // Arrange
      UUID userId = UUID.randomUUID();

      when(userJpaRepository.findById(userId)).thenReturn(Optional.empty());

      // Act
      var result = userRepositoryAdapter.findById(userId);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();

      verify(userJpaRepository, times(1)).findById(userId);
      verify(userMapper, never()).toDomain(any());
    }
  }

  @Nested
  @DisplayName("Testes para o método existsByUsername")
  class ExistByUsernameTests {

    @ParameterizedTest(name = "Quando o username existe = {0}, deve retornar {0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("Deve retornar o resultado esperado para a existência do username")
    void existsByUsername_shouldReturnExpectedBoolean(boolean usernameExists) {
      // Arrange
      String username = "testuser";
      when(userJpaRepository.existsByUsername(username)).thenReturn(usernameExists);

      // Act
      boolean actualResult = userRepositoryAdapter.existsByUsername(username);

      // Assert
      assertThat(actualResult).isEqualTo(usernameExists);
      verify(userJpaRepository, times(1)).existsByUsername(username);
    }
  }

  @Nested
  @DisplayName("Testes para o método existsByPhone")
  class ExistsByPhoneTests {

    @ParameterizedTest(name = "Quando o telefone existe = {0}, deve retornar {0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("Deve retornar o resultado esperado para a existência do telefone")
    void existsByPhone_shouldReturnExpectedBoolean(boolean phoneExists) {
      // Arrange
      String phone = "+5547988887777";
      when(userJpaRepository.existsByPhone(phone)).thenReturn(phoneExists);

      // Act
      boolean actualResult = userRepositoryAdapter.existsByPhone(phone);

      // Assert
      assertThat(actualResult).isEqualTo(phoneExists);
      verify(userJpaRepository, times(1)).existsByPhone(phone);
    }
  }

}
