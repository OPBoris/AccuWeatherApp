package server_client.services;

import com.fasterxml.jackson.databind.JsonNode;
import server_client.ApiClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class GeocodingService {
    private final ApiClient apiClient;


    private static final String GEOCODING_URL =
            "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json";

    public GeocodingService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }


    public JsonNode getCoordinates(String city) throws Exception {

        if (city == null || city.trim().length() < 3) {
            throw new IllegalArgumentException("City name must have at least 3 characters");
        }


        if (!city.trim().matches("[a-zA-ZäöüÄÖÜß\\s\\-,]+")) {
            throw new IllegalArgumentException("City name can only contain letters, spaces, hyphens, and commas");
        }


        String encodedCity = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);


        String urlString = String.format(GEOCODING_URL, encodedCity);
        JsonNode response = apiClient.makeOpenMeteoCall(urlString);


        if (response != null && response.has("results") && response.get("results").isArray()
                && response.get("results").size() > 0) {
            JsonNode firstResult = response.get("results").get(0);


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

    public boolean isValidCityName(String city) {
        if (city == null || city.trim().length() < 3) {
            return false;
        }
        return city.trim().matches("[a-zA-ZäöüÄÖÜß\\s\\-,]+");
    }
}
