package server_client.services;

import server_client.ApiClient;
import server_client.ApiUrls;
import server_client.JsonParser;
import server_client.exceptions.WeatherAppException;

import java.net.URLEncoder;


public class GeocodingService {
    private final ApiClient apiClient;


    public GeocodingService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }


    public String getCoordinates(String city) throws WeatherAppException {

        if (city == null || city.trim().length() < 3) {
            throw new WeatherAppException("City name must have at least 3 characters");
        }


        if (!city.trim().matches("[a-zA-ZäöüÄÖÜß\\s\\-,]+")) {
            throw new WeatherAppException("City name can only contain letters, spaces, hyphens, and commas");
        }

        try {
            String encodedCity = URLEncoder.encode(city.trim());


            String urlString = String.format(ApiUrls.GEOCODING, encodedCity);
            String response = apiClient.makeOpenMeteoCall(urlString);


            if (response == null || !response.contains("\"results\"")) {
                throw new WeatherAppException("City '" + city + "' has not been found.");
            }

            return parseGeocodingResponse(response);

        } catch (WeatherAppException e) {
            throw e;
        } catch (Exception e) {
            throw new WeatherAppException("Error while searching the coordinates: " + e.getMessage());
        }
    }

    private String parseGeocodingResponse(String json) throws WeatherAppException {
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

            if (lat == null || lon == null) {
                throw new WeatherAppException("Invalid data received from the geocoding service.");
            }

            String nameResult;
            if (name != null) {
                nameResult = name;
            } else {
                nameResult = "";
            }

            String countryResult;
            if (country != null) {
                countryResult = country;
            } else {
                countryResult = "";
            }

            return lat + "|" + lon + "|" + nameResult + "|" + countryResult;

        } catch (Exception e) {
            throw new WeatherAppException("Fehler beim Lesen der Geodaten.", e);
        }
    }
}
