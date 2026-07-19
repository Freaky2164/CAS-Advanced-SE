package de.frauenhaus.security;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * @author Nils
 *     <p>Anwendungsbenutzer mit BCrypt-Hash (ersetzt compucrash.user_def / CLoginFrame).
 */
@Entity
@Table(name = "app_user", schema = "app")
public class AppUser {

  /**
   * @author Nils
   *     <p>Rollen der Anwendung: Administration bzw. Sachbearbeitung.
   */
  public enum Role {
    ADMIN,
    SACHBEARBEITUNG
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(nullable = false)
  private boolean enabled = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  /**
   * @author Nils
   *     <p>Für JPA.
   */
  protected AppUser() {}

  public AppUser(String username, String passwordHash, Role role) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.role = role;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
