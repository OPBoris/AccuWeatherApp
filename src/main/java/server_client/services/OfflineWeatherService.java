package server_client.services;

import server_client.ApiClient;
import server_client.ApiUrls;
import server_client.JsonParser;
import server_client.WeatherCodeDecoder;
import server_client.exceptions.WeatherAppException;

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
                                  String unit, String username) throws WeatherAppException {
        try {

            String tempUnit;
            if (unit.equalsIgnoreCase("F")) {
                tempUnit = "fahrenheit";
            } else {
                tempUnit = "celsius";
            }
            String currentUrl = String.format(ApiUrls.CURRENT_WEATHER, lat, lon, tempUnit);
            String currentData = apiClient.makeOpenMeteoCall(currentUrl);

            if (currentData == null) {
                return "ERROR: Unable to fetch weather data for offline cache.";
            }

            return saveOfflineDataInternal(cityNameReal, country, currentData, unit, username);

        } catch (WeatherAppException e) {
            throw e;
        } catch (Exception e) {
            throw new WeatherAppException("Error saving offline data.", e);
        }
    }


    private String saveOfflineDataInternal(String cityNameReal, String country, String jsonData,
                                           String unit, String username) throws WeatherAppException {
        try {

            File dbFolder = new File(DB_FOLDER);
            if (!dbFolder.exists() && !dbFolder.mkdirs()) {
                throw new WeatherAppException("Could not create database folder.");
            }


            String sanitizedUser = username.replaceAll("[^a-zA-Z0-9]", "_");
            String filename = "offline_cache_" + sanitizedUser + ".csv";
            File cacheFile = new File(DB_FOLDER, filename);


            deleteOldOfflineCache(cacheFile);

            if (jsonData == null) {
                throw new WeatherAppException("ERROR: Unable to fetch weather data for offline cache.");
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

                int currentStart = jsonData.indexOf("\"current\":");
                int objectStart = jsonData.indexOf("{", currentStart);
                int objectEnd = JsonParser.findMatchingBrace(jsonData, objectStart);
                String currentBlock = jsonData.substring(objectStart, objectEnd + 1);

                double temp = JsonParser.parseDoubleValue(currentBlock, "temperature_2m");
                double feelsLike = JsonParser.parseDoubleValue(currentBlock, "apparent_temperature");
                int humidity = JsonParser.parseIntValue(currentBlock, "relative_humidity_2m");
                double windSpeed = JsonParser.parseDoubleValue(currentBlock, "wind_speed_10m");
                int weatherCode = JsonParser.parseIntValue(currentBlock, "weather_code");

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
            throw new WeatherAppException("File error while saving.", e);
        }
    }


    private void deleteOldOfflineCache(File cacheFile) throws WeatherAppException {
        if (!cacheFile.exists()) {
            return;
        }

        String cachedDate = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# Date: ")) {
                    cachedDate = line.substring(8).trim();
                    break;
                }
            }
        } catch (IOException e) {
            throw new WeatherAppException("Error reading offline cache: " + e.getMessage(), e);
        }

        String today = java.time.LocalDate.now().toString();
        if (cachedDate != null && !cachedDate.equals(today)) {
            if (!cacheFile.delete()) {
                throw new WeatherAppException("Could not delete old cache file.");
            }
            System.out.println("Deleted old offline cache from: " + cachedDate);
        }
    }

    public String loadOfflineData(String username) throws WeatherAppException {
        try {

            String sanitizedUser = username.replaceAll("[^a-zA-Z0-9]", "_");
            String filename = "offline_cache_" + sanitizedUser + ".csv";
            File cacheFile = new File(DB_FOLDER, filename);


            if (!cacheFile.exists()) {
                throw new WeatherAppException("ERROR: No offline data available. Please download data first with internet connection.");
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

        } catch (WeatherAppException e) {
            throw e;
        } catch (Exception e) {
            throw new WeatherAppException("Error loading offline data.", e);
        }
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
