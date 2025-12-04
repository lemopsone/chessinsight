package ru.chessinsight.infrastructure.persistence.jpa.repository;

import org.springframework.stereotype.Repository;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.domain.user.repository.UserRepository;
import ru.chessinsight.infrastructure.persistence.jpa.hibernate.UserJpaRepository;
import ru.chessinsight.infrastructure.persistence.jpa.mapper.EntityMapper;
import ru.chessinsight.infrastructure.persistence.jpa.model.UserEntity;

import java.util.Optional;
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
    public User save(User user) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(user)));
    }
}
