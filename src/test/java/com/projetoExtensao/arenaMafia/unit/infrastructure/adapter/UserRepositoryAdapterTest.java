package com.projetoExtensao.arenaMafia.unit.infrastructure.adapter;

import static com.projetoExtensao.arenaMafia.unit.config.TestDataProvider.defaultPhone;
import static com.projetoExtensao.arenaMafia.unit.config.TestDataProvider.defaultUsername;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.repository.UserRepositoryAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.UserMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
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

  @Test
  @DisplayName("Deve mapear corretamente um User para userEntity, salvar e retornar um User")
  void save_shouldMapperUserAndSave() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UserEntity userEntity = new UserEntity();

    when(userMapper.toEntity(user)).thenReturn(userEntity);
    when(userJpaRepository.save(userEntity)).thenReturn(userEntity);
    when(userMapper.toDomain(userEntity)).thenReturn(user);

    // Act
    User result = userRepositoryAdapter.save(user);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(user.getId());
    assertThat(result.getUsername()).isEqualTo(user.getUsername());
    assertThat(result.getFullName()).isEqualTo(user.getFullName());
    assertThat(result.getPhone()).isEqualTo(user.getPhone());
    assertThat(result.getPasswordHash()).isEqualTo(user.getPasswordHash());
  }

  @Nested
  @DisplayName("Testes para o método findByUsername")
  class FindByUsernameTests {

    @Test
    @DisplayName("Deve encontrar um userEntity pelo username e retornar um User mapeado")
    void findByUsername_shouldReturnMappedUser() {
      // Arrange
      User user = TestDataProvider.createActiveUser();
      UserEntity userEntity = new UserEntity();

      when(userJpaRepository.findByUsername(defaultUsername)).thenReturn(Optional.of(userEntity));
      when(userMapper.toDomain(userEntity)).thenReturn(user);

      // Act
      Optional<User> result = userRepositoryAdapter.findByUsername(defaultUsername);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isPresent();
      assertThat(result.get().getUsername()).isEqualTo(defaultUsername);

      verify(userJpaRepository, times(1)).findByUsername(defaultUsername);
      verify(userMapper, times(1)).toDomain(userEntity);
    }

    @Test
    @DisplayName("Deve retornar um optional vazio quando não encontrar um User pelo username")
    void findByUsername_shouldReturnEmptyOptionalWhenNotFound() {
      // Arrange
      when(userJpaRepository.findByUsername(defaultUsername)).thenReturn(Optional.empty());

      // Act
      Optional<User> result = userRepositoryAdapter.findByUsername(defaultUsername);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();

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
      UserEntity userEntity = new UserEntity();
      User userMapped = TestDataProvider.createActiveUser();

      when(userJpaRepository.findByPhone(defaultPhone)).thenReturn(Optional.of(userEntity));
      when(userMapper.toDomain(userEntity)).thenReturn(userMapped);

      // Act
      Optional<User> result = userRepositoryAdapter.findByPhone(defaultPhone);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isPresent();
      assertThat(result.get().getPhone()).isEqualTo(defaultPhone);

      verify(userMapper, times(1)).toDomain(userEntity);
    }

    @Test
    @DisplayName("Deve retornar um optional vazio quando não encontrar um User pelo telefone")
    void findByPhone_shouldReturnEmptyOptionalWhenNotFound() {
      // Arrange
      when(userJpaRepository.findByPhone(defaultPhone)).thenReturn(Optional.empty());

      // Act
      Optional<User> result = userRepositoryAdapter.findByPhone(defaultPhone);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();

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
      User user = TestDataProvider.createActiveUser();
      UUID userId = user.getId();
      UserEntity userEntity = new UserEntity();

      when(userJpaRepository.findById(userId)).thenReturn(Optional.of(userEntity));
      when(userMapper.toDomain(userEntity)).thenReturn(user);

      // Act
      Optional<User> result = userRepositoryAdapter.findById(userId);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(userId);

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
      Optional<User> result = userRepositoryAdapter.findById(userId);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();

      verify(userJpaRepository, times(1)).findById(userId);
      verify(userMapper, never()).toDomain(any());
    }
  }

  @ParameterizedTest(name = "Quando o username existe = {0}, deve retornar {0}")
  @ValueSource(booleans = {true, false})
  @DisplayName("Deve retornar o resultado esperado para a existência do username")
  void existsByUsername_shouldReturnExpectedBoolean(boolean usernameExists) {
    // Arrange
    when(userJpaRepository.existsByUsername(defaultUsername)).thenReturn(usernameExists);

    // Act
    boolean actualResult = userRepositoryAdapter.existsByUsername(defaultUsername);

    // Assert
    assertThat(actualResult).isEqualTo(usernameExists);
    verify(userJpaRepository, times(1)).existsByUsername(defaultUsername);
  }

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
