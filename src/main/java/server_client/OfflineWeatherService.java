package server_client;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Service for offline weather data storage and retrieval
 * Handles saving and loading weather data to/from CSV files
 */
public class OfflineWeatherService {
    private static final String DB_FOLDER = "src/main/DB";

    public OfflineWeatherService() {
    }

    /**
     * Save weather data for offline use
     * Stores current weather + hourly forecast for today in CSV file
     * Each user has a separate offline cache file
     *
     * @param cityNameReal City name from geocoding
     * @param country Country code
     * @param currentData Current weather JSON data
     * @param forecastData Forecast weather JSON data
     * @param unit Temperature unit (C or F)
     * @param username Username for separate cache files
     * @return Success or error message
     */
    public String saveOfflineData(String cityNameReal, String country, JsonNode currentData,
                                   JsonNode forecastData, String unit, String username) {
        try {
            // Create DB folder if it doesn't exist
            File dbFolder = new File(DB_FOLDER);
            if (!dbFolder.exists()) {
                dbFolder.mkdirs();
            }

            // Create offline cache file for this user
            String sanitizedUser = username.replaceAll("[^a-zA-Z0-9]", "_");
            String filename = "offline_cache_" + sanitizedUser + ".csv";
            File cacheFile = new File(DB_FOLDER, filename);

            // Delete old cache (older than today)
            deleteOldOfflineCache(cacheFile);

            if (currentData == null) {
                return "ERROR: Unable to fetch weather data for offline cache.";
            }

            // Save data to CSV
            String downloadTime = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );
            String today = java.time.LocalDate.now().toString();

            try (PrintWriter writer = new PrintWriter(new FileWriter(cacheFile))) {
                // Write metadata header
                writer.println("# OFFLINE WEATHER CACHE");
                writer.println("# Downloaded: " + downloadTime);
                writer.println("# City: " + cityNameReal + ", " + country);
                writer.println("# Unit: " + unit);
                writer.println("# Date: " + today);
                writer.println("#");

                // Write current weather section (Open-Meteo format)
                writer.println("[CURRENT]");

                JsonNode current = currentData.get("current");
                double temp = current.get("temperature_2m").asDouble();
                double feelsLike = current.get("apparent_temperature").asDouble();
                int humidity = current.get("relative_humidity_2m").asInt();
                double windSpeed = current.get("wind_speed_10m").asDouble();
                int weatherCode = current.get("weather_code").asInt();

                // Get min/max from daily forecast (first day = today)
                double tempMin = temp;
                double tempMax = temp;
                if (forecastData != null && forecastData.has("daily")) {
                    JsonNode daily = forecastData.get("daily");
                    if (daily.has("temperature_2m_min") && daily.get("temperature_2m_min").size() > 0) {
                        tempMin = daily.get("temperature_2m_min").get(0).asDouble();
                    }
                    if (daily.has("temperature_2m_max") && daily.get("temperature_2m_max").size() > 0) {
                        tempMax = daily.get("temperature_2m_max").get(0).asDouble();
                    }
                }

                // Decode weather code
                String weatherDesc = WeatherCodeDecoder.decode(weatherCode);

                writer.println("city=" + cityNameReal);
                writer.println("unit=" + unit);

                // Write daily forecast section (Open-Meteo format)
                writer.println("");
                writer.println("[FORECAST]");
                writer.println("date,temp_max,temp_min,weather,wind,rain_prob");

                if (forecastData != null && forecastData.has("daily")) {
                    JsonNode daily = forecastData.get("daily");
                    JsonNode times = daily.get("time");
                    JsonNode tempMaxArr = daily.get("temperature_2m_max");
                    JsonNode tempMinArr = daily.get("temperature_2m_min");
                    JsonNode weatherCodes = daily.get("weather_code");
                    JsonNode windSpeedArr = daily.get("wind_speed_10m_max");
                    JsonNode precipProb = daily.get("precipitation_probability_max");

                    int daysCount = Math.min(5, times.size());
                    for (int i = 0; i < daysCount; i++) {
                        String dateStr = times.get(i).asText();
                        double tMax = tempMaxArr.get(i).asDouble();
                        double tMin = tempMinArr.get(i).asDouble();
                        int wCode = weatherCodes.get(i).asInt();
                        double wSpeed = windSpeedArr.get(i).asDouble();
                        int rainProb = precipProb.get(i).asInt();

                        String weather = WeatherCodeDecoder.decode(wCode);

                        writer.println(String.format("%s,%.1f,%.1f,%s,%.1f,%d",
                            dateStr, tMax, tMin, weather, wSpeed, rainProb));
                    }
                }
            }

            return "SUCCESS: Offline data saved!\nCity: " + cityNameReal + ", " + country +
                   "\nDownloaded at: " + downloadTime +
                   "\nFile: " + cacheFile.getName();

        } catch (Exception e) {
            System.err.println("Error saving offline data: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: Failed to save offline data: " + e.getMessage();
        }
    }

    /**
     * Delete old offline cache files (older than today)
     */
    private void deleteOldOfflineCache(File cacheFile) {
        if (!cacheFile.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
            String line;
            String cachedDate = null;

            // Find the date in the cache
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("# Date: ")) {
                    cachedDate = line.substring(8).trim();
                    break;
                }
            }

            // If date is not today, delete the file
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

    /**
     * Load offline weather data from cache
     *
     * @param username Username to find cache file
     * @return Formatted weather data string or error message
     */
    public String loadOfflineData(String username) {
        try {
            // Find cache file for this user
            String sanitizedUser = username.replaceAll("[^a-zA-Z0-9]", "_");
            String filename = "offline_cache_" + sanitizedUser + ".csv";
            File cacheFile = new File(DB_FOLDER, filename);

            // Check if cache file exists
            if (!cacheFile.exists()) {
                return "ERROR: No offline data available. Please download data first with internet connection.";
            }

            // Read cache file
            try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
                String line;
                String downloadTime = "";
                String cachedDate = "";
                String city = "";
                String country = "";
                String unit = "C";

                // Current weather data
                String temp = "";
                String feelsLike = "";
                String tempMin = "";
                String tempMax = "";
                String humidity = "";
                String wind = "";
                String weather = "";
                String description = "";

                // Forecast data
                StringBuilder forecastBuilder = new StringBuilder();
                boolean inForecast = false;
                boolean isHeader = true;

                while ((line = reader.readLine()) != null) {
                    // Parse metadata
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
                    } else if (line.startsWith("temp_min=")) {
                        tempMin = line.substring(9);
                    } else if (line.startsWith("temp_max=")) {
                        tempMax = line.substring(9);
                    } else if (line.startsWith("humidity=")) {
                        humidity = line.substring(9);
                    } else if (line.startsWith("wind=")) {
                        wind = line.substring(5);
                    } else if (line.startsWith("weather=")) {
                        weather = line.substring(8);
                    } else if (line.startsWith("description=")) {
                        description = line.substring(12);
                    } else if (line.startsWith("unit=")) {
                        unit = line.substring(5);
                    } else if (line.equals("[FORECAST]")) {
                        inForecast = true;
                    } else if (inForecast && !line.isEmpty() && !line.startsWith("#")) {
                        if (isHeader) {
                            isHeader = false; // Skip header line
                        } else {
                            // Parse forecast line: date,temp_max,temp_min,weather,wind,rain_prob
                            String[] parts = line.split(",");
                            if (parts.length >= 6) {
                                forecastBuilder.append("  ").append(parts[0])
                                    .append(" - Max: ").append(parts[1]).append("°").append(unit)
                                    .append(" Min: ").append(parts[2]).append("°").append(unit)
                                    .append(" | ").append(parts[3])
                                    .append(" | Rain: ").append(parts[5]).append("%\n");
                            }
                        }
                    }
                }

                // Check if data is from today
                String today = java.time.LocalDate.now().toString();
                if (!cachedDate.equals(today)) {
                    return "ERROR: Offline data is outdated (from " + cachedDate + ").\n" +
                           "Please download new data when internet is available.";
                }

                // Build output string
                StringBuilder result = new StringBuilder();
                result.append("=== OFFLINE WEATHER DATA ===\n");
                result.append("[Last updated: ").append(downloadTime).append("]\n\n");
                result.append("City: ").append(city).append(", ").append(country).append("\n\n");
                result.append("CURRENT WEATHER:\n");
                result.append("Temperature: ").append(temp).append("°").append(unit).append("\n");
                result.append("Feels like: ").append(feelsLike).append("°").append(unit).append("\n");
                result.append("Range: ").append(tempMin).append("° - ").append(tempMax).append("°").append(unit).append("\n");
                result.append("Weather: ").append(weather).append(" (").append(description).append(")\n");
                result.append("Humidity: ").append(humidity).append("%\n");
                result.append("Wind: ").append(wind).append(" km/h\n");

                if (forecastBuilder.length() > 0) {
                    result.append("\n5-DAY FORECAST:\n");
                    result.append(forecastBuilder);
                }

                return result.toString();
            }

        } catch (Exception e) {
            System.err.println("Error loading offline data: " + e.getMessage());
            return "ERROR: Failed to load offline data: " + e.getMessage();
        }
    }

    /**
     * Get offline forecast data as separate day strings (for UI display)
     *
     * @param username Username to find cache file
     * @return Forecast string separated by "|||" or error message
     */
    public String getOfflineForecast(String username) {
        try {
            String sanitizedUser = username.replaceAll("[^a-zA-Z0-9]", "_");
            String filename = "offline_cache_" + sanitizedUser + ".csv";
            File cacheFile = new File(DB_FOLDER, filename);

            if (!cacheFile.exists()) {
                return "ERROR: No offline data available.";
            }

            // Read forecast data from cache
            StringBuilder result = new StringBuilder();
            boolean inForecast = false;
            boolean isHeader = true;
            String cachedDate = "";
            String unit = "C";

            try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("# Date: ")) {
                        cachedDate = line.substring(8).trim();
                    } else if (line.startsWith("unit=")) {
                        unit = line.substring(5);
                    } else if (line.equals("[FORECAST]")) {
                        inForecast = true;
                    } else if (inForecast && !line.isEmpty() && !line.startsWith("#")) {
                        if (isHeader) {
                            isHeader = false;
                        } else {
                            // Parse: date,temp_max,temp_min,weather,wind,rain_prob
                            String[] parts = line.split(",");
                            if (parts.length >= 6) {
                                if (result.length() > 0) {
                                    result.append("|||");
                                }
                                result.append(parts[0]).append("\n"); // date
                                result.append("Max: ").append(parts[1]).append("°").append(unit).append("\n");
                                result.append("Min: ").append(parts[2]).append("°").append(unit).append("\n");
                                result.append("Weather: ").append(parts[3]).append("\n");
                                result.append("Wind: ").append(parts[4]).append(" km/h\n");
                                result.append("Rain: ").append(parts[5]).append("%");
                            }
                        }
                    }
                }
            }

            // Check if data is from today
            String today = java.time.LocalDate.now().toString();
            if (!cachedDate.equals(today)) {
                return "ERROR: Offline data outdated (from " + cachedDate + ")";
            }

            if (result.length() == 0) {
                return "No forecast data available|||N/A|||N/A|||N/A|||N/A";
            }

            return result.toString();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Check if internet connection is available
     * @return true if online, false if offline
     */
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

    /**
     * Convert Celsius to Fahrenheit
     */
    private double celsiusToFahrenheit(double celsius) {
        return (celsius * 9.0 / 5.0) + 32.0;
    }
}
