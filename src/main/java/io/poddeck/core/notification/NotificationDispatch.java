package io.poddeck.core.notification;

import com.beust.jcommander.internal.Lists;
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
    UUID clusterId, NotificationType type, String title, String description,
    String... parameters
  ) {
    return memberRepository.findAll()
      .thenCompose(members -> AsyncIterator.execute(members,
          member -> dispatch(member.id(), clusterId, type,
            title, description, parameters))
        .thenApply(_ -> null));
  }

  public CompletableFuture<Void> dispatch(
    UUID memberId, UUID clusterId, NotificationType type, String title,
    String description, String... parameters
  ) {
    return notificationRepository.generateAvailableId(UUID::randomUUID)
      .thenCompose(id -> dispatchToMember(id, memberId, clusterId, type, title,
        description, parameters));
  }

  private CompletableFuture<Void> dispatchToMember(
    UUID notificationId, UUID memberId, UUID clusterId, NotificationType type,
    String title, String description, String... parameters
  ) {
    var notification = Notification.create(notificationId, memberId, clusterId,
      title, description, Lists.newArrayList(parameters), type,
      NotificationState.UNSEEN, System.currentTimeMillis());
    return notificationRepository.save(notification).thenApply(_ -> null);
  }
}
