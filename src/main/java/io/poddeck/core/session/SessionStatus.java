package io.poddeck.core.session;

public enum SessionStatus {
  ACTIVE,
  CLOSED;

  public boolean isActive() {
    return this == ACTIVE;
  }

  public boolean isClosed() {
    return this == CLOSED;
  }
}
