package server_client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * WeatherService - Facade class for all weather operations
 * Delegates all tasks to specialized service classes
 * Uses Open-Meteo API (FREE, no API key required)
 */
public class WeatherService {

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

    // =====================================================
    // GEOCODING
    // =====================================================

    public JsonNode getCoordinatesForCity(String city) throws Exception {
        return geocodingService.getCoordinates(city);
    }

    // =====================================================
    // CURRENT WEATHER
    // =====================================================

    public String getCurrentWeather(double lat, double lon, String unit,
                                    boolean showFeelsLike, boolean showHumidity, boolean showWind) {
        try {
            String tempUnit = unit.equalsIgnoreCase("F") ? "fahrenheit" : "celsius";
            String url = String.format(CURRENT_WEATHER_URL, lat, lon, tempUnit);
            JsonNode data = apiClient.makeOpenMeteoCall(url);
            return currentWeatherService.processCurrentWeather(data, unit, showFeelsLike, showHumidity, showWind);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String getWeatherByCity(String cityName, String unit, String username) {
        return getWeatherByCity(cityName, unit, username, true, true, true);
    }

    public String getWeatherByCity(String cityName, String unit, String username,
                                   boolean showFeelsLike, boolean showHumidity, boolean showWind) {
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
            String weather = getCurrentWeather(lat, lon, unit, showFeelsLike, showHumidity, showWind);

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
            return "ERROR: " + e.getMessage();
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
