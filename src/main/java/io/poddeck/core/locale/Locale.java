package io.poddeck.core.locale;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class Locale {
  public static Locale createAndLoad(String language) throws Exception {
    var configuration = create(language);
    configuration.load();
    return configuration;
  }

  @Getter
  private final String language;
  private final Map<String, String> locale = Maps.newHashMap();

  private static final String LOCALE_PATH = "/locale/%s.json";

  public void load() throws Exception {
    var resource = new ClassPathResource(String.format(LOCALE_PATH, language));
    try (var inputStream = resource.getInputStream()) {
      var json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      deserialize(new JSONObject(json));
    }
  }

  private void deserialize(JSONObject json) {
    for (var key : json.keySet()) {
      locale.put(key, json.getString(key));
    }
  }

  public void addLocale(Locale newLocale) {
    locale.putAll(newLocale.locale);
  }

  public void removeLocale(Locale otherLocale) {
    locale.keySet().removeAll(otherLocale.locale.keySet());
  }

  public String findText(String key) {
    if (!locale.containsKey(key)) {
      return key;
    }
    return locale.get(key);
  }
}