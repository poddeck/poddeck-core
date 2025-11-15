package io.poddeck.core.locale;

import io.poddeck.core.member.Member;
import io.poddeck.core.member.MemberRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Translation {
  private final MemberRepository memberRepository;
  private final Locales locales;

  /**
   * Translates a locale for a member
   * @param memberId The id of the member
   * @param key The key of the locale
   * @return A future that contains the translated locale
   */
  public CompletableFuture<String> translateMember(UUID memberId, String key) {
    return memberRepository.findById(memberId)
      .thenApply(member -> translateMember(member.get(), key));
  }

  /**
   * Translates a locale for a member
   * @param member The member
   * @param key The key of the locale
   * @return A future that contains the translated locale
   */
  public String translateMember(Member member, String key) {
    return translate(member.language(), key);
  }

  /**
   * Translates a locale into a specific language
   * @param language The language
   * @param key The key of the locale
   * @return A future that contains the translated locale
   */
  public String translate(String language, String key) {
    if (!locales.hasLanguage(language)) {
      return key;
    }
    return locales.findLocale(language).get().findText(key);
  }
}
