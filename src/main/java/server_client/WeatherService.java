package server_client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class WeatherService {
    private static final String HISTORY_FILE = "search_history.txt";

    public String getWeatherForCity(String city) {

        if (city == null || city.trim().isEmpty()) {
            return "ERROR: City name cannot be empty.";
        }

        String cleanCity = formatCityName(city);

        saveToHistory(cleanCity);

        // MOCK, later API call
        // TODO: JAN HttpURLConnection to AccuWeatheru
        return mockWeatherData(cleanCity);
    }

    public String getRecentCities() {
        if (!Files.exists(Paths.get(HISTORY_FILE))) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_FILE))) {

            List<String> allLines = reader.lines().collect(Collectors.toList());
            Collections.reverse(allLines);

            return allLines.stream()
                    .distinct()
                    .limit(5)
                    .collect(Collectors.joining(","));
        } catch (IOException e) {
            System.err.println("Error reading history: " + e.getMessage());
            return "ERROR: Unable to read history.";
        }
    }

    // TODO: JAN bearbeiten was alles wird in history gespeichert, format etc.
    private synchronized void saveToHistory(String city) {

        String formattedCity = formatCityName(city);
        try (PrintWriter writer = new PrintWriter(new FileWriter(HISTORY_FILE, true))) {
            writer.println(formattedCity);
        } catch (IOException e) {
            System.err.println("Error while writing to history: " + e.getMessage());
        }
    }

    private String formatCityName(String city) {
        city = city.trim().toLowerCase();

        if (city.isEmpty()) {
            return city;
        }

        String[] parts = city.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    // TODO: JAN ersetzen mit echtem API call und parsing - funktion löschen oder anpassen
    private String mockWeatherData(String city) {
        Random rand = new Random();
        int temp = 10 + rand.nextInt(25);
        String[] desc = {"Sunny", "Cloudy", "Rain", "Windy", "Stormy", "Snowy"};
        String description = desc[rand.nextInt(desc.length)];

        return city + "|" + temp + "°C|" + description;
    }
}
