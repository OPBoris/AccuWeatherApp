package server_client.services;

import server_client.ApiClient;
import server_client.ApiUrls;
import server_client.JsonParser;
import server_client.WeatherCodeDecoder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class HistoryService {
    private static final int MAX_HISTORY_ENTRIES = 10;
    private static final String DB_FOLDER = "src/main/DB";
    private static final String HISTORY_CSV = DB_FOLDER + "/search_history.csv";

    private final ApiClient apiClient;

    public HistoryService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }


    public synchronized void saveToHistory(String city, String username) {
        try {

            File dbFolder = new File(DB_FOLDER);
            boolean success = dbFolder.mkdirs();
            if (!success) {
                System.err.println("ERROR: DB folder creation failed or already exists.");
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
                    .toList();


            List<String> reversed = new ArrayList<>(cities);
            java.util.Collections.reverse(reversed);


            return cities.stream()
                    .distinct()
                    .limit(MAX_HISTORY_ENTRIES)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

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


            String url = String.format(ApiUrls.HISTORICAL, lat, lon, startDate, endDate);
            String weatherData = apiClient.makeOpenMeteoCall(url);

            if (weatherData == null || !weatherData.contains("\"daily\"")) {
                return "ERROR: Unable to fetch historical data from Open-Meteo.";
            }

            return exportHistoricalDataToCSVInternal(cityName, unit, username, weatherData);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }


    private String exportHistoricalDataToCSVInternal(String cityName, String unit,
                                                     String username, String jsonData) {
        try {

            File dbFolder = new File(DB_FOLDER);
            boolean success = dbFolder.mkdirs();
            if (!success) {
                System.err.println("ERROR: DB folder creation failed or already exists.");
            }


            String sanitizedCity = cityName.replaceAll("[^a-zA-Z0-9]", "_");
            String sanitizedUser = username.replaceAll("[^a-zA-Z0-9]", "_");
            String timestamp = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            );
            String filename = String.format("30_day_weather_history_%s_%s_%s.csv",
                    sanitizedUser, sanitizedCity, timestamp);
            File csvFile = new File(DB_FOLDER, filename);

            if (jsonData == null || !jsonData.contains("\"daily\"")) {
                return "ERROR: Unable to fetch historical data from Open-Meteo.";
            }

            int dailyStart = jsonData.indexOf("\"daily\":");
            int objectStart = jsonData.indexOf("{", dailyStart);
            int objectEnd = JsonParser.findMatchingBrace(jsonData, objectStart);
            String dailyBlock = jsonData.substring(objectStart, objectEnd + 1);

            List<String> times = JsonParser.parseArrayValues(dailyBlock, "time");
            List<String> tempMax = JsonParser.parseArrayValues(dailyBlock, "temperature_2m_max");
            List<String> tempMin = JsonParser.parseArrayValues(dailyBlock, "temperature_2m_min");
            List<String> tempMean = JsonParser.parseArrayValues(dailyBlock, "temperature_2m_mean");
            List<String> precipitation = JsonParser.parseArrayValues(dailyBlock, "precipitation_sum");
            List<String> rain = JsonParser.parseArrayValues(dailyBlock, "rain_sum");
            List<String> windSpeed = JsonParser.parseArrayValues(dailyBlock, "windspeed_10m_max");
            List<String> weatherCode = JsonParser.parseArrayValues(dailyBlock, "weathercode");


            try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {

                writer.println("Date,Day,Temperature_Mean_" + unit + ",Temp_Min_" + unit +
                        ",Temp_Max_" + unit + ",Weather,Wind_km/h,Rain_mm,Precipitation_mm");

                int daysCount = times.size();

                for (int i = 0; i < daysCount; i++) {
                    String dateStr = times.get(i).replace("\"", "");
                    int daysAgo = daysCount - i;


                    double tempMeanVal = Double.parseDouble(tempMean.get(i));
                    double tempMaxVal = Double.parseDouble(tempMax.get(i));
                    double tempMinVal = Double.parseDouble(tempMin.get(i));

                    if (unit.equalsIgnoreCase("F")) {
                        tempMeanVal = celsiusToFahrenheit(tempMeanVal);
                        tempMaxVal = celsiusToFahrenheit(tempMaxVal);
                        tempMinVal = celsiusToFahrenheit(tempMinVal);
                    }


                    int wmoCode = Integer.parseInt(weatherCode.get(i));
                    String weatherDesc = WeatherCodeDecoder.decode(wmoCode);


                    double precipVal = Double.parseDouble(precipitation.get(i));
                    double rainVal = Double.parseDouble(rain.get(i));
                    double windVal = Double.parseDouble(windSpeed.get(i));


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


            String url = String.format(ApiUrls.HISTORICAL, lat, lon, startDate, endDate);
            String data = apiClient.makeOpenMeteoCall(url);

            if (data == null || !data.contains("\"daily\"")) {
                return "ERROR: Unable to fetch historical data.";
            }

            int dailyStart = data.indexOf("\"daily\":");
            int objectStart = data.indexOf("{", dailyStart);
            int objectEnd = JsonParser.findMatchingBrace(data, objectStart);
            String dailyBlock = data.substring(objectStart, objectEnd + 1);

            return processHistoricalData(dailyBlock, unit);

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }


    private double celsiusToFahrenheit(double celsius) {
        return (celsius * 9.0 / 5.0) + 32.0;
    }


    public String processHistoricalData(String dailyBlock, String unit) {
        StringBuilder result = new StringBuilder();

        List<String> times = JsonParser.parseArrayValues(dailyBlock, "time");
        List<String> tempMax = JsonParser.parseArrayValues(dailyBlock, "temperature_2m_max");
        List<String> tempMin = JsonParser.parseArrayValues(dailyBlock, "temperature_2m_min");
        List<String> tempMean = JsonParser.parseArrayValues(dailyBlock, "temperature_2m_mean");
        List<String> precipitation = JsonParser.parseArrayValues(dailyBlock, "precipitation_sum");
        List<String> rain = JsonParser.parseArrayValues(dailyBlock, "rain_sum");
        List<String> windSpeed = JsonParser.parseArrayValues(dailyBlock, "windspeed_10m_max");
        List<String> weatherCode = JsonParser.parseArrayValues(dailyBlock, "weathercode");

        int daysCount = times.size();

        for (int i = 0; i < daysCount; i++) {
            String dateStr = times.get(i).replace("\"", "");


            double tempMeanVal = Double.parseDouble(tempMean.get(i));
            double tempMaxVal = Double.parseDouble(tempMax.get(i));
            double tempMinVal = Double.parseDouble(tempMin.get(i));

            if (unit.equalsIgnoreCase("F")) {
                tempMeanVal = celsiusToFahrenheit(tempMeanVal);
                tempMaxVal = celsiusToFahrenheit(tempMaxVal);
                tempMinVal = celsiusToFahrenheit(tempMinVal);
            }


            double precipVal = Double.parseDouble(precipitation.get(i));
            double rainVal = Double.parseDouble(rain.get(i));
            double windVal = Double.parseDouble(windSpeed.get(i));
            int wmoCode = Integer.parseInt(weatherCode.get(i));
            String weatherDesc = WeatherCodeDecoder.decode(wmoCode);


            int daysAgo = daysCount - i;


            String dayData = dateStr + "\n" +
                    "(" + daysAgo + " day" + (daysAgo > 1 ? "s" : "") + " ago)\n\n" +
                    "Temp: " + String.format("%.1f", tempMeanVal) + "°" + unit + "\n" +
                    "Range: " + String.format("%.1f - %.1f", tempMinVal, tempMaxVal) + "°" + unit + "\n" +
                    "Weather: " + weatherDesc + "\n" +
                    "Wind: " + String.format("%.1f", windVal) + " km/h\n" +
                    "Rain: " + String.format("%.1f", rainVal) + " mm\n" +
                    "Precipitation: " + String.format("%.1f", precipVal) + " mm";

            if (i > 0) {
                result.append("|||");
            }
            result.append(dayData);
        }

        return result.toString();
    }
}

