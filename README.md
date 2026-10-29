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
diese Adresse öffnen, damit sie den passenden Link erzeugt. Über **Ask and view
questions** gelangt der Teilnehmer zu `/question.html?eventId=<id>`. Die
Veranstaltungs-ID wird dabei für das Einreichen und Abrufen der Fragen übernommen.

Zusätzlich prüfen die automatisierten Tests den Zugriff ohne Anmeldung,
Veranstaltungstrennung, fehlerhafte Links, Wiederholungsversuche und die
Auslieferung der Teilnehmerseite.

## US03 – Fragen einreichen

Voraussetzung: Java 21. Im Projektverzeichnis starten:

```sh
cd backend
./mvnw spring-boot:run
```

Anschließend eine Veranstaltung unter `http://localhost:8080/create-event.html`
erstellen, ihren Teilnehmerlink öffnen und **Ask and view questions** wählen.
Spring Boot
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
node --test frontend/question.test.cjs frontend/question-list.test.cjs
```

Die Backend-Tests prüfen echte HTTP-Anfragen, Speicherung in H2, ungültige
Eingaben, die 500-Zeichen-Grenze und die Auslieferung des Frontends samt Assets.
Die JavaScript-Tests benötigen Node.js mit integriertem Test-Runner und prüfen
Validierung, POST-Daten, Bestätigung, Fehlerbehandlung und doppelte Anfragen.

**API-Vertrag:** `POST /api/questions` benötigt nun die Veranstaltungs-ID im
JSON-Body, beispielsweise `{"text":"How does this work?","eventId":1}`. Jede Frage
ist über einen nicht leeren Fremdschlüssel mit einer existierenden Veranstaltung
verbunden. Fehlende oder ungültige IDs liefern `400`, unbekannte Veranstaltungen
`404`. Die Antwort enthält zusätzlich `eventId`. Alte Clients ohne `eventId`
müssen entsprechend angepasst werden; es gibt keine globale Standardveranstaltung.

## US04 – Fragen anzeigen

Unter `/question.html?eventId=<id>` erscheint unter dem Eingabeformular
die Fragenliste der gewählten Veranstaltung. `GET /api/questions?eventId=<id>`
liefert ausschließlich deren Fragen als JSON-Array, in Einreichungsreihenfolge
(ID aufsteigend). Die Liste lädt beim
Öffnen der Seite, über **Refresh** und automatisch nach erfolgreichem Einreichen
einer Frage. Bei einer leeren Liste erscheint ein Hinweis. Schlägt das Laden
fehl, erscheint eine Fehlermeldung; bereits angezeigte Fragen bleiben erhalten.

Die Veranstaltungs-ID ist verpflichtend. Ohne gültigen Event-Link werden
Formular und Aktualisierung deaktiviert. Die API prüft die Veranstaltung ebenfalls;
es gibt keinen Endpunkt zum Abrufen einer globalen Fragenliste.

Manuell testen:

1. Das Backend starten, eine Veranstaltung erstellen und über ihren Teilnehmerlink
   **Ask and view questions** öffnen. Ohne gespeicherte Fragen erscheint
   „No questions yet. Be the first to ask!“.
2. Eine Frage absenden. Sie erscheint automatisch in der Liste, zusätzlich zur
   Bestätigung von US03.
3. Dieselbe Seite in einem zweiten Tab öffnen und dort eine weitere Frage
   absenden. Im ersten Tab **Refresh** drücken: Beide Fragen erscheinen, ohne
   Duplikate. Alternativ die Seite neu laden.
4. Das Backend stoppen und **Refresh** drücken. Die Fehlermeldung erscheint,
   bisher angezeigte Fragen bleiben sichtbar.
5. Zwei unterschiedliche Veranstaltungen erstellen und jeweils ihre Fragenseite
   öffnen. In beiden eine andere Frage einreichen und aktualisieren: Auf jeder
   Seite erscheinen ausschließlich die Fragen ihrer eigenen Veranstaltung.
6. `/question.html` ohne `eventId` öffnen: Ein Hinweis auf den fehlenden Event-Link
   erscheint; es wird keine globale Liste geladen und keine Frage abgeschickt.

Die zusätzlichen Tests prüfen die leere Liste, gespeicherte Fragen und die
Abfolge POST → GET über HTTP mit H2 sowie Laden, Aktualisieren, Fehlerbehandlung
und die sichere Textdarstellung im Frontend. Zusätzlich wird mit zwei
Veranstaltungen geprüft, dass POST und GET keine fremden Fragen zuordnen oder
anzeigen und ungültige bzw. unbekannte Veranstaltungen abgewiesen werden.
Es gibt kein automatisches Polling;
Fragen anderer Teilnehmer erscheinen nach **Refresh** oder einem Neuladen.

Alle Frontend-Tests gemeinsam: `node --test frontend/*.test.cjs`.
