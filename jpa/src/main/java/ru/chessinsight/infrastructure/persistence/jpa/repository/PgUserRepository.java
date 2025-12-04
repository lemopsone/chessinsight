package ru.chessinsight.infrastructure.persistence.jpa.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.chessinsight.domain.common.pagination.Page;
import ru.chessinsight.domain.common.pagination.PageParams;
import ru.chessinsight.domain.user.model.Role;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.persistence.jpa.hibernate.UserJpaRepository;
import ru.chessinsight.infrastructure.persistence.jpa.mapper.EntityMapper;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class PgUserRepository implements UserRepository {
    private final EntityMapper<User, UserEntity> mapper;
    private final UserJpaRepository jpaRepository;

    public PgUserRepository(EntityMapper<User, UserEntity> mapper, UserJpaRepository jpaRepository) {
        this.mapper = mapper;
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findOneById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findOneByLogin(String login) {
        return jpaRepository.findByLogin(login).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findOneByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    public Page<User> findAll(PageParams params,
                              Boolean active,
                              Set<Role> roles) {

        Pageable pageable = PageRequest.of(params.page(), params.size());

        boolean filterActive = active != null;
        boolean filterRoles = roles != null && !roles.isEmpty();

        org.springframework.data.domain.Page<UserEntity> springPage;
        if (!filterActive && !filterRoles) {
            springPage = jpaRepository.findAll(pageable);
        } else if (filterActive && !filterRoles) {
            springPage = jpaRepository.findAllByActive(active, pageable);
        } else if (!filterActive){
            springPage = jpaRepository.findDistinctByRolesIn(roles, pageable);
        } else {
            springPage = jpaRepository.findDistinctByActiveAndRolesIn(active, roles, pageable);
        }

        List<User> content = springPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new Page<>(
                content,
                params.page(),
                params.size(),
                springPage.getTotalElements()
        );
    }
    @Override
    public User save(User user) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(user)));
    }
}
