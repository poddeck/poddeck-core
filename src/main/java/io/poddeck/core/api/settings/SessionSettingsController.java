package io.poddeck.core.api.settings;

import com.google.common.collect.Maps;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.api.security.panel.PanelRestController;
import io.poddeck.core.member.MemberRepository;
import io.poddeck.core.session.Session;
import io.poddeck.core.session.SessionRepository;
import io.poddeck.core.session.SessionStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public final class SessionSettingsController extends PanelRestController {
  private final SessionRepository sessionRepository;

  private SessionSettingsController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, SessionRepository sessionRepository
  ) {
    super(authenticationKey, memberRepository);
    this.sessionRepository = sessionRepository;
  }

  @PanelEndpoint
  @RequestMapping(path = "/settings/sessions/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> sessions(
    HttpServletRequest request
  ) {
    var memberSessionId = findSessionId(request);
    return sessionRepository.findSessionsOfMemberByStatus(
        findMemberId(request), SessionStatus.ACTIVE)
      .thenApply(sessions -> Map.of("sessions", sessions.stream().map(session ->
        assemblySessionInformation(memberSessionId, session)).toList()));
  }

  private Map<String, Object> assemblySessionInformation(
    UUID memberSessionId, Session session
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", session.id());
    information.put("platform", session.devicePlatform());
    information.put("country", session.country());
    information.put("city", session.city());
    information.put("openTime", session.openTime());
    information.put("status", session.status());
    information.put("isCurrent", memberSessionId.equals(session.id()));
    return information;
  }

  @PanelEndpoint
  @RequestMapping(path = "/settings/session/close/", method = RequestMethod.POST)
  public void closeSession(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var sessionId = body.getUUID("session");
    var memberId = findMemberId(request);
    var memberSessionId = findSessionId(request);
    sessionRepository.findById(sessionId)
      .thenAccept(exists -> closeSession(memberId, memberSessionId, exists));
  }

  private void closeSession(
    UUID memberId, UUID memberSessionId, Optional<Session> sessionOptional
  ) {
    if (sessionOptional.isEmpty()) {
      return;
    }
    var session = sessionOptional.get();
    if (!session.memberId().equals(memberId)) {
      return;
    }
    if (memberSessionId.equals(session.id())) {
      return;
    }
    session.close();
    sessionRepository.save(session);
  }
}
