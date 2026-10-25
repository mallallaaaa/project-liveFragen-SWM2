# Project description for Max
Web application that allows participants to submit and vote on questions during lectures, presentations, or events. Presenters can monitor the most relevant questions, mark them as answered, and understand audience interests through visualizations. In future versions, AI will help categorize, group, and prioritize similar questions.
So we have as the main actors:
- Participant: submits questions, views existing questions and votes.
- Presenter: monitors questions, identifies the most important ones and marks them as answered.

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
node --test frontend/question.test.cjs frontend/question-list.test.cjs
```

Die Backend-Tests prüfen echte HTTP-Anfragen, Speicherung in H2, ungültige
Eingaben, die 500-Zeichen-Grenze und die Auslieferung des Frontends samt Assets.
Die JavaScript-Tests benötigen Node.js mit integriertem Test-Runner und prüfen
Validierung, POST-Daten, Bestätigung, Fehlerbehandlung und doppelte Anfragen.

## US04 – Fragen anzeigen

Unter `http://localhost:8080/question.html` erscheint unter dem Eingabeformular
die Fragenliste. `GET /api/questions` liefert alle gespeicherten Fragen als
JSON-Array, in Einreichungsreihenfolge (ID aufsteigend). Die Liste lädt beim
Öffnen der Seite, über **Refresh** und automatisch nach erfolgreichem Einreichen
einer Frage. Bei einer leeren Liste erscheint ein Hinweis. Schlägt das Laden
fehl, erscheint eine Fehlermeldung; bereits angezeigte Fragen bleiben erhalten.

**Offenes Akzeptanzkriterium:** Die Liste ist vorläufig global. Die Zuordnung und
Filterung nach Veranstaltung wird nach Absprache erst bei der Integration mit
US01/US02 ergänzt. Im aktuellen Datenmodell existiert noch keine Veranstaltung.
US04 erfüllt daher das Kriterium „Fragen gehören zur richtigen Veranstaltung“
noch nicht.

Manuell testen:

1. Das Backend starten und die Seite öffnen. Ohne gespeicherte Fragen erscheint
   „No questions yet. Be the first to ask!“.
2. Eine Frage absenden. Sie erscheint automatisch in der Liste, zusätzlich zur
   Bestätigung von US03.
3. Dieselbe Seite in einem zweiten Tab öffnen und dort eine weitere Frage
   absenden. Im ersten Tab **Refresh** drücken: Beide Fragen erscheinen, ohne
   Duplikate. Alternativ die Seite neu laden.
4. Das Backend stoppen und **Refresh** drücken. Die Fehlermeldung erscheint,
   bisher angezeigte Fragen bleiben sichtbar.

Die zusätzlichen Tests prüfen die leere Liste, gespeicherte Fragen und die
Abfolge POST → GET über HTTP mit H2 sowie Laden, Aktualisieren, Fehlerbehandlung
und die sichere Textdarstellung im Frontend. Es gibt kein automatisches Polling;
Fragen anderer Teilnehmer erscheinen nach **Refresh** oder einem Neuladen.
