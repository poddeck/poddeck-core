package io.poddeck.core.api.authentication;

import io.jsonwebtoken.Jwts;
import io.poddeck.core.hashing.Hashing;
import io.poddeck.core.member.MemberRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public final class Authentication {
  private final MemberRepository memberRepository;
  private final Key authenticationKey;
  private final Key refreshKey;
  private final Hashing hashing;

  private Authentication(
    MemberRepository memberRepository,
    @Qualifier("authenticationKey") Key authenticationKey,
    @Qualifier("refreshKey") Key refreshKey, Hashing hashing
  ) {
    this.memberRepository = memberRepository;
    this.authenticationKey = authenticationKey;
    this.refreshKey = refreshKey;
    this.hashing = hashing;
  }

  public CompletableFuture<Boolean> isAuthenticated(
    String email, String password
  ) {
    var futureResponse = new CompletableFuture<Boolean>();
    if (email == null || password == null || email.isEmpty()) {
      futureResponse.complete(false);
      return futureResponse;
    }
    memberRepository.existsByEmail(email)
      .thenAccept(exists -> isAuthenticated(email, password, exists)
        .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Boolean> isAuthenticated(
    String email, String password, boolean exists
  ) {
    var futureResponse = new CompletableFuture<Boolean>();
    if (!exists) {
      futureResponse.complete(false);
      return futureResponse;
    }
    memberRepository.findByEmail(email)
      .thenAccept(member -> futureResponse.complete(
        hashing.matches(password, member.get().passwordHash())));
    return futureResponse;
  }

  private static final long MAXIMUM_AUTHENTICATION_EXPIRATION_TIME =
    1000L * 60 * 10;

  public String generateAuthenticationToken(UUID memberId, UUID sessionId) {
    return generateAuthenticationToken(memberId, sessionId,
      System.currentTimeMillis() + MAXIMUM_AUTHENTICATION_EXPIRATION_TIME);
  }

  public String generateAuthenticationToken(
    UUID memberId, UUID sessionId, long expiration
  ) {
    if (expiration - System.currentTimeMillis() > MAXIMUM_AUTHENTICATION_EXPIRATION_TIME) {
      expiration = System.currentTimeMillis() +
        MAXIMUM_AUTHENTICATION_EXPIRATION_TIME;
    }
    var expirationDate = new Date(expiration);
    return Jwts.builder().expiration(expirationDate)
      .claim("id", memberId.toString())
      .claim("session", sessionId.toString())
      .signWith(authenticationKey)
      .compact();
  }

  private static final long MAXIMUM_REFRESH_EXPIRATION_TIME =
    1000L * 60 * 60 * 24 * 30;

  public String generateRefreshToken(UUID memberId, UUID sessionId) {
    return generateRefreshToken(memberId, sessionId,
      System.currentTimeMillis() + MAXIMUM_REFRESH_EXPIRATION_TIME);
  }

  public String generateRefreshToken(UUID memberId, UUID sessionId, long expiration) {
    if (expiration - System.currentTimeMillis() > MAXIMUM_REFRESH_EXPIRATION_TIME) {
      expiration = System.currentTimeMillis() + MAXIMUM_REFRESH_EXPIRATION_TIME;
    }
    var expirationDate = new Date(expiration);
    return Jwts.builder().expiration(expirationDate)
      .claim("id", memberId.toString())
      .claim("session", sessionId.toString())
      .signWith(refreshKey)
      .compact();
  }
}
