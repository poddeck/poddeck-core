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
@Table(name = "member_email_change")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "create")
public class MemberEmailChange {
  @Id
  @Column(name = "member", nullable = false, unique = true)
  private UUID member;
  @Column(name = "new_email", nullable = false)
  private String newEmail;
  @Column(name = "change_token", nullable = false)
  private String changeToken;
}
