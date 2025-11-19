package io.poddeck.core.api.authentication;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.poddeck.core.api.security.panel.PanelRestController;
import io.poddeck.core.member.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;

import java.security.Key;
import java.util.AbstractMap;
import java.util.Map;

public class AuthenticationController extends PanelRestController {
  protected AuthenticationController(
    Key authenticationKey, MemberRepository memberRepository
  ) {
    super(authenticationKey, memberRepository);
  }

  protected Map.Entry<Integer, Claims> verifyToken(Key key, String token) {
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
