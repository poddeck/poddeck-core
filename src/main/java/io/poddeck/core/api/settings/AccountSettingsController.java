package io.poddeck.core.api.settings;

import com.beust.jcommander.internal.Lists;
import io.poddeck.common.iterator.AsyncIterator;
import io.poddeck.core.api.request.ApiRequestBody;
import io.poddeck.core.api.security.panel.PanelEndpoint;
import io.poddeck.core.api.security.panel.PanelRestController;
import io.poddeck.core.hashing.Hashing;
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
public final class AccountSettingsController extends PanelRestController {
  private final Hashing hashing;

  private AccountSettingsController(
    @Qualifier("authenticationKey") Key authenticationKey,
    MemberRepository memberRepository, Hashing hashing
  ) {
    super(authenticationKey, memberRepository);
    this.hashing = hashing;
  }

  @PanelEndpoint
  @RequestMapping(path = "/settings/account/delete/", method = RequestMethod.POST)
  public CompletableFuture<Map<String, Object>> deleteAccount(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var password = body.getString("password");
    return findMember(request).thenCompose(member ->
      deleteAccount(member, password));
  }

  private CompletableFuture<Map<String, Object>> deleteAccount(
    Member member, String password
  ) {
    if (!hashing.matches(password, member.passwordHash())) {
      return CompletableFuture.completedFuture(Map.of("success", false,
        "error", 1000));
    }
    return deleteAccount(member).thenApply(_ -> Map.of("success", true));
  }

  private CompletableFuture<Void> deleteAccount(Member member) {
    var operations = Lists.<CompletableFuture<Void>>newArrayList();
    operations.add(memberRepository().delete(member));
    //TODO: DELETE ALL MEMBER REFERENCES
    return AsyncIterator.execute(operations, operation -> operation)
      .thenApply(_ -> null);
  }
}
