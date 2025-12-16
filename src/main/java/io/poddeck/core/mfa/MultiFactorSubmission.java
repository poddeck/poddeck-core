package io.poddeck.core.mfa;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "member_multi_factor_submission")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(staticName = "create")
public final class MultiFactorSubmission {
  @Id
  @Column(name = "member", nullable = false, updatable = false)
  private UUID memberId;
  @Column(name = "secret", nullable = false)
  private String secret;
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
    name = "member_multi_factor_submission_recovery_codes",
    joinColumns = @JoinColumn(name = "member")
  )
  @Column(name = "recovery_code")
  private List<String> recoveryCodes;
  @Column(name = "confirmed", nullable = false)
  private boolean confirmed;

  public void confirm() {
    confirmed = true;
  }
}