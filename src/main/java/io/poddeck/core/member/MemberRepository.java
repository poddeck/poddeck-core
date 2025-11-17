package io.poddeck.core.member;

import io.poddeck.core.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface MemberRepository extends DatabaseRepository<Member, UUID> {
  @Async
  CompletableFuture<Optional<Member>> findByEmail(String email);

  @Async
  CompletableFuture<Boolean> existsByEmail(String email);
}