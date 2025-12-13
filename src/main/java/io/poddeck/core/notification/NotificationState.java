package io.poddeck.core.notification;

public enum NotificationState {
  SEEN,
  UNSEEN;

  public boolean isSeen() {
    return this == SEEN;
  }

  public boolean isUnseen() {
    return this == UNSEEN;
  }
}
