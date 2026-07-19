package de.frauenhaus.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Nils
 *     <p>Übernimmt den angemeldeten Benutzernamen aus Spring Security in den Envers- Revisionskopf,
 *     damit die Änderungshistorie den Bearbeiter anzeigen kann.
 */
public class AuditRevisionListener implements RevisionListener {

  @Override
  public void newRevision(Object revisionEntity) {
    Revision revision = (Revision) revisionEntity;
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username =
        (auth != null
                && auth.isAuthenticated()
                && auth.getName() != null
                && !auth.getName().isBlank()
                && !"anonymousUser".equals(auth.getName()))
            ? auth.getName()
            : "system";
    revision.setUsername(username);
  }
}
