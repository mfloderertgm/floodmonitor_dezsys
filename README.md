# FloodMonitor – DEZSYS GK71
Maximilian Floderer 4AHIT

REST-Schnittstelle für ein Hochwasser-Frühwarnsystem mit Spring Boot. Mehrere Messstationen
erzeugen simulierte Messwerte, die als JSON oder XML abgerufen werden können. Ein Webclient
mit jQuery zeigt die Daten an. Die Aufgabenstellung steht in [TASK.md](TASK.md).

## Starten

Voraussetzung: Java 17 oder neuer. Gradle muss nicht installiert sein (Gradle Wrapper).

```bash
./gradlew bootRun
```

Unter Windows: `gradlew.bat bootRun`

| Was | Adresse |
|---|---|
| Webclient | http://localhost:8080/ |
| REST-API | http://localhost:8080/api/v1/stations |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI-Dokument | http://localhost:8080/v3/api-docs |

Tests ausführen:

```bash
./gradlew test
```

### Konfiguration

Die Einstellungen stehen in `src/main/resources/application.properties`.
Eine Vorlage liegt in [application.properties.example](application.properties.example).

| Einstellung | Bedeutung | Standard |
|---|---|---|
| `server.port` | Port des Servers | `8080` |
| `simulation.enabled` | Simulation ein/aus | `true` |
| `simulation.interval` | Abstand zwischen zwei Messungen in ms | `10000` |

Der Niederschlag ändert sich nur langsam. Bis eine Station von selbst in `WARNING` kommt,
kann es bei 10 Sekunden Abstand lange dauern. Zum Vorführen hilft `simulation.interval=500`.

## Architektur

```
src/main/java/com/example/floodmonitor/
├── FloodMonitorApplication.java    Startklasse
├── model/                          Datenklassen und Enums
├── repository/                     Speicher für Stationen und Messungen (im Arbeitsspeicher)
├── service/                        Geschäftslogik, Simulation, Warnstufe
├── controller/                     HTTP-Endpunkte
├── exception/                      Exceptions und zentrale Fehlerbehandlung
└── config/                         Titel und Beschreibung für OpenAPI

src/main/resources/static/          Webclient (index.html, style.css, app.js)
```

Ablauf einer Anfrage: Der **Controller** nimmt die HTTP-Anfrage an und gibt die Parameter
an den **Service** weiter. Der Service prüft die Parameter, holt die Daten aus dem
**Repository** und filtert sie. Wirft der Service eine Exception, macht der
`GlobalExceptionHandler` daraus eine einheitliche Fehlerantwort. Spring wandelt das
Ergebnis je nach `Accept`-Header in JSON oder XML um.

| Klasse | Aufgabe |
|---|---|
| `StationController`, `MeasurementController`, `AlertController` | Endpunkte, keine Logik |
| `StationService` | Stationen suchen und filtern |
| `MeasurementService` | aktuellster Messwert, Historie, Statistik |
| `AlertService` | aktuelle Warnungen |
| `SimulationService` | erzeugt regelmäßig neue Messwerte |
| `WarningLevelService` | berechnet die Warnstufe |
| `ParameterParser` | prüft Datum, Warnstufe und Zeitraum |
| `StationRepository`, `MeasurementRepository` | Datenhaltung |
| `GlobalExceptionHandler` | zentrale Fehlerbehandlung (`@RestControllerAdvice`) |

Die Daten liegen nur im Arbeitsspeicher. Nach einem Neustart beginnt die Simulation von vorne.
Pro Station werden höchstens 1000 Messungen aufgehoben.

## Datenmodell

**Station**

| Feld | Typ | Beschreibung |
|---|---|---|
| `id` | String | z.B. `ST-001` |
| `name` | String | Name der Station |
| `river` | String | Fluss |
| `location` | Location | `latitude` und `longitude` |
| `normalWaterLevel` | double | normaler Wasserstand in cm |
| `warningWaterLevel` | double | ab hier `WARNING` |
| `criticalWaterLevel` | double | ab hier `CRITICAL` |
| `isActive` | boolean | deaktivierte Stationen messen nicht |
| `status` | StationStatus | `ONLINE`, `MAINTENANCE`, `OFFLINE` |

**Measurement**

| Feld | Typ | Einheit |
|---|---|---|
| `stationId` | String | |
| `timestamp` | `java.time.Instant` | ISO-8601 (UTC) |
| `waterLevel` | double | cm |
| `flowRate` | double | m³/s |
| `rainfall` | double | mm/h |
| `temperature` | double | °C |
| `batteryLevel` | int | % |
| `status` | StationStatus | |
| `warningLevel` | WarningLevel | `NORMAL`, `WARNING`, `CRITICAL`, `UNKNOWN` |

Zusätzlich gibt es `Alert` (eine aktuelle Warnung), `Statistics` (Auswertung) und
`ErrorResponse` (Fehlerantwort).

**Vorhandene Stationen**

| ID | Name | Fluss | Normal / Warnung / Kritisch | Aktiv |
|---|---|---|---|---|
| ST-001 | Donaubrücke Linz | Donau | 250 / 400 / 550 cm | ja |
| ST-002 | Reichsbrücke Wien | Donau | 300 / 450 / 600 cm | ja |
| ST-003 | Staatsbrücke Salzburg | Salzach | 180 / 300 / 420 cm | ja |
| ST-004 | Innbrücke Innsbruck | Inn | 200 / 330 / 450 cm | nein |

## Simulationsregeln

Der `SimulationService` erzeugt alle `simulation.interval` Millisekunden für jede aktive
Station eine neue Messung. Jeder neue Wert baut auf der vorherigen Messung auf.

| Wert | Regel |
|---|---|
| Niederschlag | vorheriger Wert ± höchstens 2 mm/h, begrenzt auf 0 bis 30 |
| Wasserstand | vorheriger Wert + Regen × 0,5 − Abfluss ± höchstens 2 cm, nie unter 0 |
| Abfluss | 5 % vom Abstand zum Normalwasserstand. Je höher der Pegel, desto mehr fließt ab |
| Durchfluss | Wasserstand × 2,3. Höherer Wasserstand bedeutet höheren Durchfluss |
| Temperatur | vorheriger Wert ± höchstens 0,3 °C, begrenzt auf −10 bis 35 |
| Akku | sinkt pro Messung um 0 oder 1 %, nie unter 0 |
| Status | Akku unter 15 % → `MAINTENANCE`. Bei der nächsten Messung ist der Akku getauscht (100 %, `ONLINE`) |

- Die erste Messung startet beim Normalwasserstand mit 2 mm/h Regen, 15 °C und 100 % Akku.
- Bei gleichbleibendem Regen pendelt sich der Pegel bei *Normalwert + Regen × 10* ein.
  Bei 15 mm/h liegt ST-001 also bei 400 cm und damit auf der Warnschwelle.
- Der Wasserstand ändert sich zwischen zwei Messungen um weniger als 20 cm.
- Stationen mit `isActive = false` oder Status `OFFLINE` bekommen keine Messungen.

## Warnstufe

Die Warnstufe berechnet der Server im `WarningLevelService`. Der Client kann sie nicht vorgeben.

| Bedingung | Warnstufe |
|---|---|
| keine Messung vorhanden oder Wasserstand negativ | `UNKNOWN` |
| Wasserstand ≥ `criticalWaterLevel` | `CRITICAL` |
| Wasserstand ≥ `warningWaterLevel` | `WARNING` |
| sonst | `NORMAL` |

Niederschlag und Anstiegsgeschwindigkeit werden nicht berücksichtigt.
Die aktuelle Warnstufe einer Station ist die Warnstufe ihrer letzten Messung.

## REST-Schnittstelle

Basis-URL: `/api/v1`. Die vollständige Dokumentation steht in
[docs/api/openapi.json](docs/api/openapi.json) und in der Swagger UI.

| Methode | Endpunkt | Parameter |
|---|---|---|
| GET | `/stations` | `river`, `isActive`, `warningLevel` |
| GET | `/stations/{stationId}` | |
| GET | `/stations/{stationId}/measurements/latest` | |
| GET | `/stations/{stationId}/measurements` | `from`, `to`, `limit`, `warningLevel` |
| GET | `/stations/{stationId}/statistics` | `from`, `to` |
| GET | `/alerts` | `warningLevel`, `river` |

- `from` und `to` sind ISO-8601-Zeitpunkte, z.B. `2026-05-01T08:00:00Z`.
- `limit` muss zwischen 1 und 1000 liegen. Ohne Angabe gilt 100.
- Historische Messwerte kommen mit der neuesten Messung zuerst.
- `/alerts` liefert alle Stationen, deren letzte Messung `WARNING` oder `CRITICAL` ist.

**Authentifizierung:** keine. Die API hat nur lesende GET-Endpunkte, es gibt kein Login.

### Content Negotiation

| `Accept`-Header | Antwort |
|---|---|
| keiner oder `application/json` | JSON |
| `application/xml` | XML |
| etwas anderes, z.B. `text/plain` | `406 Not Acceptable` |

### Beispiele

JSON:

```bash
curl http://localhost:8080/api/v1/stations/ST-001/measurements/latest
```

```json
{
  "stationId": "ST-001",
  "timestamp": "2026-10-13T07:51:49Z",
  "waterLevel": 251.5,
  "flowRate": 578.5,
  "rainfall": 2.4,
  "temperature": 15.0,
  "batteryLevel": 100,
  "status": "ONLINE",
  "warningLevel": "NORMAL"
}
```

XML:

```bash
curl -H "Accept: application/xml" http://localhost:8080/api/v1/stations/ST-001/measurements/latest
```

```xml
<measurement>
  <stationId>ST-001</stationId>
  <timestamp>2026-10-13T07:51:49Z</timestamp>
  <waterLevel>251.5</waterLevel>
  <flowRate>578.5</flowRate>
  <rainfall>2.4</rainfall>
  <temperature>15.0</temperature>
  <batteryLevel>100</batteryLevel>
  <status>ONLINE</status>
  <warningLevel>NORMAL</warningLevel>
</measurement>
```

Listen sehen in XML so aus:

```bash
curl -H "Accept: application/xml" "http://localhost:8080/api/v1/stations?river=Salzach"
```

```xml
<List>
  <item>
    <id>ST-003</id>
    <name>Staatsbrücke Salzburg</name>
    <river>Salzach</river>
    <location>
      <latitude>47.8009</latitude>
      <longitude>13.0447</longitude>
    </location>
    <normalWaterLevel>180.0</normalWaterLevel>
    <warningWaterLevel>300.0</warningWaterLevel>
    <criticalWaterLevel>420.0</criticalWaterLevel>
    <status>ONLINE</status>
    <isActive>true</isActive>
  </item>
</List>
```

Weitere Anfragen:

```bash
curl "http://localhost:8080/api/v1/stations?isActive=true"
curl "http://localhost:8080/api/v1/stations?warningLevel=CRITICAL"
curl "http://localhost:8080/api/v1/stations/ST-001/measurements?from=2026-05-01T08:00:00Z&to=2026-05-01T12:00:00Z&limit=100"
curl "http://localhost:8080/api/v1/stations/ST-001/statistics"
curl "http://localhost:8080/api/v1/alerts?river=Donau"
```

### Fehler

Alle Fehler haben denselben Aufbau und kommen ebenfalls als JSON oder XML. Stacktraces
werden nie ausgegeben.

```bash
curl http://localhost:8080/api/v1/stations/ST-999
```

```json
{
  "timestamp": "2026-10-13T07:51:52Z",
  "status": 404,
  "error": "Not Found",
  "message": "Station ST-999 wurde nicht gefunden",
  "path": "/api/v1/stations/ST-999"
}
```

| Situation | Statuscode |
|---|---|
| unbekannte Station, unbekannter Pfad, Station hat noch keine Messung | 404 |
| `from` nach `to`, ungültiges Datum, ungültige Warnstufe | 400 |
| `limit` kleiner 1, größer 1000 oder keine Zahl | 400 |
| `isActive` ist weder `true` noch `false` | 400 |
| andere Methode als GET | 405 |
| nicht unterstütztes Format im `Accept`-Header | 406 |
| unerwarteter Fehler im Server | 500 |

Die Statuscodes 201, 204, 401, 403, 409 und 415 aus der Angabe kommen nicht vor, weil die
API nur GET-Endpunkte und keine Anmeldung hat.

## Webclient

Der Client liegt in `src/main/resources/static` und wird vom Server unter
http://localhost:8080/ ausgeliefert.

- lädt die Stationen in eine Auswahlliste
- zeigt den aktuellsten Messwert, die aktuellen Warnungen und die Statistik
- zeigt die historischen Messwerte in einer Tabelle
- filtert nach Zeitraum, Warnstufe und Limit
- schaltet zwischen JSON und XML um. Bei XML wird die Antwort mit jQuery ausgelesen
  (`$(xml).find("item")`) und in dieselben Objekte umgewandelt wie bei JSON
- aktualisiert die Daten alle 10 Sekunden (abschaltbar)
- zeigt „Lade Daten ..." und bei Fehlern die Meldung vom Server

Farben der Warnstufen: `NORMAL` grün, `WARNING` orange, `CRITICAL` rot, `UNKNOWN` grau.

![Webclient](docs/webclient%20screenshot.png)

## Tests

`./gradlew test` führt 31 Tests aus. In den Tests ist die Simulation ausgeschaltet
(`simulation.enabled=false`). Die Messwerte werden mit festen Zeitpunkten direkt ins
Repository geschrieben, dadurch ist das Ergebnis immer gleich.

| Geforderter Test | Testmethode |
|---|---|
| Alle Stationen als JSON | `FloodMonitorApiTest.allStationsAsJson` |
| Alle Stationen als XML | `FloodMonitorApiTest.allStationsAsXml` |
| Existierende Station | `FloodMonitorApiTest.existingStation` |
| Unbekannte Station | `FloodMonitorApiTest.unknownStation` |
| Aktuellster Messwert | `FloodMonitorApiTest.latestMeasurement` |
| Filterung nach Zeitraum | `FloodMonitorApiTest.filterByTimeRange` |
| Ungültiger Zeitraum | `FloodMonitorApiTest.fromAfterToIsRejected` |
| Warnstufe NORMAL | `WarningLevelServiceTest.belowWarningValueIsNormal` |
| Warnstufe WARNING | `WarningLevelServiceTest.fromWarningValueIsWarning` |
| Warnstufe CRITICAL | `WarningLevelServiceTest.fromCriticalValueIsCritical` |
| Nicht unterstütztes Format | `FloodMonitorApiTest.unsupportedFormat` |
| Maximales Limit | `FloodMonitorApiTest.maximumLimitIsAllowed`, `limitAboveMaximumIsRejected` |

`SimulationServiceTest` prüft die Simulationsregeln. Weil dort Zufall im Spiel ist, werden
nur Grenzen geprüft, die bei jedem Zufallswert gelten.

## Fragestellungen

**1. Was sind die Hauptmerkmale von JSON?**

JSON ist ein textbasiertes Datenformat, das von keiner Programmiersprache abhängt. Es besteht
aus Objekten (`{ "name": wert }`) und Arrays (`[ ... ]`). Als Werte gibt es Strings, Zahlen,
`true`/`false`, `null`, Objekte und Arrays. JSON ist kompakt und leicht zu lesen, kennt aber
keine Kommentare und keine Attribute.

Quellen: https://www.json.org/json-de.html, https://www.rfc-editor.org/rfc/rfc8259

**2. Was sind die Hauptmerkmale von XML?**

XML ist eine Auszeichnungssprache. Die Daten stehen zwischen öffnenden und schließenden Tags,
Elemente können Attribute haben und ineinander verschachtelt sein. Jedes Dokument hat genau
ein Wurzelelement und muss wohlgeformt sein. Mit einer DTD oder einem XML-Schema kann man
zusätzlich prüfen, ob ein Dokument gültig ist. XML kennt Kommentare und Namensräume, ist
aber länger als JSON. Alle Werte sind Text.

Quellen: https://www.w3.org/TR/xml/, https://www.w3schools.com/xml/xml_whatis.asp

**3. Wie unterscheidet die Applikation, ob JSON oder XML zurückgegeben wird?**

Über den `Accept`-Header der Anfrage (Content Negotiation). Die Controller geben mit
`produces` an, dass sie `application/json` und `application/xml` liefern können. Spring
vergleicht das mit dem `Accept`-Header und wählt den passenden `HttpMessageConverter`:
Jackson für JSON oder Jackson XML (`jackson-dataformat-xml`) für XML. Ohne Header wird JSON
genommen, weil es in `produces` zuerst steht. Passt kein Format, wirft Spring eine
`HttpMediaTypeNotAcceptableException` und der `GlobalExceptionHandler` antwortet mit 406.

Quellen: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/content-negotiation.html,
https://developer.mozilla.org/en-US/docs/Web/HTTP/Content_negotiation

**4. Was zeichnet ein REST Interface aus und warum wird es so oft verwendet?**

- Alles ist eine Ressource mit einer eigenen URL (z.B. `/stations/ST-001`).
- Die Ressourcen werden einheitlich über die HTTP-Methoden angesprochen.
- Der Server ist zustandslos: jede Anfrage enthält alles, was der Server braucht.
- Client und Server sind getrennt und tauschen Repräsentationen aus (JSON, XML).
- Antworten können gecacht werden.

REST wird oft verwendet, weil es auf HTTP aufbaut, das jede Programmiersprache und jeder
Browser beherrscht. Man braucht keine zusätzliche Software, die Schnittstelle ist leicht zu
verstehen und zu testen, und durch die Zustandslosigkeit lässt sich der Server gut skalieren.

Quellen: https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm, https://restfulapi.net/

**5. Was ist der Unterschied zwischen GET, POST, PUT und PATCH?**

| Methode | Zweck | verändert Daten | idempotent |
|---|---|---|---|
| GET | Ressource lesen | nein | ja |
| POST | neue Ressource anlegen oder Aktion auslösen | ja | nein |
| PUT | Ressource komplett ersetzen | ja | ja |
| PATCH | Ressource teilweise ändern | ja | nicht unbedingt |

Idempotent heißt: mehrmals dieselbe Anfrage hat dieselbe Wirkung wie einmal.

Quellen: https://www.rfc-editor.org/rfc/rfc9110#section-9.3, https://www.rfc-editor.org/rfc/rfc5789,
https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods

**6. Welche Stellen muss ich für eine neue Request-Methode oder einen neuen Pfad ändern?**

1. **Controller:** neue Methode mit `@GetMapping`, `@PostMapping`, … und dem Pfad
2. **Service:** neue Methode mit der Logik, die der Controller aufruft
3. **Repository:** nur, wenn neue Daten gespeichert oder anders gesucht werden
4. **Model:** nur, wenn eine neue Datenklasse zurückgegeben oder empfangen wird
5. **GlobalExceptionHandler:** nur, wenn eine neue Art von Fehler dazukommt
6. **Tests:** neuer Test in `FloodMonitorApiTest`
7. **Dokumentation:** `docs/api/openapi.json` neu exportieren (springdoc erkennt den Endpunkt selbst)
8. **Webclient:** `app.js`, falls der Client den Endpunkt verwenden soll

Quellen: eigener Quellcode, https://spring.io/guides/gs/rest-service/

**7. Kann man OpenAPI und Swagger vergleichen?**

Ja, aber sie sind nicht dasselbe. **OpenAPI** ist die Spezifikation, also das Format, in dem
eine REST-API beschrieben wird (als JSON oder YAML). **Swagger** ist eine Sammlung von
Werkzeugen der Firma SmartBear, die mit dieser Spezifikation arbeiten: Swagger UI zeigt die
Dokumentation an, Swagger Editor dient zum Schreiben, Swagger Codegen erzeugt Code daraus.
Verwechselt wird das oft, weil die Spezifikation bis Version 2.0 selbst „Swagger
Specification" hieß. 2015 wurde sie an die OpenAPI Initiative übergeben und heißt seit
Version 3.0 OpenAPI. In diesem Projekt erzeugt springdoc das OpenAPI-Dokument
(`/v3/api-docs`) und Swagger UI stellt es dar.

Quellen: https://swagger.io/blog/api-strategy/difference-between-swagger-and-openapi/,
https://www.openapis.org/what-is-openapi

**8. Wo liegt der Fehler im Code-Snippet?**

```java
@GetMapping("/stations/add")
public String createStation(@RequestParam String name, @RequestParam double warning) {
    stationService.save(new Station(name, warning));
    return "Station successfully created!";
}
```

Der Hauptfehler ist, dass eine Station mit **GET** angelegt wird. GET ist eine „sichere"
Methode und darf am Server nichts verändern. Browser, Proxies und Suchmaschinen dürfen
GET-Anfragen wiederholen, cachen oder vorab laden – dabei würde jedes Mal eine neue Station
entstehen. Richtig ist `POST`.

Daraus ergeben sich weitere Fehler:

- Der Pfad enthält das Verb `add`. Bei REST steht im Pfad die Ressource, die Aktion ergibt
  sich aus der Methode: `POST /stations`.
- Die Daten stehen als Query-Parameter in der URL statt im Request-Body (`@RequestBody`).
- Die Antwort ist `200 OK` mit einem Text. Richtig wäre `201 Created` mit der neuen Station.
- Die Eingaben werden nicht geprüft.

So wäre es richtig:

```java
@PostMapping("/stations")
public ResponseEntity<Station> createStation(@RequestBody Station station) {
    Station saved = stationService.save(station);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
}
```

Quellen: https://www.rfc-editor.org/rfc/rfc9110#section-9.2.1,
https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/POST

## Reflexion

- **OneDrive und Gradle:** Das Projekt liegt in einem OneDrive-Ordner. Gradle konnte den
  `build`-Ordner immer wieder nicht löschen („Unable to delete directory"). Lösung: in
  `build.gradle` wird der Build-Ordner ins Temp-Verzeichnis gelegt.
- **Gradle 8.7 und Java 22:** Nach einer Änderung an `build.gradle` kam „Unsupported class
  file major version 66". Gradle 8.7 unterstützt Java 22 nicht vollständig, deshalb wurde der
  Wrapper auf Gradle 8.8 gestellt.
- **`isActive` in JSON:** Jackson macht aus dem Getter `isActive()` das Feld `active`. Die
  Angabe verlangt `isActive`, das ging mit `@JsonProperty("isActive")`.
- **Fehlerantwort bei 406:** Wenn der Client ein Format verlangt, das es nicht gibt, kann
  Spring auch die Fehlermeldung nicht in diesem Format ausgeben. Die 406-Antwort wird
  deshalb fest als JSON geschickt.
- **Listen in XML:** Jackson XML nennt das Wurzelelement einer Liste `<List>` und die
  Einträge `<item>`. Der Webclient sucht deshalb nach `item`.
- **Zufall in Tests:** Der Simulator verwendet Zufallszahlen. Damit die Tests nicht zufällig
  fehlschlagen, ist die Simulation dort ausgeschaltet und es werden feste Messwerte verwendet.


Disclaimer: README wurde größtenteils von KI erstellt. 
Als Hilfe beim Projekt wurde Claude Code verwendet.

