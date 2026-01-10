package server_client;
// ----Devlop----
import com.fasterxml.jackson.databind.JsonNode;
//----
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * WeatherService - Facade class for all weather operations
 * Delegates all tasks to specialized service classes
 * Uses Open-Meteo API (FREE, no API key required)
 */
public class WeatherService {
    private static final int MAX_HISTORY_ENTRIES = 10;
    /* ---Boris---
    private static final String DB_PATH = "src/main/DB/";

    public WeatherService() {
        try {
            Files.createDirectories(Paths.get(DB_PATH));
        } catch (IOException e) {
            System.out.println("Error: The system cannot create the DB folder at the specified path.: " + DB_PATH + " -> " + e.getMessage());
        }
    }*/

    // API Client
    private final ApiClient apiClient;

    // Specialized Services
    private final GeocodingService geocodingService;
    private final CurrentWeatherService currentWeatherService;
    private final ForecastService forecastService;
    private final HistoryService historyService;
    private final OfflineWeatherService offlineService;


    // Open-Meteo API URLs (FREE)
    private static final String CURRENT_WEATHER_URL =
        "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&temperature_unit=%s&timezone=auto";
    private static final String FORECAST_URL =
        "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max,wind_speed_10m_max&temperature_unit=%s&timezone=auto&forecast_days=5";
    private static final String OPEN_METEO_HISTORY_URL =
        "https://archive-api.open-meteo.com/v1/archive?latitude=%s&longitude=%s&start_date=%s&end_date=%s&daily=temperature_2m_max,temperature_2m_min,temperature_2m_mean,precipitation_sum,rain_sum,windspeed_10m_max,weathercode&timezone=auto";

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public WeatherService() {
        this.apiClient = new ApiClient();
        this.geocodingService = new GeocodingService(apiClient);
        this.currentWeatherService = new CurrentWeatherService();
        this.forecastService = new ForecastService();
        this.historyService = new HistoryService();
        this.offlineService = new OfflineWeatherService();
    }

    /**
     * Geocoding: Get coordinates for a city
     * @param city City name (optional: ",Country" e.g. "Berlin,DE")
     * @return JsonNode with "lat" and "lon" or null
     */
    public JsonNode getCoordinatesForCity(String city) throws Exception {
        return geocodingService.getCoordinates(city);
    }

    /**
     * Get current weather data using FREE API 2.5
     * @param lat Latitude
     * @param lon Longitude
     * @param unit "C" or "F"
     * @return Weather data as String
     */
    public String getCurrentWeather(double lat, double lon, String unit, boolean showHumidity, boolean showWind, boolean showFeelsLike) {
        try {
            String tempUnit = unit.equalsIgnoreCase("F") ? "fahrenheit" : "celsius";
            String url = String.format(CURRENT_WEATHER_URL, lat, lon, tempUnit);
            JsonNode data = apiClient.makeOpenMeteoCall(url);
            return currentWeatherService.processCurrentWeather(data, unit, showFeelsLike, showHumidity, showWind);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }/*  ----------in currentweather klasse-----------
            sb.append("CURRENT WEATHER:\n");
            sb.append(String.format("Temperature: %.1f %s\n", temp, unit));
            if (showFeelsLike) {
                sb.append(String.format("Feels like: %.1f %s\n", feelsLike, unit));
            }
            sb.append(String.format("Description: %s\n", description));
            if (showHumidity) {
                sb.append(String.format("Humidity: %d%%\n", humidity));
            }
            if (showWind) {
                sb.append(String.format("Wind: %.1f m/s\n", windSpeed));
            }
*/
    /**
     * Get 5-day forecast using FREE API 2.5
     * @param lat Latitude
     * @param lon Longitude
     * @param unit "C" or "F"
     * @return Forecast data as String
     */
    /*public String getForecast(double lat, double lon, String unit) {
        try {
            String urlString = String.format(FORECAST_URL, lat, lon);
            JsonNode data = apiClient.makeApiCall(urlString);
            if (data == null) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\nFORECAST (next 12 hours):\n");

            if (data.has("list")) {
                JsonNode list = data.get("list");
                // Show first 4 entries (12 hours, 3-hour intervals)
                for (int i = 0; i < Math.min(4, list.size()); i++) {
                    JsonNode forecast = list.get(i);
                    double temp = forecast.get("main").get("temp").asDouble();
                    if (unit.equalsIgnoreCase("F")) {
                        temp = celsiusToFahrenheit(temp);
                    }

                    String description = "";
                    if (forecast.has("weather") && forecast.get("weather").isArray() && forecast.get("weather").size() > 0) {
                        description = forecast.get("weather").get(0).get("main").asText();
                    }

                    sb.append(String.format("  +%dh: %.1f %s, %s\n", (i + 1) * 3, temp, unit, description));
                }
            }

            return sb.toString();
        } catch (Exception e) {
            System.err.println("Error fetching forecast: " + e.getMessage());
            return "";
        }
    }*/

    /**
     * MAIN METHOD: Get all weather data for a city
     *
     * @param cityName City name (e.g. "Berlin" or "Berlin,DE")
     * @param unit Temperature unit: "C" for Celsius or "F" for Fahrenheit
     * @param username Username for history (optional)
     * @return String with all weather data (current + forecast)
     */
    public String getWeatherByCity(String cityName, String unit, String username, boolean showHumidity, boolean showWind, boolean showFeelsLike) {
        try {
            // Get coordinates
            JsonNode geoData = geocodingService.getCoordinates(cityName);
            if (geoData == null) {
                return "ERROR: City not found. Please check name.";
            }

            double lat = geoData.get("lat").asDouble();
            double lon = geoData.get("lon").asDouble();
            String cityNameReal = geoData.get("name").asText();
            String country = geoData.has("country") ? geoData.get("country").asText() : "";

            // Get current weather
            String weather = getCurrentWeather(lat, lon, unit, showHumidity, showWind, showFeelsLike);

            // Save to history
            if (username != null && !username.isEmpty()) {
                historyService.saveToHistory(cityNameReal, username);
            }

            return "=== WEATHER FOR " + cityNameReal.toUpperCase() + ", " + country + " ===\n\n" + weather;

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // =====================================================
    // FORECAST
    // =====================================================

    public String getForecast(double lat, double lon, String unit,
                              boolean showFeelsLike, boolean showHumidity, boolean showWind) {
        try {
            String tempUnit = unit.equalsIgnoreCase("F") ? "fahrenheit" : "celsius";
            String url = String.format(FORECAST_URL, lat, lon, tempUnit);
            JsonNode data = apiClient.makeOpenMeteoCall(url);
            return forecastService.processForecast(data, unit, showFeelsLike, showHumidity, showWind);
        } catch (Exception e) {
            System.err.println("Error fetching weather data: " + e.getMessage());
            return "ERROR: Can't fetch weather data. Error: " + e.getMessage();
        }
    }

    public String getForecastByCity(String cityName, String unit) {
        return getForecastByCity(cityName, unit, true, true, true);
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

            return getForecast(lat, lon, unit, showFeelsLike, showHumidity, showWind);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // =====================================================
    // HISTORICAL WEATHER
    // =====================================================

    public String getHistoricalWeather(double lat, double lon, String unit) {
        try {
            // Calculate date range (last 5 days)
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate endDate = today.minusDays(1);
            java.time.LocalDate startDate = endDate.minusDays(4);

            // Make API call
            String url = String.format(OPEN_METEO_HISTORY_URL, lat, lon, startDate, endDate);
            JsonNode data = apiClient.makeOpenMeteoCall(url);

            if (data == null || !data.has("daily")) {
                return "ERROR: Unable to fetch historical data.";
            }

            return historyService.processHistoricalData(data.get("daily"), unit);

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

            return getHistoricalWeather(lat, lon, unit);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    // =====================================================
    // SEARCH HISTORY
    // =====================================================

    public void saveToHistory(String city, String username) {
        historyService.saveToHistory(city, username);
    }

    public String getRecentCities(String username) {
        return historyService.getRecentCities(username);
    }

    public String exportHistoricalDataToCSV(String cityName, double lat, double lon, String unit, String username) {
        try {
            // Calculate date range (last 30 days)
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate endDate = today.minusDays(1);
            java.time.LocalDate startDate = endDate.minusDays(29);

            // Make API call
            String url = String.format(OPEN_METEO_HISTORY_URL, lat, lon, startDate, endDate);
            JsonNode data = apiClient.makeOpenMeteoCall(url);

            return historyService.exportHistoricalDataToCSV(
                cityName, lat, lon, unit, username, data, WeatherCodeDecoder::decode
            );

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
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

            return exportHistoricalDataToCSV(cityNameReal, lat, lon, unit, username);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

/* ----------------Moritz---------------------
        public String getRecentCities(String username) {
            File csvFile = new File(HISTORY_CSV);
            if (!csvFile.exists()) {
                return "";
            }*/
    public synchronized boolean addFavorite(String city, String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        String cleanCity = formatCityName(city);

        List<String> currentFavs = readListFromFile(favFile);
        if (currentFavs.contains(cleanCity)) {
            return true;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(favFile, true))) {
            writer.println(cleanCity);
            return true;
        } catch (IOException e) {
            System.out.println("Error saving favorite: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean removeFavorite(String city, String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        String cleanCity = formatCityName(city);

        List<String> currentFavs = readListFromFile(favFile);
        if (!currentFavs.contains(cleanCity)) {
            return false;
        }

        currentFavs.remove(cleanCity);

        try (PrintWriter writer = new PrintWriter(new FileWriter(favFile, false))) {
            for (String fav : currentFavs) {
                writer.println(fav);
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error removing favorite: " + e.getMessage());
            return false;
        }
    }

    public String getFavorites(String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        List<String> favs = readListFromFile(favFile);
        return String.join(",", favs);
    }

/*
    public String getRecentCities(String username) {
        String historyFile = DB_PATH + username + "_history.txt";
        List<String> allLines = readListFromFile(historyFile);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {
                List<String> cities = reader.lines()
                        .skip(1) // Skip header line
                        .map(line -> line.split(","))
                        .filter(parts -> parts.length >= 3 && parts[1].equals(username)) // Filter by username
                        .map(parts -> parts[2]) // Get city name
                        .collect(Collectors.toList());

                // Reverse to show newest first
                java.util.Collections.reverse(cities);

                // Return distinct cities, limited to MAX_HISTORY_ENTRIES
                return cities.stream()
                        .distinct()
                        .limit(MAX_HISTORY_ENTRIES)
                        .collect(Collectors.joining(","));

            } catch (IOException e) {
                System.err.println("Error while reading CSV database: " + e.getMessage());
                return "ERROR: History unavailable.";
            }
        }

    */

    // =====================================================
    // OFFLINE MODE
    // =====================================================

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

            // Get current weather using Open-Meteo
            String tempUnit = unit.equalsIgnoreCase("F") ? "fahrenheit" : "celsius";
            String currentUrl = String.format(CURRENT_WEATHER_URL, lat, lon, tempUnit);
            JsonNode currentData = apiClient.makeOpenMeteoCall(currentUrl);

            if (currentData == null) {
                return "ERROR: Unable to fetch weather data for offline cache.";
            }

            // Get forecast using Open-Meteo
            String forecastUrl = String.format(FORECAST_URL, lat, lon, tempUnit);
            JsonNode forecastData = apiClient.makeOpenMeteoCall(forecastUrl);

            return offlineService.saveOfflineData(cityNameReal, country, currentData, forecastData, unit, username);

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
