package io.poddeck.core.notification;

import io.poddeck.common.iterator.AsyncIterator;
import io.poddeck.core.member.MemberRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationDispatch {
  private final NotificationRepository notificationRepository;
  private final MemberRepository memberRepository;

  public CompletableFuture<Void> dispatch(
    UUID clusterId, String title, String description, NotificationType type
  ) {
    return memberRepository.findAll()
      .thenCompose(members -> AsyncIterator.execute(members,
          member -> dispatch(member.id(), clusterId, title, description, type))
        .thenApply(_ -> null));
  }

  public CompletableFuture<Void> dispatch(
    UUID memberId, UUID clusterId, String title, String description,
    NotificationType type
  ) {
    return notificationRepository.generateAvailableId(UUID::randomUUID)
      .thenCompose(id -> dispatchToMember(id, memberId, clusterId, title,
        description, type));
  }

  private CompletableFuture<Void> dispatchToMember(
    UUID notificationId, UUID memberId, UUID clusterId, String title,
    String description, NotificationType type
  ) {
    var notification = Notification.create(notificationId, memberId, clusterId,
      title, description, type, NotificationState.UNSEEN,
      System.currentTimeMillis());
    return notificationRepository.save(notification).thenApply(_ -> null);
  }
}
