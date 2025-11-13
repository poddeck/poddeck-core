package io.poddeck.core.mfa;

import io.poddeck.core.member.MemberRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class MultiFactorAuthFactory {
  private final MultiFactorSubmissionRepository memberMultiFactorSubmissionRepository;
  private final MemberRepository memberRepository;

  public MultiFactorAuth createAuth(UUID memberId) {
    return MultiFactorAuth.create(memberMultiFactorSubmissionRepository,
      memberRepository, memberId);
  }
}