package server_client.services;

import com.fasterxml.jackson.databind.JsonNode;
import server_client.ApiClient;
import server_client.ApiUrls;
import server_client.WeatherCodeDecoder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;


public class OfflineWeatherService {
    private static final String DB_FOLDER = "src/main/DB";

    private final ApiClient apiClient;

    public OfflineWeatherService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }


    public String saveOfflineData(String cityNameReal, String country, double lat, double lon,
                                  String unit, String username) {
        try {

            String tempUnit;
            if (unit.equalsIgnoreCase("F")) {
                tempUnit = "fahrenheit";
            } else {
                tempUnit = "celsius";
            }
            String currentUrl = String.format(ApiUrls.CURRENT_WEATHER, lat, lon, tempUnit);
            JsonNode currentData = apiClient.makeOpenMeteoCall(currentUrl);

            if (currentData == null) {
                return "ERROR: Unable to fetch weather data for offline cache.";
            }

            return saveOfflineDataInternal(cityNameReal, country, currentData, unit, username);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }


    private String saveOfflineDataInternal(String cityNameReal, String country, JsonNode currentData,
                                           String unit, String username) {
        try {

            File dbFolder = new File(DB_FOLDER);
            if (!dbFolder.exists()) {
                dbFolder.mkdirs();
            }


            String sanitizedUser = username.replaceAll("[^a-zA-Z0-9]", "_");
            String filename = "offline_cache_" + sanitizedUser + ".csv";
            File cacheFile = new File(DB_FOLDER, filename);


            deleteOldOfflineCache(cacheFile);

            if (currentData == null) {
                return "ERROR: Unable to fetch weather data for offline cache.";
            }


            String downloadTime = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );
            String today = java.time.LocalDate.now().toString();

            try (PrintWriter writer = new PrintWriter(new FileWriter(cacheFile))) {

                writer.println("# OFFLINE WEATHER CACHE");
                writer.println("# Downloaded: " + downloadTime);
                writer.println("# City: " + cityNameReal + ", " + country);
                writer.println("# Unit: " + unit);
                writer.println("# Date: " + today);
                writer.println("#");


                writer.println("[CURRENT]");

                JsonNode current = currentData.get("current");
                double temp = current.get("temperature_2m").asDouble();
                double feelsLike = current.get("apparent_temperature").asDouble();
                int humidity = current.get("relative_humidity_2m").asInt();
                double windSpeed = current.get("wind_speed_10m").asDouble();
                int weatherCode = current.get("weather_code").asInt();

                String weatherDesc = WeatherCodeDecoder.decode(weatherCode);

                writer.println("city=" + cityNameReal);
                writer.println("country=" + country);
                writer.println("unit=" + unit);
                writer.println("temp=" + String.format("%.1f", temp));
                writer.println("feels_like=" + String.format("%.1f", feelsLike));
                writer.println("humidity=" + humidity);
                writer.println("wind=" + String.format("%.1f", windSpeed));
                writer.println("weather=" + weatherDesc);
                writer.println("weather_code=" + weatherCode);
            }

            return "SUCCESS: Offline data saved!\nCity: " + cityNameReal + ", " + country +
                    "\nDownloaded at: " + downloadTime +
                    "\nFile: " + cacheFile.getName();

        } catch (Exception e) {
            System.err.println("Error saving offline data: " + e.getMessage());
            return "ERROR: Failed to save offline data: " + e.getMessage();
        }
    }


    private void deleteOldOfflineCache(File cacheFile) {
        if (!cacheFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
            String line;
            String cachedDate = null;


            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# Date: ")) {
                    cachedDate = line.substring(8).trim();
                    break;
                }
            }


            String today = java.time.LocalDate.now().toString();
            if (cachedDate != null && !cachedDate.equals(today)) {
                reader.close();
                cacheFile.delete();
                System.out.println("Deleted old offline cache from: " + cachedDate);
            }

        } catch (IOException e) {
            System.err.println("Error checking offline cache: " + e.getMessage());
        }
    }

    public String loadOfflineData(String username) {
        try {

            String sanitizedUser = username.replaceAll("[^a-zA-Z0-9]", "_");
            String filename = "offline_cache_" + sanitizedUser + ".csv";
            File cacheFile = new File(DB_FOLDER, filename);


            if (!cacheFile.exists()) {
                return "ERROR: No offline data available. Please download data first with internet connection.";
            }


            try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
                String line;
                String downloadTime = "";
                String cachedDate = "";
                String city = "";
                String country = "";
                String unit = "C";


                String temp = "";
                String feelsLike = "";
                String humidity = "";
                String wind = "";
                String weather = "";

                while ((line = reader.readLine()) != null) {

                    if (line.startsWith("# Downloaded: ")) {
                        downloadTime = line.substring(14).trim();
                    } else if (line.startsWith("# Date: ")) {
                        cachedDate = line.substring(8).trim();
                    } else if (line.startsWith("city=")) {
                        city = line.substring(5);
                    } else if (line.startsWith("country=")) {
                        country = line.substring(8);
                    } else if (line.startsWith("temp=")) {
                        temp = line.substring(5);
                    } else if (line.startsWith("feels_like=")) {
                        feelsLike = line.substring(11);
                    } else if (line.startsWith("humidity=")) {
                        humidity = line.substring(9);
                    } else if (line.startsWith("wind=")) {
                        wind = line.substring(5);
                    } else if (line.startsWith("weather=")) {
                        weather = line.substring(8);
                    } else if (line.startsWith("unit=")) {
                        unit = line.substring(5);
                    }
                }


                String today = java.time.LocalDate.now().toString();
                if (!cachedDate.equals(today)) {
                    return "ERROR: Offline data is outdated (from " + cachedDate + ").\n" +
                            "Please download new data when internet is available.";
                }


                StringBuilder result = new StringBuilder();
                result.append("=== OFFLINE WEATHER DATA ===\n");
                result.append("[Last updated: ").append(downloadTime).append("]\n\n");
                result.append("City: ").append(city);
                if (!country.isEmpty()) {
                    result.append(", ").append(country);
                }
                result.append("\n\n");
                result.append("CURRENT WEATHER:\n");
                result.append("Temperature: ").append(temp).append("°").append(unit).append("\n");
                result.append("Feels like: ").append(feelsLike).append("°").append(unit).append("\n");
                result.append("Weather: ").append(weather).append("\n");
                result.append("Humidity: ").append(humidity).append("%\n");
                result.append("Wind: ").append(wind).append(" km/h\n");


                return result.toString();
            }

        } catch (Exception e) {
            System.err.println("Error loading offline data: " + e.getMessage());
            return "ERROR: Failed to load offline data: " + e.getMessage();
        }
    }


    public String getOfflineForecast(String username) {
        return "ERROR: Forecast data is not available in offline mode. Only current weather data is cached.";
    }


    public boolean isOnline() {
        try {
            URL url = new URL("https://api.open-meteo.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return (responseCode >= 200 && responseCode < 400);
        } catch (Exception e) {
            return false;
        }
    }
}
