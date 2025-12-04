package ru.chessinsight.infrastructure.persistence.jpa.mapper;

public interface EntityMapper<Domain, JPA> {
    Domain toDomain(JPA e);
    JPA toEntity(Domain d);
}
