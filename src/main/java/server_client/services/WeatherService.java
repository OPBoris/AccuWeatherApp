package server_client.services;

import server_client.ApiClient;


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
                                    boolean showHumidity, boolean showWind, boolean showFeelsLike) {
        return currentWeatherService.getCurrentWeather(lat, lon, unit, showHumidity, showWind, showFeelsLike);
    }


    public String getWeatherByCity(String cityName, String unit, String username, boolean showHumidity, boolean showWind, boolean showFeelsLike) {
        try {
            String geoData = geocodingService.getCoordinates(cityName);
            if (geoData == null) {
                return "ERROR: City not found. Please check name.";
            }

            String[] parts = geoData.split("\\|");
            if (parts.length < 2) {
                return "ERROR: Invalid geocoding data.";
            }

            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);
            String cityNameReal = parts.length > 2 ? parts[2] : cityName;
            String country = parts.length > 3 ? parts[3] : "";

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
            String geoData = geocodingService.getCoordinates(cityName);
            if (geoData == null) {
                return "ERROR: City not found. Please check name.";
            }

            String[] parts = geoData.split("\\|");
            if (parts.length < 2) {
                return "ERROR: Invalid geocoding data.";
            }

            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);

            return forecastService.getForecast(lat, lon, unit, showFeelsLike, showHumidity, showWind);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }


    public String getHistoricalWeatherByCity(String cityName, String unit) {
        try {
            String geoData = geocodingService.getCoordinates(cityName);
            if (geoData == null) {
                return "ERROR: City not found. Please check name.";
            }

            String[] parts = geoData.split("\\|");
            if (parts.length < 2) {
                return "ERROR: Invalid geocoding data.";
            }

            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);

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
            String geoData = geocodingService.getCoordinates(cityName);
            if (geoData == null) {
                return "ERROR: City not found. Cannot export data.";
            }

            String[] parts = geoData.split("\\|");
            if (parts.length < 2) {
                return "ERROR: Invalid geocoding data.";
            }

            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);
            String cityNameReal = parts.length > 2 ? parts[2] : cityName;

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
            String geoData = geocodingService.getCoordinates(cityName);
            if (geoData == null) {
                return "ERROR: City not found. Cannot save offline data.";
            }

            String[] parts = geoData.split("\\|");
            if (parts.length < 2) {
                return "ERROR: Invalid geocoding data.";
            }

            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);
            String cityNameReal = parts.length > 2 ? parts[2] : cityName;
            String country = parts.length > 3 ? parts[3] : "";

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


}

