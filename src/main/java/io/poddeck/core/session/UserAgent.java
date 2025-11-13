package io.poddeck.core.session;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "create")
public final class UserAgent {
  private final String content;

  public String findPlatform() {
    var processed = content.toLowerCase();
    if (processed.contains("windows")) {
      return "WINDOWS";
    } else if (processed.contains("x11")) {
      return "LINUX";
    } else if (processed.contains("iphone") || processed.contains("ipad")) {
      return "IOS";
    } else if (processed.contains("mac")) {
      return "MAC";
    } else if (processed.contains("android")) {
      return "ANDROID";
    }
    return "";
  }
}
