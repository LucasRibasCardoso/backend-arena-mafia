package com.projetoExtensao.arenaMafia.unit.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.UserRepositoryAdapter;
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
import org.junit.jupiter.params.provider.CsvSource;
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
      assertNotNull(result);
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
      assertNotNull(result);
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
      assertNotNull(result);
      assertThat(result).isEmpty();

      verify(userJpaRepository, times(1)).findByUsername(username);
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
      assertNotNull(result);
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
      assertNotNull(result);
      assertThat(result).isEmpty();

      verify(userJpaRepository, times(1)).findById(userId);
      verify(userMapper, never()).toDomain(any());
    }
  }

  @Nested
  @DisplayName("Testes para os métodos existsBy")
  class ExistByTests {

    @ParameterizedTest(name = "Quando o telefone {2}, deve retornar {1}")
    @CsvSource({"'5547912345678', true,  'existir'", "'5547931245678', false, 'não existir'"})
    @DisplayName("Deve retornar o resultado esperado para a existência do telefone")
    void existsByPhone_shouldReturnExpectedResult(
        String phone, boolean expectedResult, String description) {
      // Arrange
      when(userJpaRepository.existsByPhone(phone)).thenReturn(expectedResult);

      // Act
      boolean exists = userRepositoryAdapter.existsByPhone(phone);

      // Assert
      assertThat(exists).isEqualTo(expectedResult);
      verify(userJpaRepository, times(1)).existsByPhone(phone);
    }

    @ParameterizedTest(name = "Quando o username {2}, deve retornar {1}")
    @CsvSource({"'usernameTest', true, 'existir'", "'nonExistentUser', false, 'não existir'"})
    @DisplayName("Deve retornar o resultado esperado para a existência do username")
    void existsByUsername_shouldReturnExpectedResult(
        String username, boolean expectedResult, String description) {

      // Arrange
      when(userJpaRepository.existsByUsername(username)).thenReturn(expectedResult);

      // Act
      boolean exists = userRepositoryAdapter.existsByUsername(username);

      // Assert
      assertThat(exists).isEqualTo(expectedResult);
      verify(userJpaRepository, times(1)).existsByUsername(username);
    }
  }
}
