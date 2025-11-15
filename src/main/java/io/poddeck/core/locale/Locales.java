package io.poddeck.core.locale;

import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Locales {
  public static Locales create() throws Exception {
    var locales = new Locales();
    locales.setup();
    return locales;
  }

  private final Map<String, Locale> locales = Maps.newHashMap();

  private void setup() throws Exception {
    addLocale(Locale.createAndLoad("en"));
    addLocale(Locale.createAndLoad("de"));
  }

  public void addLocale(Locale locale) {
    if (locales.containsKey(locale.language())) {
      locales.get(locale.language()).addLocale(locale);
      return;
    }
    locales.put(locale.language(), locale);
  }

  public void removeLocale(Locale locale) {
    if (!locales.containsKey(locale.language())) {
      return;
    }
    locales.get(locale.language()).removeLocale(locale);
  }

  public boolean hasLanguage(String language) {
    return locales.containsKey(language);
  }

  public Optional<Locale> findLocale(String language) {
    if (!locales.containsKey(language)) {
      return Optional.empty();
    }
    return Optional.of(locales.get(language));
  }

  public Set<String> languages() {
    return locales.keySet();
  }
}
