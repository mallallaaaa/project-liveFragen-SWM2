#!/bin/bash
set -euo pipefail

REPO="mallallaaaa/project-liveFragen-SWM2"

# Bereits vorhandene Issues überprüfen
existing=$(gh issue list --repo "$REPO" --state all --limit 1000 --json title --jq '.[].title')

while IFS='|' read -r id title priority points story; do

  # Doppelte Issues vermeiden
  if grep -Eq "^${id} - " <<< "$existing"; then
    echo "$id existiert bereits. Überspringe."
    continue
  fi

  echo "Erstelle $id ..."

  BODY="## User Story

$story

## Priorisierung

- **Priorität:** $priority
- **Aufwand:** $points Story Points"

  gh issue create \
    --repo "$REPO" \
    --title "$id - $title" \
    --body "$BODY"

done <<'STORIES'
US01|Veranstaltung erstellen|Must|3|Als Präsentierender möchte ich eine Veranstaltung erstellen können, damit Teilnehmer Fragen zu meiner Veranstaltung stellen können.
US02|Veranstaltung beitreten|Must|2|Als Teilnehmer möchte ich über einen Link einer Veranstaltung beitreten können, damit ich ohne Registrierung teilnehmen kann.
US03|Fragen einreichen|Must|3|Als Teilnehmer möchte ich während einer Veranstaltung Fragen einreichen können, damit ich Unklarheiten direkt ansprechen kann.
US04|Fragen anzeigen|Must|2|Als Teilnehmer möchte ich die Fragen anderer Teilnehmer sehen können, damit ich weiß, welche Themen bereits angesprochen wurden.
US05|Für Fragen abstimmen|Must|3|Als Teilnehmer möchte ich für Fragen anderer Teilnehmer abstimmen können, damit wichtige Fragen mehr Aufmerksamkeit erhalten.
US06|Fragen sortieren|Must|3|Als Präsentierender möchte ich alle eingereichten Fragen nach Stimmen sortiert sehen können, damit ich die wichtigsten Fragen zuerst beantworten kann.
US07|Fragen als beantwortet markieren|Must|3|Als Präsentierender möchte ich Fragen als beantwortet markieren können, damit ich den Überblick über offene Fragen behalte.
US08|Beantwortete Fragen anzeigen|Should|2|Als Teilnehmer möchte ich sehen können, welche Fragen bereits beantwortet wurden, damit ich den aktuellen Stand kenne.
US09|Fragen moderieren|Should|3|Als Präsentierender möchte ich unangemessene Fragen ausblenden können, damit die Diskussion sachlich bleibt.
US10|Diagramm anzeigen|Should|5|Als Präsentierender möchte ich ein Diagramm der beliebtesten Fragen sehen können, damit ich die Interessen des Publikums schnell erkenne.
US11|KI-Kategorisierung|Could|8|Als Präsentierender möchte ich Fragen automatisch durch KI kategorisieren lassen, damit ich Themen schneller erkennen kann.
US12|KI-Gruppierung|Could|8|Als Präsentierender möchte ich ähnliche Fragen durch KI gruppieren lassen, damit doppelte oder ähnliche Fragen gemeinsam behandelt werden können.
US13|KI-Priorisierung|Could|8|Als Präsentierender möchte ich Fragen durch KI priorisieren lassen, damit besonders relevante Fragen hervorgehoben werden.
US14|KI-Ergebnisse korrigieren|Could|5|Als Präsentierender möchte ich die KI-Kategorisierung, Gruppierung und Priorisierung manuell korrigieren können, damit ich die Kontrolle über die Ergebnisse behalte.
US15|Veranstaltung beenden|Should|3|Als Präsentierender möchte ich eine Veranstaltung beenden können, damit keine weiteren Fragen oder Abstimmungen möglich sind.
STORIES

echo "Fertig! Alle User Stories wurden überprüft."
