package io.poddeck.core.member;

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
@Table(name = "member_password_reset")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "create")
public final class MemberPasswordReset {
  @Id
  @Column(name = "member", nullable = false, updatable = false)
  private UUID memberId;
  @Column(name = "reset_token", nullable = false)
  private String resetToken;
}