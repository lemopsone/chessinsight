package ru.chessinsight.fixtures;

import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryUserRepository implements UserRepository {
    private final Map<UUID, User> store = new HashMap<>();

    @Override
    public Optional<User> findOneById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<User> findOneByLogin(String login) {
        return store.values().stream()
                .filter(u -> Objects.equals(login, u.getLogin()))
                .findFirst();
    }

    @Override
    public Optional<User> findOneByEmail(String email) {
        return store.values().stream()
                .filter(u -> Objects.equals(email, u.getEmail()))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Page<User> findAll(PageParams params, Boolean active, Set<Role> roles) {
        List<User> filtered = store.values().stream()
                .filter(u -> active == null || u.isActive() == active)
                .filter(u -> roles == null || roles.isEmpty() || !Collections.disjoint(u.getRoles(), roles))
                .collect(Collectors.toList());

        int from = params.page() * params.size();
        int to = Math.min(from + params.size(), filtered.size());
        List<User> pageContent = from >= filtered.size() ? List.of() : filtered.subList(from, to);

        return new Page<>(pageContent, params.page(), params.size(), filtered.size());
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        store.put(user.getId(), user);
        return user;
    }
}
