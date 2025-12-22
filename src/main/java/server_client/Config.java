package server_client;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Einfache Klasse zum Laden der Konfiguration aus der .env Datei
 */
public class Config {
    private static final Dotenv dotenv;

    static {
        // Lade die .env Datei aus dem Projekt-Root
        dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();
    }

    /**
     * Gibt den OpenWeatherMap API Key zurück
     */
    public static String getApiKey() {
        String apiKey = dotenv.get("OPENWEATHER_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API Key not found! Inspect .env file.");
        }
        return apiKey;
    }
}

