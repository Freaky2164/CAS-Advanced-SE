package de.frauenhaus.service;

import de.frauenhaus.audit.Revision;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Liefert die Änderungshistorie auditierter Stammdaten-Datensätze aus
 * Hibernate Envers als DTOs mit Feld-Diffs.
 *
 * @author Ole
 */
@Service
@Transactional(readOnly = true)
public class AuditService {

    /**
     * Änderung eines einzelnen Feldes mit altem und neuem Wert.
     */
    public record FeldAenderung(String feld, Object alt, Object neu) { }

    /**
     * Eintrag der Änderungshistorie: eine Revision mit Bearbeiter, Zeitpunkt,
     * Änderungstyp und den Feld-Änderungen.
     */
    public record VerlaufEintrag(Long revision, Instant zeitpunkt, String benutzername, String typ,
                                 List<FeldAenderung> aenderungen) { }

    private final EntityManager entityManager;
    private final PersistenceUnitUtil persistenceUnitUtil;

    /**
     * Erzeugt den Service mit EntityManager und PersistenceUnitUtil.
     *
     * @param entityManager der EntityManager für Envers-Abfragen
     * @param entityManagerFactory die Factory zum Auflösen von Entity-IDs
     */
    public AuditService(EntityManager entityManager, EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManager;
        this.persistenceUnitUtil = entityManagerFactory.getPersistenceUnitUtil();
    }

    /**
     * Liefert die Änderungshistorie eines Datensatzes chronologisch aufsteigend.
     *
     * @param entityClass die auditierte Entity-Klasse
     * @param id die ID des Datensatzes
     * @return die Verlaufseinträge mit Feld-Diffs
     */
    public List<VerlaufEintrag> verlauf(Class<?> entityClass, Object id) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        @SuppressWarnings("unchecked")
        List<Object[]> revisionen = auditReader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        if (revisionen.isEmpty() && entityManager.find(entityClass, id) == null) {
            throw new ResponseStatusException(NOT_FOUND, entityClass.getSimpleName() + " " + id + " nicht gefunden");
        }

        List<VerlaufEintrag> eintraege = new ArrayList<>();
        Object vorherigeVersion = null;
        for (Object[] revision : revisionen) {
            Object aktuelleVersion = revision[0];
            Revision revisionInfo = (Revision) revision[1];
            RevisionType revisionType = (RevisionType) revision[2];
            List<FeldAenderung> aenderungen = switch (revisionType) {
                case ADD -> vergleiche(entityClass, null, aktuelleVersion);
                case MOD -> vergleiche(entityClass, vorherigeVersion, aktuelleVersion);
                case DEL -> vergleiche(entityClass, vorherigeVersion, null);
            };
            String benutzername = revisionInfo.getUsername() != null ? revisionInfo.getUsername() : "system";
            eintraege.add(new VerlaufEintrag(
                    revisionInfo.getRev(),
                    Instant.ofEpochMilli(revisionInfo.getRevtstmp()),
                    benutzername,
                    typ(revisionType),
                    aenderungen));
            if (revisionType != RevisionType.DEL) {
                vorherigeVersion = aktuelleVersion;
            }
        }
        return eintraege;
    }

    /**
     * Vergleicht zwei Versionen eines Datensatzes feldweise.
     */
    private List<FeldAenderung> vergleiche(Class<?> entityClass, Object alt, Object neu) {
        List<FeldAenderung> aenderungen = new ArrayList<>();
        for (Field field : relevanteFelder(entityClass)) {
            Object alterWert = leseWert(field, alt);
            Object neuerWert = leseWert(field, neu);
            if (!Objects.equals(alterWert, neuerWert)) {
                aenderungen.add(new FeldAenderung(field.getName(), alterWert, neuerWert));
            }
        }
        return aenderungen;
    }

    /**
     * Ermittelt die für den Vergleich relevanten Felder: keine statischen,
     * transienten oder synthetischen Felder, keine IDs und keine
     * Collection-Relationen.
     */
    private List<Field> relevanteFelder(Class<?> entityClass) {
        List<Field> felder = new ArrayList<>();
        ReflectionUtils.doWithFields(entityClass, feld -> {
            if (Modifier.isStatic(feld.getModifiers()) || Modifier.isTransient(feld.getModifiers())
                    || feld.isSynthetic() || feld.isAnnotationPresent(jakarta.persistence.Id.class)
                    || istCollectionOderListenRelation(feld)) {
                return;
            }
            felder.add(feld);
        });
        return felder;
    }

    /**
     * Prüft, ob das Feld eine Collection oder eine Listen-Relation ist.
     */
    private boolean istCollectionOderListenRelation(Field feld) {
        return Collection.class.isAssignableFrom(feld.getType())
                || feld.isAnnotationPresent(jakarta.persistence.OneToMany.class)
                || feld.isAnnotationPresent(jakarta.persistence.ManyToMany.class)
                || feld.isAnnotationPresent(jakarta.persistence.ElementCollection.class);
    }

    /**
     * Liest den Wert eines Feldes; Relationen werden auf ihre ID abgebildet,
     * Enums auf ihren Namen.
     */
    private Object leseWert(Field feld, Object entity) {
        if (entity == null) {
            return null;
        }
        ReflectionUtils.makeAccessible(feld);
        Object wert = ReflectionUtils.getField(feld, entity);
        if (wert == null) {
            return null;
        }
        if (feld.isAnnotationPresent(jakarta.persistence.ManyToOne.class)
                || feld.isAnnotationPresent(jakarta.persistence.OneToOne.class)) {
            Object id = persistenceUnitUtil.getIdentifier(wert);
            return id != null ? id : wert.toString();
        }
        if (wert instanceof Enum<?> enumeration) {
            return enumeration.name();
        }
        return wert;
    }

    /**
     * Bildet den Envers-Revisionstyp auf die SQL-übliche Bezeichnung ab.
     */
    private static String typ(RevisionType revisionType) {
        return switch (revisionType) {
            case ADD -> "INSERT";
            case MOD -> "UPDATE";
            case DEL -> "DELETE";
        };
    }
}
