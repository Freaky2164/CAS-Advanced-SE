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
 * @author Nils
 *
 * Revisionskopf für Hibernate Envers; hält Zeitstempel und Benutzer der Änderung
 * (ersetzt die History-Metadaten aus dem Altsystem).
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

    public Long getRev() { return rev; }
    public long getRevtstmp() { return revtstmp; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
