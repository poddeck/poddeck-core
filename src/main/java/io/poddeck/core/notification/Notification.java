package io.poddeck.core.notification;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notification")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class Notification {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "member", nullable = false)
  private UUID member;
  @Column(name = "cluster", nullable = false)
  private UUID cluster;
  @Column(name = "title", nullable = false)
  private String title;
  @Column(name = "description", nullable = false)
  private String description;
  @ElementCollection
  @CollectionTable(
    name = "notification_parameters",
    joinColumns = @JoinColumn(name = "id")
  )
  @Column(name = "parameters")
  private List<String> parameters;
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private NotificationType type;
  @Enumerated(EnumType.STRING)
  @Column(name = "state", nullable = false)
  private NotificationState state;
  @Column(name = "created_at", nullable = false)
  private long createdAt;

  public void updateState(NotificationState state) {
    this.state = state;
  }
}