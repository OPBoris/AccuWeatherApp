package server_client.services;

import com.fasterxml.jackson.databind.JsonNode;
import server_client.ApiClient;
import server_client.WeatherCodeDecoder;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;


public class HistoryService {
    private static final int MAX_HISTORY_ENTRIES = 10;
    private static final String DB_FOLDER = "src/main/DB";
    private static final String HISTORY_CSV = DB_FOLDER + "/search_history.csv";
    private static final String OPEN_METEO_HISTORY_URL =
            "https://archive-api.open-meteo.com/v1/archive?latitude=%s&longitude=%s&start_date=%s&end_date=%s&daily=temperature_2m_max,temperature_2m_min,temperature_2m_mean,precipitation_sum,rain_sum,windspeed_10m_max,weathercode&timezone=auto";

    private final ApiClient apiClient;

    public HistoryService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }


    public void saveToHistory(String city, String username) {
        try {

            File dbFolder = new File(DB_FOLDER);
            if (!dbFolder.exists()) {
                dbFolder.mkdirs();
            }


            File csvFile = new File(HISTORY_CSV);
            boolean isNewFile = !csvFile.exists();


            try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile, true))) {

                if (isNewFile) {
                    writer.println("timestamp,username,city");
                }


                String timestamp = java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                );
                writer.println(String.format("%s,%s,%s", timestamp, username, city.trim()));
            }

        } catch (IOException e) {
            System.err.println("Error while writing to CSV database: " + e.getMessage());
        }
    }


    public String getRecentCities(String username) {
        File csvFile = new File(HISTORY_CSV);
        if (!csvFile.exists()) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            List<String> cities = reader.lines()
                    .skip(1)
                    .map(line -> line.split(","))
                    .filter(parts -> parts.length >= 3 && parts[1].equals(username))
                    .map(parts -> parts[2])
                    .collect(Collectors.toList());


            java.util.Collections.reverse(cities);


            return cities.stream()
                    .distinct()
                    .limit(MAX_HISTORY_ENTRIES)
                    .collect(Collectors.joining(","));

        } catch (IOException e) {
            System.err.println("Error while reading CSV database: " + e.getMessage());
            return "ERROR: History unavailable.";
        }
    }


    public String exportHistoricalDataToCSV(String cityName, double lat, double lon,
                                            String unit, String username) {
        try {

            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate endDate = today.minusDays(1);
            java.time.LocalDate startDate = endDate.minusDays(29);


            String url = String.format(OPEN_METEO_HISTORY_URL, lat, lon, startDate, endDate);
            JsonNode weatherData = apiClient.makeOpenMeteoCall(url);

            if (weatherData == null || !weatherData.has("daily")) {
                return "ERROR: Unable to fetch historical data from Open-Meteo.";
            }

            return exportHistoricalDataToCSVInternal(cityName, lat, lon, unit, username, weatherData);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }


    private String exportHistoricalDataToCSVInternal(String cityName, double lat, double lon, String unit,
                                                     String username, JsonNode weatherData) {
        try {

            File dbFolder = new File(DB_FOLDER);
            if (!dbFolder.exists()) {
                dbFolder.mkdirs();
            }


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


            try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {

                writer.println("Date,Day,Temperature_Mean_" + unit + ",Temp_Min_" + unit +
                        ",Temp_Max_" + unit + ",Weather,Wind_km/h,Rain_mm,Precipitation_mm");

                int daysCount;
                if (times != null) {
                    daysCount = times.size();
                } else {
                    daysCount = 0;
                }

                for (int i = 0; i < daysCount; i++) {
                    String dateStr = times.get(i).asText();
                    int daysAgo = daysCount - i;


                    double tempMeanVal = tempMean.get(i).asDouble();
                    double tempMaxVal = tempMax.get(i).asDouble();
                    double tempMinVal = tempMin.get(i).asDouble();

                    if (unit.equalsIgnoreCase("F")) {
                        tempMeanVal = celsiusToFahrenheit(tempMeanVal);
                        tempMaxVal = celsiusToFahrenheit(tempMaxVal);
                        tempMinVal = celsiusToFahrenheit(tempMinVal);
                    }


                    int wmoCode = weatherCode.get(i).asInt();
                    String weatherDesc = WeatherCodeDecoder.decode(wmoCode);


                    double precipVal = precipitation.get(i).asDouble();
                    double rainVal = rain.get(i).asDouble();
                    double windVal = windSpeed.get(i).asDouble();


                    writer.println(String.format("%s,%d,%.1f,%.1f,%.1f,%s,%.1f,%.1f,%.1f",
                            dateStr, daysAgo, tempMeanVal, tempMinVal, tempMaxVal,
                            weatherDesc, windVal, rainVal, precipVal));
                }
            }

            return "SUCCESS: Historical data exported to: " + csvFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("Error exporting historical data to CSV: " + e.getMessage());
            return "ERROR: Failed to export data: " + e.getMessage();
        }
    }


    public String getHistoricalWeather(double lat, double lon, String unit) {
        try {

            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate endDate = today.minusDays(1);
            java.time.LocalDate startDate = endDate.minusDays(4);


            String url = String.format(OPEN_METEO_HISTORY_URL, lat, lon, startDate, endDate);
            JsonNode data = apiClient.makeOpenMeteoCall(url);

            if (data == null || !data.has("daily")) {
                return "ERROR: Unable to fetch historical data.";
            }

            return processHistoricalData(data.get("daily"), unit);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }


    private double celsiusToFahrenheit(double celsius) {
        return (celsius * 9.0 / 5.0) + 32.0;
    }


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

        int daysCount;
        if (times != null) {
            daysCount = times.size();
        } else {
            daysCount = 0;
        }

        for (int i = 0; i < daysCount; i++) {
            String dateStr = times.get(i).asText();


            double tempMeanVal = tempMean.get(i).asDouble();
            double tempMaxVal = tempMax.get(i).asDouble();
            double tempMinVal = tempMin.get(i).asDouble();

            if (unit.equalsIgnoreCase("F")) {
                tempMeanVal = celsiusToFahrenheit(tempMeanVal);
                tempMaxVal = celsiusToFahrenheit(tempMaxVal);
                tempMinVal = celsiusToFahrenheit(tempMinVal);
            }


            double precipVal = precipitation.get(i).asDouble();
            double rainVal = rain.get(i).asDouble();
            double windVal = windSpeed.get(i).asDouble();
            int wmoCode = weatherCode.get(i).asInt();
            String weatherDesc = WeatherCodeDecoder.decode(wmoCode);


            int daysAgo = daysCount - i;


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

