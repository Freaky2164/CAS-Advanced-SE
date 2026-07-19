package de.frauenhaus.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Einheitliche REST-Fehler für Upload-Grenzen, damit das Frontend klare Hinweise
 * auf zu große Dokumente erhält.
 *
 * @author Nils
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Übersetzt eine ResponseStatusException in eine ProblemDetail-Antwort.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> responseStatus(ResponseStatusException ex) {
        String detail = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), detail);
        return ResponseEntity.status(ex.getStatusCode()).body(problem);
    }

    /**
     * Übersetzt eine überschrittene Upload-Größe in eine ProblemDetail-Antwort.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> maxUploadSizeExceeded(MaxUploadSizeExceededException ignored) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "Datei ist zu groß – maximal 10 MB");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
    }
}
