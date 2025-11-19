package io.poddeck.core.api.authentication;

import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.member.MemberRepository;
import io.poddeck.core.session.Session;
import io.poddeck.core.session.SessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;

@RestController
public final class AuthenticationLogoutController extends AuthenticationController {
  private final SessionRepository sessionRepository;

  private AuthenticationLogoutController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, SessionRepository sessionRepository
  ) {
    super(authenticationKey, memberRepository);
    this.sessionRepository = sessionRepository;
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
}
