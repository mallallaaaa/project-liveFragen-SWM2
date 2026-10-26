# Project description for Max
Web application that allows participants to submit and vote on questions during lectures, presentations, or events. Presenters can monitor the most relevant questions, mark them as answered, and understand audience interests through visualizations. In future versions, AI will help categorize, group, and prioritize similar questions.
So we have as the main actors:
- Participant: submits questions, views existing questions and votes.
- Presenter: monitors questions, identifies the most important ones and marks them as answered.

## US01 – Veranstaltung erstellen

Voraussetzung: Java 21. Im Projektverzeichnis starten:

```sh
cd backend
./mvnw spring-boot:run
```

Im Browser `http://localhost:8080/create-event.html` öffnen. Die Seite verwendet
das vorhandene OCCOR-Design und wird einschließlich JavaScript und Hintergrundbild
von Spring Boot ausgeliefert. Ein separater Frontend-Server ist nicht nötig.
Nach Änderungen an Frontend-Dateien das Backend neu starten.

Die aktuelle US01-Version benötigt einen Titel (1 bis 200 Zeichen nach Entfernen
äußerer Leerzeichen). Leere Titel und reine Leerzeichen werden im Frontend und
Backend abgewiesen. `POST /api/events` empfängt beispielsweise:

```json
{"title": "Software Engineering lecture"}
```

Bei Erfolg liefert die API `201 Created` mit `id`, `title` und `createdAt`.
Die Seite zeigt den gespeicherten Titel und die Veranstaltungs-ID an. Gleiche
Titel sind erlaubt; jede Veranstaltung erhält eine eigene ID. Ungültige Anfragen
liefern `400 Bad Request` mit einer verständlichen `message`.

Die Veranstaltung wird in H2 gespeichert. Mit der vorhandenen Standardkonfiguration
ist die Datenbank im Arbeitsspeicher; beim Neustart gehen die Daten verloren.
Die spätere Verknüpfung der Fragen aus US03/US04 kann die Veranstaltungs-ID
verwenden. Anmeldung ist gemäß MVP nicht Teil dieser Implementierung.

Manuell testen:

1. Einen Titel eingeben und **Create event** drücken. Die Bestätigung enthält
   Titel und ID; das Eingabefeld wird geleert. In den Browser-Entwicklertools
   unter Network ist `POST /api/events` mit Status `201` zu sehen.
2. Einen weiteren Event mit demselben Titel erstellen: Er erhält eine andere ID.
3. Ohne Titel oder mit ausschließlich Leerzeichen absenden: Eine Fehlermeldung
   erscheint, ohne dass eine Anfrage gesendet wird. Maximal 200 Zeichen sind erlaubt.
4. Das Backend stoppen und erneut absenden: Eine Fehlermeldung erscheint und der
   Titel bleibt erhalten. Bei einer unterbrochenen Verbindung kann die Antwort
   verloren gehen, obwohl das Backend bereits gespeichert hat; daher den
   Verbindungsstatus vor erneutem Absenden prüfen.

Automatisierte Tests, jeweils vom Projektverzeichnis:

```sh
cd backend
./mvnw clean test
```

```sh
node --test frontend/create-event.test.cjs frontend/event.test.cjs
```

Die Backend-Tests prüfen echte HTTP-Anfragen mit H2, gespeicherte IDs und Titel,
leere und ungültige Eingaben, die 200-Zeichen-Grenze und statische Ressourcen.
Die JavaScript-Tests benötigen Node.js mit integriertem Test-Runner und prüfen
Validierung, POST-Daten, Bestätigung, sichere Textdarstellung, Fehlerbehandlung
und das Verhindern doppelter Anfragen während des Absendevorgangs.

## US02 – Über einen Link beitreten

Nach dem Erstellen einer Veranstaltung erscheint ein anklickbarer Link wie
`http://localhost:8080/event.html?eventId=1`. Der Präsentierende kann diesen Link
kopieren und an die Teilnehmer weitergeben. Der Link verwendet automatisch die
Adresse, über die die Erstellungsseite geöffnet wurde.

Beim Öffnen lädt die Teilnehmerseite die Veranstaltung über `GET /api/events/{id}`
und zeigt Titel, ID und eine Beitrittsbestätigung. Weder Registrierung noch Login
sind erforderlich. Die Teilnahme ist ein öffentlicher, zustandsloser Zugriff:
Es werden kein Benutzerkonto und kein Teilnehmerdatensatz angelegt. Die ID im
Link bestimmt die Veranstaltung und bleibt auch beim Neuladen erhalten.

Ungültige Links und unbekannte Veranstaltungen zeigen verständliche Meldungen.
Die API liefert bei ungültigen IDs `400`, bei unbekannten IDs `404`. Bei einem
Netzwerk- oder Serverfehler bietet die Seite **Try again** an.

Manuell testen:

1. Das Backend starten und `http://localhost:8080/create-event.html` öffnen.
2. Eine Veranstaltung erstellen, den angezeigten Link kopieren und in einem
   privaten Browserfenster öffnen. Titel und ID müssen mit der erstellten
   Veranstaltung übereinstimmen; es erscheint keine Anmeldeaufforderung.
3. Eine zweite Veranstaltung erstellen und ihren Link öffnen: Ihr eigener Titel
   und ihre eigene ID erscheinen. Ein Neuladen behält die Veranstaltung bei.
4. `/event.html` ohne ID oder mit `?eventId=abc` öffnen: Der Link wird als ungültig
   erkannt. Mit `?eventId=9223372036854775807` erscheint ein Hinweis auf eine
   unbekannte Veranstaltung.
5. Das Backend stoppen und die Teilnehmerseite neu laden. Eine Fehlermeldung mit
   **Try again** erscheint. Wegen der In-Memory-Datenbank gehen die Veranstaltungen
   beim Neustart verloren und müssen neu erstellt werden.

`localhost` funktioniert auf demselben Rechner. Für andere Geräte muss der Link
eine von ihnen erreichbare Serveradresse verwenden; die Erstellungsseite über
diese Adresse öffnen, damit sie den passenden Link erzeugt. Die Verknüpfung der
Fragenfunktionen US03/US04 mit dieser Veranstaltungs-ID wird separat integriert.
Die Teilnehmerseite enthält deshalb derzeit noch
keine Fragenliste und kein Frageformular.

Zusätzlich prüfen die automatisierten Tests den Zugriff ohne Anmeldung,
Veranstaltungstrennung, fehlerhafte Links, Wiederholungsversuche und die
Auslieferung der Teilnehmerseite.

## US03 – Fragen einreichen

Voraussetzung: Java 21. Im Projektverzeichnis starten:

```sh
cd backend
./mvnw spring-boot:run
```

Anschließend `http://localhost:8080/question.html` im Browser öffnen. Spring Boot
liefert die Dateien aus `frontend/` über denselben Server wie `/api/questions`
aus; die HTML-Datei daher nicht direkt per `file://` oder über einen separaten
Frontend-Server öffnen. Nach Änderungen an Frontend-Dateien das Backend neu
starten, damit Maven die Ressourcen erneut kopiert.

Manuell testen:

1. Eine Frage eingeben und mit **Submit** oder Enter absenden. Eine Bestätigung
   erscheint und das Eingabefeld wird geleert. In den Browser-Entwicklertools
   liefert `POST /api/questions` den Status `201` und die gespeicherte Frage
   einschließlich ID und Zeitstempel.
2. Ein leeres Feld oder ausschließlich Leerzeichen absenden. Eine Fehlermeldung
   erscheint und es wird keine Anfrage gesendet. Maximal 500 Zeichen sind erlaubt.
3. Das Backend stoppen und erneut absenden. Eine verständliche Fehlermeldung
   erscheint; die Eingabe bleibt für einen weiteren Versuch erhalten.

Die vorhandene H2-Konfiguration verwendet eine In-Memory-Datenbank. Fragen werden
für die Laufzeit des Backends gespeichert und gehen beim Neustart verloren.

Automatisierte Tests (jeweils vom Projektverzeichnis):

```sh
cd backend
./mvnw test
```

```sh
node --test frontend/question.test.cjs
```

Die Backend-Tests prüfen echte HTTP-Anfragen, Speicherung in H2, ungültige
Eingaben, die 500-Zeichen-Grenze und die Auslieferung des Frontends samt Assets.
Die JavaScript-Tests benötigen Node.js mit integriertem Test-Runner und prüfen
Validierung, POST-Daten, Bestätigung, Fehlerbehandlung und doppelte Anfragen.
