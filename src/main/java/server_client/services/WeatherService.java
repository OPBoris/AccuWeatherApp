package server_client.services;

import server_client.ApiClient;
import server_client.exceptions.WeatherAppException;


public class WeatherService {

    private final GeocodingService geocodingService;
    private final CurrentWeatherService currentWeatherService;
    private final ForecastService forecastService;
    private final HistoryService historyService;
    private final OfflineWeatherService offlineService;
    private final FavoritesService favoritesService;

    public WeatherService() {
        ApiClient apiClient = new ApiClient();
        this.geocodingService = new GeocodingService(apiClient);
        this.currentWeatherService = new CurrentWeatherService(apiClient);
        this.forecastService = new ForecastService(apiClient);
        this.historyService = new HistoryService(apiClient);
        this.offlineService = new OfflineWeatherService(apiClient);
        this.favoritesService = new FavoritesService();
    }

    public String getCurrentWeather(double lat, double lon, String unit,
                                    boolean showHumidity, boolean showWind, boolean showFeelsLike) throws WeatherAppException {
        return currentWeatherService.getCurrentWeather(lat, lon, unit, showHumidity, showWind, showFeelsLike);
    }


    public String getWeatherByCity(String cityName, String unit, String username, boolean showHumidity, boolean showWind, boolean showFeelsLike) throws WeatherAppException {
        String geoData = geocodingService.getCoordinates(cityName);
        if (geoData == null) {
            throw new WeatherAppException("City not found. Please check name.");
        }

        String[] parts = geoData.split("\\|");
        if (parts.length < 2) {
            throw new WeatherAppException("Invalid geocoding data.");
        }

        double lat;
        double lon;
        try {
            lat = Double.parseDouble(parts[0]);
            lon = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            throw new WeatherAppException("Invalid coordinate format.", e);
        }

        String cityNameReal;
        if (parts.length > 2) {
            cityNameReal = parts[2];
        } else {
            cityNameReal = cityName;
        }

        String country;
        if (parts.length > 3) {
            country = parts[3];
        } else {
            country = "";
        }

        String weather = getCurrentWeather(lat, lon, unit, showHumidity, showWind, showFeelsLike);

        if (username != null && !username.isEmpty()) {
            historyService.saveToHistory(cityNameReal, username);
        }

        return "=== WEATHER FOR " + cityNameReal.toUpperCase() + ", " + country + " ===\n\n" + weather;
    }

    public String getForecastByCity(String cityName, String unit,
                                    boolean showFeelsLike, boolean showHumidity, boolean showWind) throws WeatherAppException {
        String geoData = geocodingService.getCoordinates(cityName);
        if (geoData == null) {
            throw new WeatherAppException("City not found. Please check name.");
        }

        String[] parts = geoData.split("\\|");
        if (parts.length < 2) {
            throw new WeatherAppException("Invalid geocoding data.");
        }

        double lat;
        double lon;
        try {
            lat = Double.parseDouble(parts[0]);
            lon = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            throw new WeatherAppException("Invalid coordinate format.", e);
        }

        return forecastService.getForecast(lat, lon, unit, showFeelsLike, showHumidity, showWind);
    }


    public String getHistoricalWeatherByCity(String cityName, String unit) throws WeatherAppException {
        String geoData = geocodingService.getCoordinates(cityName);
        if (geoData == null) {
            throw new WeatherAppException("City not found. Please check name.");
        }

        String[] parts = geoData.split("\\|");
        if (parts.length < 2) {
            throw new WeatherAppException("Invalid geocoding data.");
        }

        double lat;
        double lon;
        try {
            lat = Double.parseDouble(parts[0]);
            lon = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            throw new WeatherAppException("Invalid coordinate format.", e);
        }

        return historyService.getHistoricalWeather(lat, lon, unit);
    }

    public String getRecentCities(String username) throws WeatherAppException {
        return historyService.getRecentCities(username);
    }

    public String exportHistoricalDataToCSVByCity(String cityName, String unit, String username) throws WeatherAppException {
        String geoData = geocodingService.getCoordinates(cityName);
        if (geoData == null) {
            throw new WeatherAppException("City not found. Cannot export data.");
        }

        String[] parts = geoData.split("\\|");
        if (parts.length < 2) {
            throw new WeatherAppException("Invalid geocoding data.");
        }

        double lat;
        double lon;
        try {
            lat = Double.parseDouble(parts[0]);
            lon = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            throw new WeatherAppException("Invalid coordinate format.", e);
        }

        String cityNameReal;
        if (parts.length > 2) {
            cityNameReal = parts[2];
        } else {
            cityNameReal = cityName;
        }

        return historyService.exportHistoricalDataToCSV(cityNameReal, lat, lon, unit, username);
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

    public String saveOfflineData(String cityName, String unit, String username) throws WeatherAppException {
        String geoData = geocodingService.getCoordinates(cityName);
        if (geoData == null) {
            throw new WeatherAppException("City not found. Cannot save offline data.");
        }

        String[] parts = geoData.split("\\|");
        if (parts.length < 2) {
            throw new WeatherAppException("Invalid geocoding data.");
        }

        double lat;
        double lon;
        try {
            lat = Double.parseDouble(parts[0]);
            lon = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            throw new WeatherAppException("Invalid coordinate format.", e);
        }

        String cityNameReal;
        if (parts.length > 2) {
            cityNameReal = parts[2];
        } else {
            cityNameReal = cityName;
        }

        String country;
        if (parts.length > 3) {
            country = parts[3];
        } else {
            country = "";
        }

        return offlineService.saveOfflineData(cityNameReal, country, lat, lon, unit, username);
    }

    public String loadOfflineData(String username) throws WeatherAppException {
        return offlineService.loadOfflineData(username);
    }

    public boolean isOnline() {
        return offlineService.isOnline();
    }


}

