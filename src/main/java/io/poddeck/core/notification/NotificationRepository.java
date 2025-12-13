package io.poddeck.core.notification;

import io.poddeck.core.database.DatabaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Repository
public interface NotificationRepository extends DatabaseRepository<Notification, UUID> {
  @Async
  CompletableFuture<List<Notification>> findAllByMember(UUID member);

  @Async
  CompletableFuture<List<Notification>> findAllByMemberAndCluster(
    UUID member, UUID cluster);
}