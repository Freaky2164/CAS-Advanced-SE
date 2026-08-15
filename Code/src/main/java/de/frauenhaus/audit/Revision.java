package de.frauenhaus.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Revisionskopf für Hibernate Envers; hält Revisionsnummer, Zeitstempel und
 * Benutzer einer Änderung.
 *
 * @author Ole
 */
@Entity
@Table(name = "revinfo", schema = "app")
@RevisionEntity(AuditRevisionListener.class)
public class Revision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private Long rev;

    @RevisionTimestamp
    @Column(name = "revtstmp", nullable = false)
    private long revtstmp;

    @Column(length = 50)
    private String username;

    /** Liefert die Revisionsnummer. */
    public Long getRev() { return rev; }

    /** Liefert den Zeitstempel der Revision in Millisekunden. */
    public long getRevtstmp() { return revtstmp; }

    /** Liefert den Benutzernamen des Bearbeiters. */
    public String getUsername() { return username; }

    /** Setzt den Benutzernamen des Bearbeiters. */
    public void setUsername(String username) { this.username = username; }
}
