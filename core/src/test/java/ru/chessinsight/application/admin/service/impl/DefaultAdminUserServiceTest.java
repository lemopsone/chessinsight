package ru.chessinsight.application.admin.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.chessinsight.application.auth.dto.SignUpDTO;
import ru.chessinsight.application.auth.exception.UserExistsException;
import ru.chessinsight.application.auth.service.AuthService;
import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.statistics.exception.UserNotFoundException;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.testdata.UserBuilder;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import ru.chessinsight.testutil.Tag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DefaultAdminUserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;
    @Mock
    private Logger logger;
    @Captor
    private ArgumentCaptor<User> userCaptor;

    private DefaultAdminUserService service;

    @BeforeEach
    void setUp() {
        service = new DefaultAdminUserService(userRepository, authService, logger);
    }

    @Test
    void listUsers_returnsPage() {
        PageParams params = new PageParams(0, 10);
        Page<User> page = new Page<>(java.util.List.of(), 0, 10, 0);
        when(userRepository.findAll(params, true, EnumSet.of(Role.ROLE_USER))).thenReturn(page);

        Page<User> result = service.listUsers(true, EnumSet.of(Role.ROLE_USER), params);

        assertEquals(page, result);
    }

    @Test
    void listUsers_returnsEmptyPage_whenRepositoryEmpty() {
        PageParams params = new PageParams(1, 5);
        Page<User> page = new Page<>(java.util.List.of(), 1, 5, 0);
        when(userRepository.findAll(params, null, null)).thenReturn(page);

        Page<User> result = service.listUsers(null, null, params);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    @Test
    void getUser_returnsUser_whenFound() {
        UUID id = UUID.randomUUID();
        User user = UserBuilder.user().withId(id).build();
        when(userRepository.findOneById(id)).thenReturn(Optional.of(user));

        User result = service.getUser(id);

        assertEquals(user, result);
    }

    @Test
    void getUser_throws_whenMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findOneById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getUser(id));
    }

    @Test
    void createUser_assignsRolesAndActive_whenProvided() {
        SignUpDTO dto = new SignUpDTO("alice", "alice@example.com", "pass");
        User user = UserBuilder.user().withLogin("alice").withActive(true).build();
        when(userRepository.findOneByLogin("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.createUser(dto, EnumSet.of(Role.ROLE_ADMIN), false);

        assertFalse(result.isActive());
        assertTrue(result.getRoles().contains(Role.ROLE_ADMIN));
        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isActive());
    }

    @Test
    void createUser_throws_whenUserExists() {
        SignUpDTO dto = new SignUpDTO("alice", "alice@example.com", "pass");
        doThrow(new UserExistsException("exists")).when(authService).signUp(dto);

        assertThrows(UserExistsException.class, () -> service.createUser(dto, Set.of(Role.ROLE_USER), true));
        verify(userRepository, never()).save(any());
    }

    @Test
    void patchUser_updatesProvidedFields() {
        UUID id = UUID.randomUUID();
        User user = UserBuilder.user().withId(id).withLogin("old").withEmail("old@example.com").build();
        when(userRepository.findOneById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.patchUser(id, "new", "new@example.com", EnumSet.of(Role.ROLE_ADMIN), false);

        assertEquals("new", result.getLogin());
        assertEquals("new@example.com", result.getEmail());
        assertFalse(result.isActive());
        assertTrue(result.getRoles().contains(Role.ROLE_ADMIN));
    }

    @Test
    void patchUser_throws_whenMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findOneById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.patchUser(id, "x", "y", null, null));
    }

    @Test
    void deactivateUser_setsInactive() {
        UUID id = UUID.randomUUID();
        User user = UserBuilder.user().withId(id).withActive(true).build();
        when(userRepository.findOneById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deactivateUser(id);

        assertFalse(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_throws_whenMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findOneById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.deactivateUser(id));
    }
}