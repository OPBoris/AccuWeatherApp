package server_client.services;

import com.fasterxml.jackson.databind.JsonNode;
import server_client.ApiClient;

import java.util.List;


public class WeatherService {

    private final ApiClient apiClient;

    private final GeocodingService geocodingService;
    private final CurrentWeatherService currentWeatherService;
    private final ForecastService forecastService;
    private final HistoryService historyService;
    private final OfflineWeatherService offlineService;
    private final FavoritesService favoritesService;

    public WeatherService() {
        this.apiClient = new ApiClient();
        this.geocodingService = new GeocodingService(apiClient);
        this.currentWeatherService = new CurrentWeatherService(apiClient);
        this.forecastService = new ForecastService(apiClient);
        this.historyService = new HistoryService(apiClient);
        this.offlineService = new OfflineWeatherService(apiClient);
        this.favoritesService = new FavoritesService();
    }

    public String getCurrentWeather(double lat, double lon, String unit,
                                    boolean showHumidity, boolean showWind, boolean showFeelsLike) {
        return currentWeatherService.getCurrentWeather(lat, lon, unit, showHumidity, showWind, showFeelsLike);
    }


    public String getWeatherByCity(String cityName, String unit, String username, boolean showHumidity, boolean showWind, boolean showFeelsLike) {
        try {
            JsonNode geoData = geocodingService.getCoordinates(cityName);
            if (geoData == null) {
                return "ERROR: City not found. Please check name.";
            }

            double lat = geoData.get("lat").asDouble();
            double lon = geoData.get("lon").asDouble();
            String cityNameReal = geoData.get("name").asText();
            String country = geoData.has("country") ? geoData.get("country").asText() : "";


            String weather = getCurrentWeather(lat, lon, unit, showHumidity, showWind, showFeelsLike);


            if (username != null && !username.isEmpty()) {
                historyService.saveToHistory(cityNameReal, username);
            }

            return "=== WEATHER FOR " + cityNameReal.toUpperCase() + ", " + country + " ===\n\n" + weather;

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String getForecastByCity(String cityName, String unit,
                                    boolean showFeelsLike, boolean showHumidity, boolean showWind) {
        try {
            JsonNode geoData = geocodingService.getCoordinates(cityName);
            if (geoData == null) {
                return "ERROR: City not found. Please check name.";
            }

            double lat = geoData.get("lat").asDouble();
            double lon = geoData.get("lon").asDouble();

            return forecastService.getForecast(lat, lon, unit, showFeelsLike, showHumidity, showWind);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }


    public String getHistoricalWeatherByCity(String cityName, String unit) {
        try {
            JsonNode geoData = geocodingService.getCoordinates(cityName);
            if (geoData == null) {
                return "ERROR: City not found. Please check name.";
            }

            double lat = geoData.get("lat").asDouble();
            double lon = geoData.get("lon").asDouble();

            return historyService.getHistoricalWeather(lat, lon, unit);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String getRecentCities(String username) {
        return historyService.getRecentCities(username);
    }

    public String exportHistoricalDataToCSVByCity(String cityName, String unit, String username) {
        try {
            JsonNode geoData = geocodingService.getCoordinates(cityName);
            if (geoData == null) {
                return "ERROR: City not found. Cannot export data.";
            }

            double lat = geoData.get("lat").asDouble();
            double lon = geoData.get("lon").asDouble();
            String cityNameReal = geoData.get("name").asText();

            return historyService.exportHistoricalDataToCSV(cityNameReal, lat, lon, unit, username);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public synchronized boolean addFavorite(String city, String username) {
        return favoritesService.addFavorite(city, username);
    }

    public synchronized boolean removeFavorite(String city, String username) {
        return favoritesService.removeFavorite(city, username);
    }

    public String getFavorites(String username) {
        return favoritesService.getFavorites(username);
    }

    public boolean isFavorite(String city, String username) {
        return favoritesService.isFavorite(city, username);
    }

    public String saveOfflineData(String cityName, String unit, String username) {
        try {
            JsonNode geoData = geocodingService.getCoordinates(cityName);
            if (geoData == null) {
                return "ERROR: City not found. Cannot save offline data.";
            }

            double lat = geoData.get("lat").asDouble();
            double lon = geoData.get("lon").asDouble();
            String cityNameReal = geoData.get("name").asText();
            String country = geoData.has("country") ? geoData.get("country").asText() : "";

            return offlineService.saveOfflineData(cityNameReal, country, lat, lon, unit, username);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String loadOfflineData(String username) {
        return offlineService.loadOfflineData(username);
    }

    public boolean isOnline() {
        return offlineService.isOnline();
    }

    public String getOfflineForecast(String username) {
        return offlineService.getOfflineForecast(username);
    }
}

