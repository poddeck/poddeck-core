package io.poddeck.core.session;

import io.poddeck.core.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface SessionRepository extends DatabaseRepository<Session, UUID> {
  @Async
  CompletableFuture<List<Session>> findByMemberIdAndStatus(
    UUID memberId, SessionStatus status);
}