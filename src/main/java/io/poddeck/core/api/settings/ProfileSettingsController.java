package io.poddeck.core.api.settings;

import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.api.security.panel.PanelRestController;
import io.poddeck.core.member.Member;
import io.poddeck.core.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class ProfileSettingsController extends PanelRestController {
  private ProfileSettingsController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository
  ) {
    super(authenticationKey, memberRepository);
  }

  @PanelEndpoint
  @RequestMapping(path = "/settings/profile/username/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> username(
    HttpServletRequest request
  ) {
    return findMember(request)
      .thenApply(member -> Map.of("username", member.name()));
  }

  @PanelEndpoint
  @RequestMapping(path = "/settings/profile/username/change/", method = RequestMethod.POST)
  public CompletableFuture<Void> changeUsername(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var username = body.getSanitizedString("username");
    return findMember(request).thenCompose(member ->
      changeUsername(member, username));
  }

  private CompletableFuture<Void> changeUsername(Member member, String newUsername) {
    member.changeName(newUsername);
    return memberRepository().save(member).thenApply(_ -> null);
  }
}
