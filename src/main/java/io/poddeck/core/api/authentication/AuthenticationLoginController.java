package io.poddeck.core.api.authentication;

import com.maxmind.geoip2.DatabaseReader;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
public final class AuthenticationLoginController extends PanelRestController {
  private final Key refreshKey;
  private final Authentication authentication;
  private final MultiFactorAuthFactory multiFactorAuthFactory;
  private final SessionRepository sessionRepository;
  private final DatabaseReader geoDatabaseReader;

  private AuthenticationLoginController(
    @Qualifier("authenticationKey") Key authenticationKey,
    @Qualifier("refreshKey") Key refreshKey,
    MemberRepository memberRepository, Authentication authentication,
    MultiFactorAuthFactory multiFactorAuthFactory,
    SessionRepository sessionRepository, DatabaseReader geoDatabaseReader
  ) {
    super(authenticationKey, memberRepository);
    this.refreshKey = refreshKey;
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
    var loginFuture = login(request, body.getString("email").trim(),
      body.getString("password"), body.getString("multi_factor_code"));
    loginFuture.thenAccept(result -> applyLoginResponseStatus(response, result));
    return loginFuture;
  }

  private void applyLoginResponseStatus(
    HttpServletResponse response, Map<String, Object> result
  ) {
    if ((boolean) result.get("success")) {
      response.setStatus(HttpServletResponse.SC_OK);
      return;
    }
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
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
      "authentication_token", authenticationToken, "refresh_token", refreshToken));
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
      country = location.getCountry().getName();
      city = location.getCity().getName();
      platform = UserAgent.create(request.getHeader("User-Agent"))
        .findPlatform();
    } catch (Exception ignored) {
    }
    var session = Session.create(sessionId, memberId, SessionStatus.ACTIVE,
      platform, ipAddress, country, city, System.currentTimeMillis(),
      refreshToken, System.currentTimeMillis());
    sessionRepository.save(session);
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

  @PanelEndpoint
  @RequestMapping(path = "/authentication/logout/", method = RequestMethod.GET)
  public void logout(HttpServletRequest request) {
    sessionRepository.findById(findSessionId(request))
      .thenAccept(session -> logout(session.get()));
  }

  private void logout(Session session) {
    session.close();
    sessionRepository.save(session);
  }

  @RequestMapping(path = "/authentication/isValid/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> isValid(
    @RequestBody String payload, HttpServletResponse response
  ) {
    return isValid(authenticationKey(), payload, response)
      .thenApply(result -> Map.of("isValid", result.getKey()));
  }

  private CompletableFuture<Map.Entry<Boolean, Claims> > isValid(
    Key key, String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var result = verifyToken(key, body.getString("token"));
    if (result.getKey() != HttpServletResponse.SC_ACCEPTED) {
      response.setStatus(result.getKey());
      return CompletableFuture.completedFuture(
        new AbstractMap.SimpleEntry<>(false, result.getValue()));
    }
    var memberId = UUID.fromString(result.getValue().get("id", String.class));
    return memberRepository().existsById(memberId)
      .thenApply(exists -> new AbstractMap.SimpleEntry<>(exists, result.getValue()));
  }

  private Map.Entry<Integer, Claims> verifyToken(Key key, String token) {
    try {
      return new AbstractMap.SimpleEntry(HttpServletResponse.SC_ACCEPTED,
        Jwts.parser()
          .setSigningKey(key)
          .build()
          .parseClaimsJws(token)
          .getPayload());
    } catch (ExpiredJwtException exception) {
      return new AbstractMap.SimpleEntry(
        HttpServletResponse.SC_EXPECTATION_FAILED, null);
    } catch (Exception exception) {
      return new AbstractMap.SimpleEntry(
        HttpServletResponse.SC_FORBIDDEN, null);
    }
  }
}
