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
   * @param args The arguments for the translation
   * @return A future that contains the translated locale
   */
  public CompletableFuture<String> translateMember(
    UUID memberId, String key, String... args
  ) {
    return memberRepository.findById(memberId)
      .thenApply(member -> translateMember(member.get(), key, args));
  }

  /**
   * Translates a locale for a member
   * @param member The member
   * @param key The key of the locale
   * @param args The arguments for the translation
   * @return A future that contains the translated locale
   */
  public String translateMember(Member member, String key, String... args) {
    return translate(member.language(), key, args);
  }

  /**
   * Translates a locale into a specific language
   * @param language The language
   * @param key The key of the locale
   * @param args The arguments for the translation
   * @return A future that contains the translated locale
   */
  public String translate(String language, String key, String... args) {
    if (!locales.hasLanguage(language)) {
      return format(key, args);
    }
    return format(locales.findLocale(language).get().findText(key), args);
  }

  /**
   * Is used to format the translated message and replacing placeholders
   * @param template The translated message
   * @param args The arguments for the translation
   * @return The formatted locale
   */
  private String format(String template, String... args) {
    if (args == null || args.length == 0) {
      return template;
    }
    var result = template;
    for (int i = 0; i < args.length; i++) {
      result = result.replace("{" + i + "}", args[i]);
    }
    return result;
  }
}
