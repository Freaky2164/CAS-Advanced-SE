package de.frauenhaus.security;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Anwendungsbenutzer mit BCrypt-gehashtem Passwort, Rolle und Aktiv-Status.
 *
 * @author Paul
 */
@Entity
@Table(name = "app_user", schema = "app")
public class AppUser {

    /**
     * Rollen der Anwendung: Administration bzw. Sachbearbeitung.
     */
    public enum Role { ADMIN, SACHBEARBEITUNG }

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

    /** Parameterloser Konstruktor für JPA. */
    protected AppUser() { }

    /**
     * Legt einen Benutzer mit Benutzername, Passwort-Hash und Rolle an.
     *
     * @param username der eindeutige Benutzername
     * @param passwordHash der BCrypt-Hash des Passworts
     * @param role die Rolle des Benutzers
     */
    public AppUser(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /** Liefert die ID des Benutzers. */
    public Long getId() { return id; }

    /** Liefert den Benutzernamen. */
    public String getUsername() { return username; }

    /** Liefert den BCrypt-Hash des Passworts. */
    public String getPasswordHash() { return passwordHash; }

    /** Setzt den BCrypt-Hash des Passworts. */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** Liefert die Rolle des Benutzers. */
    public Role getRole() { return role; }

    /** Setzt die Rolle des Benutzers. */
    public void setRole(Role role) { this.role = role; }

    /** Gibt an, ob der Benutzer aktiv ist. */
    public boolean isEnabled() { return enabled; }

    /** Setzt den Aktiv-Status des Benutzers. */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** Liefert den Zeitpunkt der Anlage. */
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
