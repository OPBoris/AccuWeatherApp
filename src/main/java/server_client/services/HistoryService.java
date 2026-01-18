package server_client.services;

import server_client.api.ApiClient;
import server_client.api.ApiUrls;
import server_client.JsonParser;
import server_client.WeatherCodeDecoder;
import server_client.exceptions.WeatherAppException;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


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
            List<String> oldLines = new ArrayList<>();
            if (!isNewFile) {
                try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                    reader.readLine();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        oldLines.add(line);
                    }
                } catch (IOException e) {
                    System.err.println("Error while reading the CSV File: " + e.getMessage());
                }
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
                writer.write("timestamp,username,city");
                writer.newLine();
                String timestamp = java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                );
                writer.write(String.format("%s,%s,%s", timestamp, username, city.trim()));
                writer.newLine();
                for (String oldLine : oldLines) {
                    writer.write(oldLine);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Error while writing to CSV database: " + e.getMessage());
        }
    }


    public String getRecentCities(String username) throws WeatherAppException {
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


            return cities.stream()
                    .distinct()
                    .limit(MAX_HISTORY_ENTRIES)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

        } catch (IOException e) {
            throw new WeatherAppException("Could not load search history.", e);
        }
    }


    public String exportHistoricalDataToCSV(String cityName, double lat, double lon,
                                            String unit, String username) throws WeatherAppException {
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

        } catch (WeatherAppException e) {
            throw e;
        } catch (Exception e) {
            throw new WeatherAppException("Export failed: " + e.getMessage(), e);
        }
    }


    private String exportHistoricalDataToCSVInternal(String cityName, String unit,
                                                     String username, String jsonData) throws WeatherAppException {
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
                throw new WeatherAppException("Unable to fetch historical data.");
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
            List<String> windSpeed = JsonParser.parseArrayValues(dailyBlock, "windspeed_10m_max");
            List<String> weatherCode = JsonParser.parseArrayValues(dailyBlock, "weathercode");

            Collections.reverse(times);
            Collections.reverse(tempMax);
            Collections.reverse(tempMin);
            Collections.reverse(precipitation);
            Collections.reverse(windSpeed);
            Collections.reverse(weatherCode);


            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {

                writer.write("Date,Day,Temperature_Mean_" + unit + ",Temp_Max_" + unit +
                        ",Temp_Min_" + unit + ",Weather,Wind_km/h,Precipitation_mm");
                writer.newLine();

                int daysCount = times.size();

                for (int i = 0; i < daysCount; i++) {
                    String dateStr = times.get(i).replace("\"", "");
                    int daysAgo = i + 1;


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

                    double windVal = Double.parseDouble(windSpeed.get(i));


                    writer.write(String.format(Locale.ENGLISH, "%s,%d,%.1f,%.1f,%.1f,%s,%.1f ,%.1f",
                            dateStr, daysAgo, tempMeanVal, tempMaxVal, tempMinVal,
                            weatherDesc, windVal, precipVal));
                    writer.newLine();
                }
            }

            return "SUCCESS: Historical data exported to: " + csvFile.getPath();

        } catch (IOException e) {
            throw new WeatherAppException("File error during export.", e);
        } catch (Exception e) {
            throw new WeatherAppException("Error processing historical data.", e);
        }
    }


    public String getHistoricalWeather(double lat, double lon, String unit) throws WeatherAppException {
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

        } catch (WeatherAppException e) {
            throw e;
        } catch (Exception e) {
            throw new WeatherAppException("Error loading historical weather data.", e);
        }
    }


    private double celsiusToFahrenheit(double celsius) {
        return (celsius * 9.0 / 5.0) + 32.0;
    }


    public String processHistoricalData(String dailyBlock, String unit) throws WeatherAppException {
        try {
            StringBuilder result = new StringBuilder();

            List<String> times = JsonParser.parseArrayValues(dailyBlock, "time");
            List<String> tempMax = JsonParser.parseArrayValues(dailyBlock, "temperature_2m_max");
            List<String> tempMin = JsonParser.parseArrayValues(dailyBlock, "temperature_2m_min");
            List<String> rain = JsonParser.parseArrayValues(dailyBlock, "rain_sum");
            List<String> windSpeed = JsonParser.parseArrayValues(dailyBlock, "windspeed_10m_max");
            List<String> weatherCode = JsonParser.parseArrayValues(dailyBlock, "weathercode");

            Collections.reverse(times);
            Collections.reverse(tempMax);
            Collections.reverse(tempMin);
            Collections.reverse(rain);
            Collections.reverse(windSpeed);
            Collections.reverse(weatherCode);

            int daysCount = times.size();

            for (int i = 0; i < daysCount; i++) {
                String dateStr = times.get(i).replace("\"", "");


                double tempMaxVal = Double.parseDouble(tempMax.get(i));
                double tempMinVal = Double.parseDouble(tempMin.get(i));

                if (unit.equalsIgnoreCase("F")) {
                    tempMaxVal = celsiusToFahrenheit(tempMaxVal);
                    tempMinVal = celsiusToFahrenheit(tempMinVal);
                }


                double rainVal = Double.parseDouble(rain.get(i));
                double windVal = Double.parseDouble(windSpeed.get(i));
                int wmoCode = Integer.parseInt(weatherCode.get(i));
                String weatherDesc = WeatherCodeDecoder.decode(wmoCode);

                int daysAgo = i + 1;

                String daysAgoSuffix;
                if (daysAgo > 1) {
                    daysAgoSuffix = "s";
                } else {
                    daysAgoSuffix = "";
                }

                String dayData = dateStr + "\n" +
                        "(" + daysAgo + " day" + daysAgoSuffix + " ago)\n\n" +
                        "Max: " + String.format("%.1f", tempMaxVal) + "°" + unit + "\n" +
                        "Min: " + String.format("%.1f", tempMinVal) + "°" + unit + "\n" +
                        "Weather: " + weatherDesc + "\n" +
                        "Wind: " + String.format("%.1f", windVal) + " km/h\n" +
                        "Rain: " + String.format("%.1f", rainVal) + " mm\n";

                if (i > 0) {
                    result.append("|||");
                }
                result.append(dayData);
            }

            return result.toString();
        } catch (Exception e) {
            throw new WeatherAppException("Error parsing historical data.", e);
        }

    }
}
