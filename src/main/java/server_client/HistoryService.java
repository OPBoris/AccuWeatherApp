package server_client;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing search history and exporting historical weather data
 * Handles CSV-based history database and 30-day weather reports
 */
public class HistoryService {
    private static final int MAX_HISTORY_ENTRIES = 10;
    private static final String DB_FOLDER = "src/main/DB";
    private static final String HISTORY_CSV = DB_FOLDER + "/search_history.csv";

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

    /**
     * Export 30-day historical weather data to CSV file
     * Uses Open-Meteo Historical Weather API (FREE, no API key required)
     *
     * @param cityName City name
     * @param lat Latitude
     * @param lon Longitude
     * @param unit Temperature unit
     * @param username Username for filename
     * @param weatherData Historical weather JSON data from Open-Meteo
     * @param decodeFunction Function to decode WMO weather codes
     * @return Success message with file path or error message
     */
    public String exportHistoricalDataToCSV(String cityName, double lat, double lon, String unit,
                                            String username, JsonNode weatherData,
                                            WMOWeatherCodeDecoder decodeFunction) {
        try {
            // Create DB folder if it doesn't exist
            File dbFolder = new File(DB_FOLDER);
            if (!dbFolder.exists()) {
                dbFolder.mkdirs();
            }

            // Create filename: weather_history_<username>_<cityname>_<timestamp>.csv
            String sanitizedCity = cityName.replaceAll("[^a-zA-Z0-9]", "_");
            String sanitizedUser = username.replaceAll("[^a-zA-Z0-9]", "_");
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            );
            String filename = String.format("30_day_weather_history_%s_%s_%s.csv",
                sanitizedUser, sanitizedCity, timestamp);
            File csvFile = new File(DB_FOLDER, filename);

            if (weatherData == null || !weatherData.has("daily")) {
                return "ERROR: Unable to fetch historical data from Open-Meteo.";
            }

            JsonNode daily = weatherData.get("daily");
            JsonNode times = daily.get("time");
            JsonNode tempMax = daily.get("temperature_2m_max");
            JsonNode tempMin = daily.get("temperature_2m_min");
            JsonNode tempMean = daily.get("temperature_2m_mean");
            JsonNode precipitation = daily.get("precipitation_sum");
            JsonNode rain = daily.get("rain_sum");
            JsonNode windSpeed = daily.get("windspeed_10m_max");
            JsonNode weatherCode = daily.get("weathercode");

            // Write CSV file
            try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
                // Write header
                writer.println("Date,Day,Temperature_Mean_" + unit + ",Temp_Min_" + unit +
                    ",Temp_Max_" + unit + ",Weather,Wind_km/h,Rain_mm,Precipitation_mm");

                int daysCount = times != null ? times.size() : 0;

                // Write each day
                for (int i = 0; i < daysCount; i++) {
                    String dateStr = times.get(i).asText();
                    int daysAgo = daysCount - i;

                    // Temperature
                    double tempMeanVal = tempMean.get(i).asDouble();
                    double tempMaxVal = tempMax.get(i).asDouble();
                    double tempMinVal = tempMin.get(i).asDouble();

                    if (unit.equalsIgnoreCase("F")) {
                        tempMeanVal = celsiusToFahrenheit(tempMeanVal);
                        tempMaxVal = celsiusToFahrenheit(tempMaxVal);
                        tempMinVal = celsiusToFahrenheit(tempMinVal);
                    }

                    // Weather
                    int wmoCode = weatherCode.get(i).asInt();
                    String weatherDesc = decodeFunction.decode(wmoCode);

                    // Other values
                    double precipVal = precipitation.get(i).asDouble();
                    double rainVal = rain.get(i).asDouble();
                    double windVal = windSpeed.get(i).asDouble();

                    // Write data row
                    writer.println(String.format("%s,%d,%.1f,%.1f,%.1f,%s,%.1f,%.1f,%.1f",
                        dateStr, daysAgo, tempMeanVal, tempMinVal, tempMaxVal,
                        weatherDesc, windVal, rainVal, precipVal));
                }
            }

            return "SUCCESS: Historical data exported to: " + csvFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("Error exporting historical data to CSV: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: Failed to export data: " + e.getMessage();
        }
    }

    /**
     * Convert Celsius to Fahrenheit
     */
    private double celsiusToFahrenheit(double celsius) {
        return (celsius * 9.0 / 5.0) + 32.0;
    }

    /**
     * Functional interface for decoding WMO weather codes
     */
    @FunctionalInterface
    public interface WMOWeatherCodeDecoder {
        String decode(int code);
    }

    /**
     * Process historical weather data from Open-Meteo API
     * Returns formatted string with weather data for each day
     *
     * @param daily JSON node with daily data
     * @param unit Temperature unit (C or F)
     * @return Formatted historical data string (days separated by "|||")
     */
    public String processHistoricalData(JsonNode daily, String unit) {
        StringBuilder result = new StringBuilder();

        JsonNode times = daily.get("time");
        JsonNode tempMax = daily.get("temperature_2m_max");
        JsonNode tempMin = daily.get("temperature_2m_min");
        JsonNode tempMean = daily.get("temperature_2m_mean");
        JsonNode precipitation = daily.get("precipitation_sum");
        JsonNode rain = daily.get("rain_sum");
        JsonNode windSpeed = daily.get("windspeed_10m_max");
        JsonNode weatherCode = daily.get("weathercode");

        int daysCount = times != null ? times.size() : 0;

        for (int i = 0; i < daysCount; i++) {
            String dateStr = times.get(i).asText();

            // Temperature
            double tempMeanVal = tempMean.get(i).asDouble();
            double tempMaxVal = tempMax.get(i).asDouble();
            double tempMinVal = tempMin.get(i).asDouble();

            if (unit.equalsIgnoreCase("F")) {
                tempMeanVal = celsiusToFahrenheit(tempMeanVal);
                tempMaxVal = celsiusToFahrenheit(tempMaxVal);
                tempMinVal = celsiusToFahrenheit(tempMinVal);
            }

            // Other values
            double precipVal = precipitation.get(i).asDouble();
            double rainVal = rain.get(i).asDouble();
            double windVal = windSpeed.get(i).asDouble();
            int wmoCode = weatherCode.get(i).asInt();
            String weatherDesc = WeatherCodeDecoder.decode(wmoCode);

            // Days ago
            int daysAgo = daysCount - i;

            // Build output
            StringBuilder dayData = new StringBuilder();
            dayData.append(dateStr).append("\n");
            dayData.append("(").append(daysAgo).append(" day").append(daysAgo > 1 ? "s" : "").append(" ago)\n\n");
            dayData.append("Temp: ").append(String.format("%.1f", tempMeanVal)).append("°").append(unit).append("\n");
            dayData.append("Range: ").append(String.format("%.1f - %.1f", tempMinVal, tempMaxVal)).append("°").append(unit).append("\n");
            dayData.append("Weather: ").append(weatherDesc).append("\n");
            dayData.append("Wind: ").append(String.format("%.1f", windVal)).append(" km/h\n");
            dayData.append("Rain: ").append(String.format("%.1f", rainVal)).append(" mm\n");
            dayData.append("Precipitation: ").append(String.format("%.1f", precipVal)).append(" mm");

            if (i > 0) {
                result.append("|||");
            }
            result.append(dayData);
        }

        return result.toString();
    }
}

