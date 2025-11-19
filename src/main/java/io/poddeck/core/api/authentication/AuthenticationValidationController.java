package io.poddeck.core.api.authentication;

import io.jsonwebtoken.Claims;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.member.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class AuthenticationValidationController extends AuthenticationController {
  private AuthenticationValidationController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository
  ) {
    super(authenticationKey, memberRepository);
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
}
