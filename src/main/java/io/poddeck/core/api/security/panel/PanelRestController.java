package io.poddeck.core.api.security.panel;

import io.jsonwebtoken.Jwts;
import io.poddeck.core.member.Member;
import io.poddeck.core.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.security.Key;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter(AccessLevel.PROTECTED)
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PanelRestController {
  private final Key secretKey;
  private final MemberRepository memberRepository;

  /**
   * Is used to find the id of the member that send the request
   * @param request The request
   * @return The id of the member
   */
  protected UUID findMemberId(HttpServletRequest request) {
    return findMemberId(findApiKey(request));
  }

  /**
   * Is used to find the id of a member inside an api key
   * @param apiKey The api key
   * @return The id of the member
   */
  protected UUID findMemberId(String apiKey) {
    return UUID.fromString(Jwts.parser().setSigningKey(secretKey).build()
      .parseClaimsJws(apiKey).getPayload().get("id", String.class));
  }

  /**
   * Is used to find the id of the session that send the request
   * @param request The request
   * @return The id of the session
   */
  protected UUID findSessionId(HttpServletRequest request) {
    return findSessionId(findApiKey(request));
  }

  /**
   * Is used to find the id of a session inside an api key
   * @param apiKey The api key
   * @return The id of the session
   */
  protected UUID findSessionId(String apiKey) {
    return UUID.fromString(Jwts.parser().setSigningKey(secretKey).build()
      .parseClaimsJws(apiKey).getPayload().get("session", String.class));
  }

  /**
   * Is used to find the member that send the request
   * @param request The request
   * @return A future that contains the member
   */
  protected CompletableFuture<Member> findMember(HttpServletRequest request) {
    var apiKey = request.getHeader("Authorization").replace("Bearer ", "");
    return memberRepository.findById(findMemberId(apiKey))
      .thenApply(Optional::get);
  }

  /**
   * Is used to find the api key that is sent via a request
   * @param request The request
   * @return The api key
   */
  protected String findApiKey(HttpServletRequest request) {
    return request.getHeader("Authorization").replace("Bearer ", "");
  }

  /**
   * Checks whether an api key is valid
   * @param apiKey The api key
   * @return Is true if api key is valid, otherwise false
   */
  protected boolean isValidApiKey(String apiKey) {
    try {
      Jwts.parser()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(apiKey);
      return true;
    } catch (Exception exception) {
      return false;
    }
  }
}
