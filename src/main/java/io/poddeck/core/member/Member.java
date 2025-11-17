package io.poddeck.core.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.UUID;

@Entity
@Table(name = "member")
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "create")
public final class Member {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;
  @Column(name = "name", nullable = false)
  private String name;
  @Column(name = "email", nullable = false, unique = true)
  private String email;
  @Column(name = "password", nullable = false)
  private String passwordHash;
  @Column(name = "language")
  private String language;
  @Column(name = "join", nullable = false)
  private long join;

  public void changeName(String newName) {
    this.name = newName;
  }

  public void changeEmail(String newEmail) {
    this.email = newEmail;
  }

  public void changePassword(String newPasswordHash) {
    this.passwordHash = newPasswordHash;
  }

  public void changeLanguage(String newLanguage) {
    this.language = newLanguage;
  }

  public static Member unknown(UUID id) {
    return create(id, "Unknown", "Unknown", "", "", -1);
  }
}
