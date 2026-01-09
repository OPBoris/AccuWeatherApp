package server_client;


public class Config {

    public static String getApiKey() {
        String apiKey = System.getenv("OPENWEATHER_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API Key not found! Inspect .env file.");
        }
        return apiKey;
    }
}

