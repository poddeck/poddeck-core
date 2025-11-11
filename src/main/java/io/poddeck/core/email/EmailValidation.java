package io.poddeck.core.email;

import lombok.RequiredArgsConstructor;

import java.util.regex.Pattern;

@RequiredArgsConstructor(staticName = "create")
public final class EmailValidation {
  public static boolean validate(String email) {
    var validation = create(email);
    return validation.checkEmailFormat();
  }

  private final String email;

  private static final Pattern EMAIL_PATTERN = Pattern.compile(
    "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

  public boolean checkEmailFormat() {
    if (email == null) {
      return false;
    }
    var matcher = EMAIL_PATTERN.matcher(email);
    return matcher.matches();
  }
}
