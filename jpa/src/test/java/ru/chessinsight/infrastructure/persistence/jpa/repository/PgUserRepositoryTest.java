package ru.chessinsight.infrastructure.persistence.jpa.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.persistence.jpa.hibernate.UserJpaRepository;
import ru.chessinsight.infrastructure.persistence.jpa.mapper.EntityMapper;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserEntity;
import ru.chessinsight.testdata.UserBuilder;
import ru.chessinsight.testdata.UserEntityBuilder;
import ru.chessinsight.testdata.UserEntityMother;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import io.qameta.allure.Tag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PgUserRepositoryTest {
    @Mock
    private EntityMapper<User, UserEntity> mapper;
    @Mock
    private UserJpaRepository jpaRepository;
    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private UserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PgUserRepository(mapper, jpaRepository);
    }

    @Test
    void findOneById_returnsMappedUser_whenFound() {
        UserEntity entity = UserEntityMother.fullUserEntity();
        UUID id = entity.getId();
        User user = UserBuilder.user().withId(id).build();
        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(user);

        Optional<User> result = repository.findOneById(id);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void findOneById_returnsEmpty_whenMissing() {
        UUID id = UUID.randomUUID();
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        Optional<User> result = repository.findOneById(id);

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void findOneByLogin_returnsMappedUser_whenFound() {
        String login = "alice";
        UserEntity entity = UserEntityMother.fullUserEntity();
        User user = UserBuilder.user().withId(entity.getId()).build();
        when(jpaRepository.findByLogin(login)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(user);

        Optional<User> result = repository.findOneByLogin(login);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void findOneByLogin_returnsEmpty_whenMissing() {
        when(jpaRepository.findByLogin("missing")).thenReturn(Optional.empty());

        Optional<User> result = repository.findOneByLogin("missing");

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void findOneByEmail_returnsMappedUser_whenFound() {
        String email = "alice@example.com";
        UserEntity entity = UserEntityMother.fullUserEntity();
        User user = UserBuilder.user().withId(entity.getId()).build();
        when(jpaRepository.findByEmail(email)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(user);

        Optional<User> result = repository.findOneByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void findOneByEmail_returnsEmpty_whenMissing() {
        when(jpaRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        Optional<User> result = repository.findOneByEmail("missing@example.com");

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void findAll_mapsAllEntities() {
        UserEntity entity = UserEntityMother.fullUserEntity();
        User user = UserBuilder.user().withId(entity.getId()).build();
        when(jpaRepository.findAll()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(user);

        List<User> result = repository.findAll();

        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    void findAll_returnsEmptyList_whenRepositoryEmpty() {
        when(jpaRepository.findAll()).thenReturn(List.of());

        List<User> result = repository.findAll();

        assertTrue(result.isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void findAll_withFilters_callsProperJpaMethod_andMaps() {
        PageParams params = new PageParams(0, 2);
        Set<Role> roles = EnumSet.of(Role.ROLE_USER);
        UserEntity entity = UserEntityBuilder.userEntity().withId(UUID.randomUUID()).build();
        User user = UserBuilder.user().withId(entity.getId()).build();
        when(jpaRepository.findDistinctByActiveAndRolesIn(eq(true), eq(roles), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 2), 1));
        when(mapper.toDomain(entity)).thenReturn(user);

        Page<User> result = repository.findAll(params, true, roles);

        assertEquals(1, result.content().size());
        assertEquals(1, result.totalElements());
        assertEquals(user, result.content().get(0));
        verify(jpaRepository).findDistinctByActiveAndRolesIn(eq(true), eq(roles), pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(2, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void findAll_withoutFilters_returnsEmptyPage_whenNoData() {
        PageParams params = new PageParams(1, 3);
        when(jpaRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 3), 0));

        Page<User> result = repository.findAll(params, null, null);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        verify(jpaRepository).findAll(pageableCaptor.capture());
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(3, pageableCaptor.getValue().getPageSize());
        verifyNoInteractions(mapper);
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        User user = UserBuilder.user().build();
        UserEntity entity = UserEntityMother.fullUserEntity();
        when(mapper.toEntity(user)).thenReturn(entity);
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(user);

        User result = repository.save(user);

        assertEquals(user, result);
        verify(jpaRepository).save(entity);
        verify(mapper).toEntity(user);
        verify(mapper).toDomain(entity);
    }

    @Test
    void save_throws_whenJpaRejectsEntity() {
        User user = UserBuilder.user().build();
        when(mapper.toEntity(user)).thenReturn(null);
        when(jpaRepository.save(null)).thenThrow(new IllegalArgumentException("entity must not be null"));

        assertThrows(IllegalArgumentException.class, () -> repository.save(user));
    }
}