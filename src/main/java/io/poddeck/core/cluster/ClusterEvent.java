package io.poddeck.core.cluster;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.UUID;

@Entity
@Table(name = "cluster_event")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "create")
public final class ClusterEvent {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "cluster", nullable = false)
  private UUID cluster;
  @Column(name = "name", nullable = false)
  private String name;
  @Column(name = "namespace", nullable = false)
  private String namespace;
  @Column(name = "reason", nullable = false)
  private String reason;
  @Column(name = "message", nullable = false)
  private String message;
  @Column(name = "type", nullable = false)
  private String type;
  @Column(name = "involved_kind", nullable = false)
  private String involvedKind;
  @Column(name = "involved_name", nullable = false)
  private String involvedName;
  @Column(name = "first_timestamp", nullable = false)
  private long firstTimestamp;
  @Column(name = "last_timestamp", nullable = false)
  private long lastTimestamp;
  @Column(name = "count", nullable = false)
  private int count;
}
