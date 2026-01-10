package server_client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for making HTTP API calls
 * Handles GET requests and JSON parsing
 */
public class ApiClient {
    private final ObjectMapper objectMapper;

    public ApiClient() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Make HTTP GET request to OpenWeatherMap API
     * Handles authentication errors and common HTTP errors
     *
     * @param urlString Full URL with parameters
     * @return JSON response as JsonNode or null on error
     */
    public JsonNode makeApiCall(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                // Success - read response
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return objectMapper.readTree(response.toString());

            } else if (responseCode == 404) {
                System.err.println("API Error 404: Resource not found");
                return null;

            } else if (responseCode == 401) {
                System.err.println("API Error 401: Invalid API Key");
                throw new Exception("Invalid API Key");

            } else {
                System.err.println("API Error: HTTP " + responseCode);
                return null;
            }

        } finally {
            connection.disconnect();
        }
    }

    /**
     * Make HTTP GET request to Open-Meteo API
     * No authentication needed for Open-Meteo
     *
     * @param urlString Full URL with parameters
     * @return JSON response as JsonNode or null on error
     */
    public JsonNode makeOpenMeteoCall(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                // Success - read response
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return objectMapper.readTree(response.toString());

            } else {
                System.err.println("Open-Meteo API Error: HTTP " + responseCode);
                return null;
            }

        } finally {
            connection.disconnect();
        }
    }
}

