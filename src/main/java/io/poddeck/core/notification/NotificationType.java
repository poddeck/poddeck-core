package io.poddeck.core.notification;

public enum NotificationType {
  REPORT,
  SUCCESS,
  WARNING,
  ERROR;

  public boolean isReport() {
    return this == REPORT;
  }

  public boolean isSuccess() {
    return this == SUCCESS;
  }

  public boolean isWarning() {
    return this == WARNING;
  }

  public boolean isError() {
    return this == ERROR;
  }
}
