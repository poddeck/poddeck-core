package io.poddeck.core.api.authentication;

import com.maxmind.geoip2.DatabaseReader;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelRestController;
import io.poddeck.core.member.Member;
import io.poddeck.core.member.MemberRepository;
import io.poddeck.core.mfa.MultiFactorAuthFactory;
import io.poddeck.core.session.Session;
import io.poddeck.core.session.SessionRepository;
import io.poddeck.core.session.SessionStatus;
import io.poddeck.core.session.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.security.Key;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class AuthenticationLoginController extends PanelRestController {
  private final Authentication authentication;
  private final MultiFactorAuthFactory multiFactorAuthFactory;
  private final SessionRepository sessionRepository;
  private final DatabaseReader geoDatabaseReader;

  private AuthenticationLoginController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, Authentication authentication,
    MultiFactorAuthFactory multiFactorAuthFactory,
    SessionRepository sessionRepository, DatabaseReader geoDatabaseReader
  ) {
    super(authenticationKey, memberRepository);
    this.authentication = authentication;
    this.multiFactorAuthFactory = multiFactorAuthFactory;
    this.sessionRepository = sessionRepository;
    this.geoDatabaseReader = geoDatabaseReader;
  }

  @RequestMapping(path = "/authentication/login/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> login(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    return login(request, body.getString("email").trim(),
      body.getString("password"), body.getString("multi_factor_code"));
  }

  private CompletableFuture<Map<String, Object>> login(
    HttpServletRequest request, String email, String password,
    String multiFactorCode
  ) {
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    authentication.isAuthenticated(email, password)
      .thenAccept(isAuthenticated -> checkAuthorization(request, email,
        multiFactorCode, futureResponse, isAuthenticated));
    return futureResponse;
  }

  private void checkAuthorization(
    HttpServletRequest request, String email, String multiFactorCode,
    CompletableFuture<Map<String, Object>> futureResponse, boolean isAuthenticated
  ) {
    if (!isAuthenticated) {
      futureResponse.complete(Map.of("success", false, "error", 1000));
      return;
    }
    memberRepository().findByEmail(email)
      .thenAccept(member -> multiFactorAuthFactory.createAuth(member.get().id())
        .verifyCode(multiFactorCode)
        .thenAccept(verified -> checkMultiFactorAuth(request, member.get(),
          futureResponse, verified)));
  }

  private void checkMultiFactorAuth(
    HttpServletRequest request, Member member,
    CompletableFuture<Map<String, Object>> futureResponse,
    boolean multiFactorVerified
  ) {
    if (!multiFactorVerified) {
      futureResponse.complete(Map.of("success", false, "error", 1001));
      return;
    }
    processAuthorizedLogin(request, member, futureResponse);
  }

  public void processAuthorizedLogin(
    HttpServletRequest request, Member member,
    CompletableFuture<Map<String, Object>> futureResponse
  ) {
    sessionRepository.generateAvailableId(UUID::randomUUID)
      .thenAccept(sessionId -> completeLogin(request,
        futureResponse, member, sessionId));
  }

  private void completeLogin(
    HttpServletRequest request,
    CompletableFuture<Map<String, Object>> futureResponse,
    Member member, UUID sessionId
  ) {
    var authenticationToken =
      authentication.generateAuthenticationToken(member.id(), sessionId);
    var refreshToken =
      authentication.generateRefreshToken(member.id(), sessionId);
    storeSession(request, member.id(), sessionId, refreshToken);
    futureResponse.complete(Map.of("success", true,
      "authentication_token", authenticationToken, "refresh_token", refreshToken,
      "email", member.email(), "name", member.name()));
  }

  public void storeSession(
    HttpServletRequest request, UUID memberId, UUID sessionId, String refreshToken
  ) {
    var country = "";
    var city = "";
    var platform = "";
    var ipAddress = request.getHeader("X-Real-IP");
    try {
      var location = geoDatabaseReader.city(InetAddress.getByName(ipAddress));
      country = location.country().name();
      city = location.city().name();
    } catch (Exception ignored) {
    }
    try {
      platform = UserAgent.create(request.getHeader("User-Agent"))
        .findPlatform();
    } catch (Exception ignored) {
    }
    var session = Session.create(sessionId, memberId, SessionStatus.ACTIVE,
      platform, ipAddress, country, city, System.currentTimeMillis(),
      refreshToken, System.currentTimeMillis());
    sessionRepository.save(session);
  }
}
