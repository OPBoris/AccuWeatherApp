package server_client.services;

import server_client.ApiClient;
import server_client.ApiUrls;
import server_client.JsonParser;

import java.net.URLEncoder;


public class GeocodingService {
    private final ApiClient apiClient;


    public GeocodingService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }


    public String getCoordinates(String city) throws Exception {

        if (city == null || city.trim().length() < 3) {
            throw new IllegalArgumentException("City name must have at least 3 characters");
        }


        if (!city.trim().matches("[a-zA-ZäöüÄÖÜß\\s\\-,]+")) {
            throw new IllegalArgumentException("City name can only contain letters, spaces, hyphens, and commas");
        }


        String encodedCity = URLEncoder.encode(city.trim());


        String urlString = String.format(ApiUrls.GEOCODING, encodedCity);
        String response = apiClient.makeOpenMeteoCall(urlString);


        if (response != null && response.contains("\"results\"")) {
            return parseGeocodingResponse(response);
        }

        return null;
    }

    private String parseGeocodingResponse(String json) {
        try {
            int resultsStart = json.indexOf("\"results\":");
            if (resultsStart == -1) return null;

            int arrayStart = json.indexOf("[", resultsStart);
            if (arrayStart == -1) return null;

            int firstObjectStart = json.indexOf("{", arrayStart);
            if (firstObjectStart == -1) return null;

            int firstObjectEnd = JsonParser.findMatchingBrace(json, firstObjectStart);
            if (firstObjectEnd == -1) return null;

            String firstResult = json.substring(firstObjectStart, firstObjectEnd + 1);

            String lat = JsonParser.extractValue(firstResult, "latitude");
            String lon = JsonParser.extractValue(firstResult, "longitude");
            String name = JsonParser.extractValue(firstResult, "name");
            String country = JsonParser.extractValue(firstResult, "country");

            if (lat != null && lon != null) {
                return lat + "|" + lon + "|" + (name != null ? name : "") + "|" + (country != null ? country : "");
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error parsing geocoding response: " + e.getMessage());
            return null;
        }
    }
}
