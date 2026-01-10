package server_client;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service for geocoding operations
 * Converts city names to coordinates (latitude/longitude)
 * Uses Open-Meteo Geocoding API (FREE, no API key required)
 */
public class GeocodingService {
    private final ApiClient apiClient;

    // Open-Meteo Geocoding API URL (FREE)
    private static final String GEOCODING_URL =
        "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json";

    public GeocodingService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Get coordinates for a city name
     *
     * @param city City name (e.g. "Berlin" or "Berlin,DE")
     * @return JsonNode with "latitude", "longitude", "name", "country" or null if not found
     * @throws IllegalArgumentException if city name is invalid
     */
    public JsonNode getCoordinates(String city) throws Exception {
        // Validate input: must have at least 3 characters
        if (city == null || city.trim().length() < 3) {
            throw new IllegalArgumentException("City name must have at least 3 characters");
        }

        // Validate input: only letters, spaces, hyphens, commas allowed
        if (!city.trim().matches("[a-zA-ZäöüÄÖÜß\\s\\-,]+")) {
            throw new IllegalArgumentException("City name can only contain letters, spaces, hyphens, and commas");
        }

        // Encode city name for URL
        String encodedCity = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);

        // Build URL and make API call
        String urlString = String.format(GEOCODING_URL, encodedCity);
        JsonNode response = apiClient.makeOpenMeteoCall(urlString);

        // Open-Meteo returns: {"results": [{latitude, longitude, name, country, ...}]}
        if (response != null && response.has("results") && response.get("results").isArray()
            && response.get("results").size() > 0) {
            JsonNode firstResult = response.get("results").get(0);

            // Rename fields to match old format (lat/lon instead of latitude/longitude)
            // This ensures backward compatibility
            com.fasterxml.jackson.databind.node.ObjectNode result =
                (com.fasterxml.jackson.databind.node.ObjectNode) firstResult;

            if (firstResult.has("latitude") && firstResult.has("longitude")) {
                result.put("lat", firstResult.get("latitude").asDouble());
                result.put("lon", firstResult.get("longitude").asDouble());
            }

            return result;
        }

        return null;
    }

    /**
     * Validate city name format
     *
     * @param city City name to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidCityName(String city) {
        if (city == null || city.trim().length() < 3) {
            return false;
        }
        return city.trim().matches("[a-zA-ZäöüÄÖÜß\\s\\-,]+");
    }
}

