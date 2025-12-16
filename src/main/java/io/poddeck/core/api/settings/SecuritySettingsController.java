package io.poddeck.core.api.settings;

import com.google.common.collect.Maps;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.api.security.panel.PanelRestController;
import io.poddeck.core.hashing.Hashing;
import io.poddeck.core.member.Member;
import io.poddeck.core.member.MemberRepository;
import io.poddeck.core.mfa.MultiFactorAuthFactory;
import io.poddeck.core.mfa.MultiFactorSubmission;
import io.poddeck.core.mfa.MultiFactorSubmissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class SecuritySettingsController extends PanelRestController {
  private final MultiFactorSubmissionRepository submissionRepository;
  private final MultiFactorAuthFactory authFactory;
  private final Hashing hashing;

  private SecuritySettingsController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository,
    MultiFactorSubmissionRepository submissionRepository,
    MultiFactorAuthFactory authFactory, Hashing hashing
  ) {
    super(authenticationKey, memberRepository);
    this.submissionRepository = submissionRepository;
    this.authFactory = authFactory;
    this.hashing = hashing;
  }

  @PanelEndpoint
  @RequestMapping(path = "/settings/2fa/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> multiFactorAuth(
    HttpServletRequest request
  ) {
    return submissionRepository.existsById(findMemberId(request))
      .thenApply(exists -> Map.of("enabled", exists));
  }

  @PanelEndpoint
  @RequestMapping(path = "/settings/2fa/switch/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> switchMultiFactorAuth(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    return findMember(request).thenCompose(member ->
      switchMultiFactorAuth(member, body.getString("password")));
  }

  private CompletableFuture<Map<String, Object>> switchMultiFactorAuth(
    Member member, String password
  ) {
    if (!hashing.matches(password, member.passwordHash())) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    return submissionRepository.existsById(member.id())
      .thenCompose(exists -> switchMultiFactorAuth(member.id(), exists));
  }

  private CompletableFuture<Map<String, Object>> switchMultiFactorAuth(
    UUID memberId, boolean multiFactorAuthEnabled
  ) {
    if (multiFactorAuthEnabled) {
      submissionRepository.deleteById(memberId);
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var auth = authFactory.createAuth(memberId);
    return auth.setup().thenCompose(value -> auth.generateQRCode()
      .thenApply(Base64.getEncoder()::encodeToString)
      .thenCompose(qrCodeEncoded -> submissionRepository.findById(memberId)
        .thenApply(Optional::get)
        .thenApply(authMember -> Map.of("qrCode", qrCodeEncoded, "secret",
          authMember.secret(), "recoveryCodes", authMember.recoveryCodes()))));
  }

  @PanelEndpoint
  @RequestMapping(path = "/settings/2fa/confirm/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> confirmMultiFactorAuth(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var memberId = findMemberId(request);
    return submissionRepository.existsById(memberId)
      .thenCompose(exists -> confirmMultiFactorAuth(memberId,
        body.getString("code"), exists));
  }

  private CompletableFuture<Map<String, Object>> confirmMultiFactorAuth(
    UUID memberId, String code, boolean multiFactorEnabled
  ) {
    if (!multiFactorEnabled) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    return submissionRepository.findById(memberId)
      .thenCompose(auth -> confirmMultiFactorAuth(memberId, code, auth.get()));
  }

  private CompletableFuture<Map<String, Object>> confirmMultiFactorAuth(
    UUID memberId, String code, MultiFactorSubmission submission
  ) {
    if (submission.confirmed()) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    return authFactory.createAuth(memberId).verifyCode(code)
      .thenCompose(verified -> !verified ?
        CompletableFuture.completedFuture(Map.of("success", false)) :
        confirmMultiFactorAuth(submission));
  }

  private CompletableFuture<Map<String, Object>> confirmMultiFactorAuth(
    MultiFactorSubmission submission
  ) {
    submission.confirm();
    return submissionRepository.save(submission)
      .thenApply(_ -> Map.of("success", true));
  }

  @PanelEndpoint
  @RequestMapping(path = "/settings/2fa/isConfirmed/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> isMultiFactorAuthConfirmed(
    HttpServletRequest request
  ) {
    var memberId = findMemberId(request);
    return submissionRepository.existsById(memberId)
      .thenCompose(exists -> isMultiFactorAuthConfirmed(memberId, exists));
  }

  private CompletableFuture<Map<String, Object>> isMultiFactorAuthConfirmed(
    UUID memberId, boolean multiFactorEnabled
  ) {
    if (!multiFactorEnabled) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    return submissionRepository.findById(memberId)
      .thenApply(auth -> Map.of("success", true, "confirmed", auth.get().confirmed()));
  }
}
