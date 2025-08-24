package com.projetoExtensao.arenaMafia.unit.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.repository.RefreshTokenRepositoryAdapter;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.RefreshTokenEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.RefreshTokenMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.UserMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para RefreshTokenRepositoryAdapter")
public class RefreshTokenRepositoryAdapterTest {

  @Mock private RefreshTokenJpaRepository refreshTokenJpaRepository;
  @Mock private RefreshTokenMapper refreshTokenMapper;
  @Mock private UserMapper userMapper;

  @InjectMocks private RefreshTokenRepositoryAdapter refreshTokenRepositoryAdapter;

  @Nested
  @DisplayName("Testes para o método save")
  class SaveTests {
    @Test
    @DisplayName("Deve salvar e retornar o RefreshToken mapeado")
    void save_shouldSaveAndReturnMappedRefreshToken() {
      // Arrange
      RefreshToken refreshTokenToSave = RefreshToken.create(2L, mock(User.class));
      RefreshTokenEntity refreshTokenEntityMapped = mock(RefreshTokenEntity.class);
      RefreshTokenEntity refreshTokenEntitySaved = mock(RefreshTokenEntity.class);
      RefreshToken refreshTokenReturned = refreshTokenToSave;

      when(refreshTokenMapper.toEntity(refreshTokenToSave)).thenReturn(refreshTokenEntityMapped);
      when(refreshTokenJpaRepository.save(refreshTokenEntityMapped))
          .thenReturn(refreshTokenEntitySaved);
      when(refreshTokenMapper.toDomain(refreshTokenEntitySaved)).thenReturn(refreshTokenReturned);

      // Act
      RefreshToken result = refreshTokenRepositoryAdapter.save(refreshTokenToSave);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.getToken()).isEqualTo(refreshTokenToSave.getToken());
      assertThat(result.getUser()).isEqualTo(refreshTokenToSave.getUser());

      verify(refreshTokenMapper).toEntity(refreshTokenToSave);
      verify(refreshTokenJpaRepository).save(refreshTokenEntityMapped);
      verify(refreshTokenMapper).toDomain(refreshTokenEntitySaved);
    }
  }

  @Nested
  @DisplayName("Testes para o método findByToken")
  class findByTokenTests {

    @Test
    @DisplayName("Deve retornar um Optional contendo o RefreshToken quando encontrado")
    void findByToken_shouldReturnOptionalRefreshTokenWhenFound() {
      // Arrange
      RefreshToken refreshToken = RefreshToken.create(2L, mock(User.class));
      RefreshTokenEntity refreshTokenEntity = mock(RefreshTokenEntity.class);

      when(refreshTokenMapper.toDomain(refreshTokenEntity)).thenReturn(refreshToken);
      when(refreshTokenJpaRepository.findByToken(refreshToken.getToken().toString()))
          .thenReturn(Optional.of(refreshTokenEntity));

      // Act
      Optional<RefreshToken> result =
          refreshTokenRepositoryAdapter.findByToken(refreshToken.getToken());

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get()).isEqualTo(refreshToken);
      assertThat(result.get().getToken()).isEqualTo(refreshToken.getToken());
    }

    @Test
    @DisplayName("Deve retornar um Optional vazio quando o RefreshToken não for encontrado")
    void findByToken_shouldReturnEmptyOptionalWhenNotFound() {
      // Arrange
      RefreshToken refreshToken = RefreshToken.create(2L, mock(User.class));

      when(refreshTokenJpaRepository.findByToken(refreshToken.getToken().toString()))
          .thenReturn(Optional.empty());

      // Act
      Optional<RefreshToken> result =
          refreshTokenRepositoryAdapter.findByToken(refreshToken.getToken());

      // Assert
      assertThat(result).isNotPresent();
    }
  }

  @Nested
  @DisplayName("Testes para o método deleteByUser")
  class DeleteByUserTests {

    @Test
    @DisplayName("Deve deletar o RefreshToken associado ao User")
    void deleteByUser_shouldDeleteRefreshTokenByUser() {
      // Arrange
      User user = mock(User.class);
      UserEntity userEntity = mock(UserEntity.class);

      when(userMapper.toEntity(user)).thenReturn(userEntity);
      doNothing().when(refreshTokenJpaRepository).deleteByUser(userEntity);
      doNothing().when(refreshTokenJpaRepository).flush();

      // Act
      refreshTokenRepositoryAdapter.deleteByUser(user);

      // Assert
      verify(refreshTokenJpaRepository, times(1)).deleteByUser(any());
      verify(refreshTokenJpaRepository, times(1)).flush();
    }
  }

  @Nested
  @DisplayName("Testes para o método delete")
  class DeleteTests {
    @Test
    @DisplayName("Deve deletar o RefreshToken")
    void delete_shouldDeleteRefreshToken() {
      // Arrange
      RefreshToken refreshToken = RefreshToken.create(2L, mock(User.class));
      RefreshTokenEntity refreshTokenEntity = mock(RefreshTokenEntity.class);

      when(refreshTokenMapper.toEntity(refreshToken)).thenReturn(refreshTokenEntity);
      doNothing().when(refreshTokenJpaRepository).delete(refreshTokenEntity);

      // Act
      refreshTokenRepositoryAdapter.delete(refreshToken);

      // Assert
      verify(refreshTokenJpaRepository, times(1)).delete(refreshTokenEntity);
    }
  }
}
