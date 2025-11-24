package io.poddeck.core.mfa;

import com.google.common.collect.Lists;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import io.poddeck.core.member.Member;
import io.poddeck.core.member.MemberRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor(staticName = "create")
public final class MultiFactorAuth {
  private final MultiFactorSubmissionRepository memberMultiFactorSubmissionRepository;
  private final MemberRepository memberRepository;
  private final UUID memberId;

  public CompletableFuture<MultiFactorSubmission> setup() {
    return memberMultiFactorSubmissionRepository.save(
      MultiFactorSubmission.create(memberId, generateSecret(),
        generateRecoveryCodes(), false));
  }

  private String generateSecret() {
    var secretGenerator = new DefaultSecretGenerator();
    return secretGenerator.generate();
  }

  private List<String> generateRecoveryCodes() {
    var recoveryCodes = new RecoveryCodeGenerator();
    return Lists.newArrayList(recoveryCodes.generateCodes(16));
  }

  public CompletableFuture<byte[]> generateQRCode() {
    return memberRepository.findById(memberId)
      .thenCompose(member -> memberMultiFactorSubmissionRepository.findById(memberId)
        .thenApplyAsync(auth -> buildQRCode(member.get(), auth.get().secret())));
  }

  private byte[] buildQRCode(Member member, String secret) {
    var data = new QrData.Builder()
      .label(member.email())
      .secret(secret)
      .issuer("PodDeck")
      .algorithm(HashingAlgorithm.SHA256)
      .digits(6)
      .period(30)
      .build();
    var generator = new ZxingPngQrGenerator();
    try {
      return generator.generate(data);
    } catch (Exception exception) {
      return new byte[0];
    }
  }

  public CompletableFuture<Boolean> verifyCode(String code) {
    return memberMultiFactorSubmissionRepository.existsById(memberId)
      .thenCompose(exists -> verifyCode(code, exists));
  }

  private CompletableFuture<Boolean> verifyCode(
    String code, boolean multiFactorAuthEnabled
  ) {
    if (!multiFactorAuthEnabled) {
      return CompletableFuture.completedFuture(true);
    }
    if (code.contains("-")) {
      return verifyRecoveryCode(code);
    }
    var timeProvider = new SystemTimeProvider();
    var codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA256);
    var verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    return memberMultiFactorSubmissionRepository.findById(memberId)
      .thenApplyAsync(auth -> verifier.isValidCode(auth.get().secret(), code));
  }

  public CompletableFuture<Boolean> verifyRecoveryCode(String recoveryCode) {
    return memberMultiFactorSubmissionRepository.findById(memberId)
      .thenApply(auth -> auth.get().recoveryCodes().contains(recoveryCode));
  }
}