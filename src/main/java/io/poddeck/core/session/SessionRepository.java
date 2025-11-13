package io.poddeck.core.session;

import io.poddeck.core.database.DatabaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SessionRepository extends DatabaseRepository<Session, UUID> {
}