package server_client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class WeatherService {
    private static final int MAX_HISTORY_ENTRIES = 10;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    // CSV Database file path
    private static final String DB_FOLDER = "src/main/DB";
    private static final String HISTORY_CSV = DB_FOLDER + "/search_history.csv";

    // Geocoding API: City name to coordinates
    private static final String GEOCODING_URL = "http://api.openweathermap.org/geo/1.0/direct?q=%s&limit=1&appid=%s";
    // Current Weather API 2.5 (FREE)
    private static final String CURRENT_WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric";
    // 5-day Forecast API (FREE)
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast?lat=%s&lon=%s&appid=%s&units=metric";

    public WeatherService() {
        this.apiKey = Config.getApiKey();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Geocoding: Get coordinates for a city
     * @param city City name (optional: ",Country" e.g. "Berlin,DE")
     * @return JsonNode with "lat" and "lon" or null
     */
    public JsonNode getCoordinatesForCity(String city) throws Exception {
        String encodedCity = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);
        String urlString = String.format(GEOCODING_URL, encodedCity, apiKey);
        JsonNode results = makeApiCall(urlString);
        if (results != null && results.isArray() && results.size() > 0) {
            return results.get(0);
        }
        return null;
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
            String urlString = String.format(Locale.US, CURRENT_WEATHER_URL, lat, lon, apiKey);
            JsonNode data = makeApiCall(urlString);
            if (data == null) {
                return "ERROR: Unable to fetch weather data.";
            }

            StringBuilder sb = new StringBuilder();

            // Current weather
            double temp = data.get("main").get("temp").asDouble();
            double feelsLike = data.get("main").get("feels_like").asDouble();
            int humidity = data.get("main").get("humidity").asInt();
            double windSpeed = data.get("wind").get("speed").asDouble();

            if (unit.equalsIgnoreCase("F")) {
                temp = celsiusToFahrenheit(temp);
                feelsLike = celsiusToFahrenheit(feelsLike);
            }

            String description = "";
            if (data.has("weather") && data.get("weather").isArray() && data.get("weather").size() > 0) {
                description = data.get("weather").get(0).get("description").asText();
            }

            sb.append(String.format("CURRENT WEATHER:\n"));
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

            return sb.toString();
        } catch (Exception e) {
            System.err.println("Error fetching current weather: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Get 5-day forecast using FREE API 2.5
     * @param lat Latitude
     * @param lon Longitude
     * @param unit "C" or "F"
     * @return Forecast data as String
     */
    public String getForecast(double lat, double lon, String unit) {
        try {
            String urlString = String.format(Locale.US, FORECAST_URL, lat, lon, apiKey);
            JsonNode data = makeApiCall(urlString);
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
    }

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
            // Step 1: Get coordinates for the city
            System.out.println("Looking for coordinates: " + cityName);
            JsonNode geoData = getCoordinatesForCity(cityName);

            if (geoData == null) {
                return "ERROR: City not found. Please check name.";
            }

            double lat = geoData.get("lat").asDouble();
            double lon = geoData.get("lon").asDouble();
            String cityNameReal = geoData.get("name").asText();
            String country = geoData.has("country") ? geoData.get("country").asText() : "";

            System.out.println("City found: " + cityNameReal + ", " + country + " (Lat: " + lat + ", Lon: " + lon + ")");

            // Step 2: Get current weather + forecast
            String currentWeather = getCurrentWeather(lat, lon, unit, showHumidity, showWind, showFeelsLike);
            String forecast = getForecast(lat, lon, unit);

            // Step 3: Save city to history
            if (username != null && !username.isEmpty()) {
                saveToHistory(cityNameReal, username);
            }

            // Done! Return weather data
            return "=== WEATHER FOR " + cityNameReal.toUpperCase() + ", " + country + " ===\n\n" + currentWeather + forecast;

        } catch (Exception e) {
            System.err.println("Error fetching weather data: " + e.getMessage());
            return "ERROR: Can't fetch weather data. Error: " + e.getMessage();
        }
    }

    /**
     * Execute HTTP GET request and parse JSON response
     */
    private JsonNode makeApiCall(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
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

    private double celsiusToFahrenheit(double celsius) {
        return (celsius * 9.0 / 5.0) + 32.0;
    }

    // CSV-based History Database

    /**
     * Save search to CSV database
     * CSV Format: timestamp,username,city
     */
    public void saveToHistory(String city, String username) {
        try {
            // Create DB folder if it doesn't exist
            File dbFolder = new File(DB_FOLDER);
            if (!dbFolder.exists()) {
                dbFolder.mkdirs();
            }

            // Create CSV file with header if it doesn't exist
            File csvFile = new File(HISTORY_CSV);
            boolean isNewFile = !csvFile.exists();

            // Append to CSV file
            try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile, true))) {
                // Write header if new file
                if (isNewFile) {
                    writer.println("timestamp,username,city");
                }

                // Write data: timestamp,username,city
                String timestamp = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                );
                writer.println(String.format("%s,%s,%s", timestamp, username, city.trim()));
            }

        } catch (IOException e) {
            System.err.println("Error while writing to CSV database: " + e.getMessage());
        }
    }

    /**
     * Get recent searches for a user from CSV database
     * @param username Username to filter by
     * @return Comma-separated list of recent cities
     */
    public String getRecentCities(String username) {
        File csvFile = new File(HISTORY_CSV);
        if (!csvFile.exists()) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
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

    public void saveUserSettings(String username, boolean showHumidity, boolean showWind, boolean showFeelsLike,
                                 String unit, String standardCity) {
        if (username == null || username.isEmpty() || username.equalsIgnoreCase("Guest")) return;


        String filename = DB_FOLDER + "/settings_" + username + ".csv";
        try {
            File dbFolder = new File(DB_FOLDER);

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.println("showHumidity,showWind,showFeelsLike,unit,standardCity");
                writer.println(showHumidity + "," + showWind + "," + showFeelsLike + "," + unit+ "," + standardCity);
            }
        } catch (IOException e) {
            System.err.println("Error saving user settings: " + e.getMessage());
        }
    }

    public void setStandardCity(String username, String city) {
        if (username == null || username.isEmpty() || username.equalsIgnoreCase("Guest")) return;

        String filename = DB_FOLDER + "/settings_" + username + ".csv";
        try {
            File file = new File(filename);
            String[] currentSettings = loadUserSettings(username);

            try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
                writer.println("showHumidity,showWind,showFeelsLike,unit,standardCity");
                writer.println(currentSettings[0] + "," + currentSettings[1] + "," + currentSettings[2] + "," + currentSettings[3] + "," + city);
            }
        } catch (IOException e) {
            System.err.println("Error saving standard city: " + e.getMessage());
        }
    }

    public String getStandardCity(String username) {
        if (username == null || username.isEmpty()) return "";
        String[] settings = loadUserSettings(username);
        if (settings.length > 4 && settings[4] != null) {
            return settings[4];
        } else {
            return "";
        }
    }

    public String[] loadUserSettings(String username) {
        String[] settings = {"false", "false", "false" , "C", ""};

        if (username == null || username.isEmpty()) return settings;

        String filename = DB_FOLDER + "/settings_" + username + ".csv";
        File file = new File(filename);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                reader.readLine();
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        settings[0] = parts[0];
                        settings[1] = parts[1];
                        settings[2] = parts[2];
                        settings[3] = parts[3];
                    }
                    if (parts.length >= 5) {
                        settings[4] = parts[4];
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading settings for " + username + ": " + e.getMessage());
            }
        }
        return settings;
    }
}
