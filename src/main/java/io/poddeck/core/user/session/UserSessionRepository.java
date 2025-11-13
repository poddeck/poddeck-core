package io.poddeck.core.user.session;

import io.poddeck.core.database.DatabaseRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserSessionRepository extends DatabaseRepository<UserSession, UUID> {
}