package io.poddeck.core.api.authentication;

import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.member.Member;
import io.poddeck.core.member.MemberRepository;
import io.poddeck.core.session.Session;
import io.poddeck.core.session.SessionRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class AuthenticationRefreshController extends AuthenticationController {
  private final Key refreshKey;
  private final Authentication authentication;
  private final SessionRepository sessionRepository;

  private AuthenticationRefreshController(
    @Qualifier("authenticationKey") Key authenticationKey,
    @Qualifier("refreshKey") Key refreshKey,
    MemberRepository memberRepository, Authentication authentication,
    SessionRepository sessionRepository
  ) {
    super(authenticationKey, memberRepository);
    this.refreshKey = refreshKey;
    this.authentication = authentication;
    this.sessionRepository = sessionRepository;
  }

  @RequestMapping(path = "/authentication/refresh/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> refreshVerification(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var refreshToken = body.getString("refresh_token");
    var result = verifyToken(refreshKey, refreshToken);
    if (result.getKey() != HttpServletResponse.SC_ACCEPTED) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    var memberId = UUID.fromString(result.getValue().get("id", String.class));
    var sessionId = UUID.fromString(result.getValue().get("session", String.class));
    return memberRepository().existsById(memberId)
      .thenCompose(memberExists -> sessionRepository.existsById(sessionId)
        .thenCompose(sessionExists -> refreshVerification(refreshToken, memberId,
          sessionId, memberExists, sessionExists)));
  }

  private CompletableFuture<Map<String, Object>> refreshVerification(
    String refreshToken, UUID memberId, UUID sessionId, boolean memberExists,
    boolean sessionExists
  ) {
    if (!memberExists || !sessionExists) {
      return CompletableFuture.completedFuture(Map.of("success", false));
    }
    return memberRepository().findById(memberId)
      .thenCompose(member -> sessionRepository.findById(sessionId)
        .thenApply(session -> refreshVerification(refreshToken, member.get(),
          session.get())));
  }

  private Map<String, Object> refreshVerification(
    String refreshToken, Member member, Session session
  ) {
    if (session.status().isClosed() ||
      !session.lastRefreshToken().equals(refreshToken)
    ) {
      return Map.of("success", false);
    }
    var newAuthenticationToken =
      authentication.generateAuthenticationToken(member.id(), session.id());
    var newRefreshToken =
      authentication.generateRefreshToken(member.id(), session.id());
    session.updateRefreshToken(newRefreshToken);
    sessionRepository.save(session);
    return Map.of("success", true, "authentication_token", newAuthenticationToken,
      "refresh_token", newRefreshToken);
  }
}
