# AccuWeatherApp
Gruppenprojekt im Rahmen des Faches objektorientierte Dienstentwicklung am Technikum Wien.

## Team
* Binder Moritz (ic24b002@technikum-wien.at)
* Jan Kerschbaum (ic24b005@technikum-wien.at)
* Opacic-Peric Boris (ic24b042@technikum-wien.at)

## Meilensteine (Wöchentliches Update)
- Projektdetails erstellt
- Git Repo aufsetzen
- Requirements gesammelt und definiert
- API Recherche und Auswahl
- API Anbindung implementiert
- Grundlegende UI erstellt
- Must Have Features implementiert
- Testing und Bugfixing
- Should Have Features implementiert
- Testing und Bugfixing
- Nice to Have Features implementiert
- Testing und Bugfixing
- Overkill Features implementiert
- Finales Testing und Optimierung
- Projektabschluss

## Projektbeschreibung
Eine JavaFX-basierte Wetteranwendung mit Client-Server-Architektur. Die Anwendung nutzt die Open-Meteo API für Wetterdaten und bietet Features wie aktuelle Wetterinformationen, Wettervorhersagen, Wetterverlauf, Favoriten-Verwaltung und einen Offline-Modus.

### Hauptfunktionen
- **Aktuelle Wetterdaten**: Temperatur, gefühlte Temperatur, Luftfeuchtigkeit, Windgeschwindigkeit
- **5-Tage-Wettervorhersage**: Detaillierte Vorhersagen für die nächsten Tage
- **30-Tage-Wetterverlauf**: Historische Wetterdaten mit CSV-Export
- **Favoriten-Verwaltung**: Speichern und schneller Zugriff auf bevorzugte Städte
- **Offline-Modus**: Zwischenspeicherung von Tageswetterdaten für Offline-Zugriff
- **Benutzer-Management**: Auswahl zwischen Gast- und registriertem Nutzer
- **Einstellungen**: Anpassbare Anzeige (Celsius/Fahrenheit, Standardstadt, sichtbare Parameter)

## Technische Umsetzung

### Architektur
- **Client-Server-Architektur** mit TCP-Socket-Kommunikation
- **Multithreading**: Server nutzt Multithreading für parallele Client-Anfragen
- **JavaFX GUI**: FXML-basierte Benutzeroberfläche mit asynchronen Task-Operationen

### OOP-Konzepte
- **Vererbung**: User-Klassenhierarchie (User → RegularUser, GuestUser)
- **Abstrakte Klassen**: User mit abstrakten Methoden
- **Method Overriding**: getRole(), toString()
- **Interfaces**: Callback-Interface für asynchrone Operationen

### Exception Handling
- Eigene Exception-Klasse: WeatherAppException
- Try-catch-Blöcke in allen Services
- Fehlerbehandlung für Netzwerk-, IO- und API-Fehler



### JSON-Verarbeitung
- Eigener JSON-Parser ohne externe Bibliotheken
- Manuelle Parsing-Logik für API-Responses

## Externe API
Open-Meteo API: https://open-meteo.com/en/docs
- Geocoding für Stadtsuche
- Aktuelle Wetterdaten
- Wettervorhersage
- Historische Wetterdaten

## Starten der Anwendung

### Voraussetzungen
- Java 17 oder höher
- JavaFX SDK

### Ausführung
1. **WeatherServer starten**: 
   - `WeatherServer.java` ausführen
   - Server läuft auf Port 8080

2. **Client starten**: 
   - `Client.java` ausführen
   - UI öffnet sich automatisch

3. **Anwendung nutzen**:
   - Nutzer wählen
   - Stadt eingeben und Wetterdaten abrufen
   - Features wie Favoriten, Verlauf und Einstellungen nutzen

## Projektstruktur
- `server_client/`: Server- und Client-Logik, Services
- `fhtw/accuweatherapp/`: UI-Komponenten und Handler
- `user/`: Benutzer-Klassen mit Vererbung
- `DB/`: Datenspeicherung (CSV, TXT)
