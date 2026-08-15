package de.frauenhaus.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Überträgt den angemeldeten Benutzernamen aus Spring Security in den
 * Envers-Revisionskopf, damit die Änderungshistorie den Bearbeiter ausweist.
 *
 * @author Ole
 */
public class AuditRevisionListener implements RevisionListener {

    /**
     * Setzt den Benutzernamen der aktuellen Anmeldung an der neuen Revision;
     * ohne authentifizierten Benutzer wird "system" eingetragen.
     *
     * @param revisionEntity die neu angelegte {@link Revision}
     */
    @Override
    public void newRevision(Object revisionEntity) {
        Revision revision = (Revision) revisionEntity;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !auth.getName().isBlank() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName()
                : "system";
        revision.setUsername(username);
    }
}
