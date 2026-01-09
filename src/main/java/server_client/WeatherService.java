package server_client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


public class WeatherService {
    private static final int MAX_HISTORY_ENTRIES = 10;
    private static final String DB_PATH = "src/main/DB/";

    public WeatherService() {
        try {
            Files.createDirectories(Paths.get(DB_PATH));
        } catch (IOException e) {
            System.out.println("Error: The system cannot create the DB folder at the specified path.: " + DB_PATH + " -> " + e.getMessage());
        }
    }

    public String getWeatherForCity(String city, String unit, String username) {

        if (city == null || city.trim().isEmpty()) {
            return "ERROR: City name cannot be empty.";
        }

        String cleanCity = formatCityName(city);

        if (cleanCity.equalsIgnoreCase("ErrorCity")) {
            return "ERROR: City not found.";
        }

        saveToHistory(cleanCity, username);

        // MOCK, later API call
        // TODO: JAN HttpURLConnection to AccuWeatheru
        return mockWeatherData(cleanCity, unit);
    }

    public String searchCities(String partialName) {
        if (partialName == null || partialName.trim().length() < 2) {
            return "ERROR: Query too short. Please enter at least 2 characters.";
        }

        String query = partialName.trim().toLowerCase();

        // MOCK: Simulation
        List<String> suggestions = List.of(
                "Wien,AT",
                "Graz,AT",
                "London,UK",
                "New York,US",
                "Miami,US",
                "Paris,FR",
                "Paris,US",
                "Berlin,DE"
        );

        return suggestions.stream()
                .filter(s -> s.toLowerCase().contains(query))
                .limit(10)
                .collect(Collectors.joining(";"));
    }

    public synchronized boolean addFavorite(String city, String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        String cleanCity = formatCityName(city);

        List<String> currentFavs = readListFromFile(favFile);
        if (currentFavs.contains(cleanCity)) {
            return true;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(favFile, true))) {
            writer.println(cleanCity);
            return true;
        } catch (IOException e) {
            System.out.println("Error saving favorite: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean removeFavorite(String city, String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        String cleanCity = formatCityName(city);

        List<String> currentFavs = readListFromFile(favFile);
        if (!currentFavs.contains(cleanCity)) {
            return false;
        }

        currentFavs.remove(cleanCity);

        try (PrintWriter writer = new PrintWriter(new FileWriter(favFile, false))) {
            for (String fav : currentFavs) {
                writer.println(fav);
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error removing favorite: " + e.getMessage());
            return false;
        }
    }

    public String getFavorites(String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        List<String> favs = readListFromFile(favFile);
        return String.join(",", favs);
    }


    public String getRecentCities(String username) {
        String historyFile = DB_PATH + username + "_history.txt";
        List<String> allLines = readListFromFile(historyFile);

        Collections.reverse(allLines);

        return allLines.stream()
                .distinct()
                .limit(MAX_HISTORY_ENTRIES)
                .collect(Collectors.joining(","));
    }

    // TODO: JAN bearbeiten was alles wird in history gespeichert, format etc.
    private synchronized void saveToHistory(String city, String username) {

        String historyFile = DB_PATH + username + "_history.txt";
        String formattedCity = formatCityName(city);
        String cityWithCountry = formattedCity + "," + mockCountry(formattedCity);
        try (PrintWriter writer = new PrintWriter(new FileWriter(historyFile, true))) {
            writer.println(formattedCity);
        } catch (IOException e) {
            System.err.println("Error while writing to history: " + e.getMessage());
        }
    }

    private List<String> readListFromFile(String fileName) {
        if (!Files.exists(Paths.get(fileName))) {
            return new ArrayList<>();
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            return reader.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            System.out.println("Error reading file " + fileName + ": " + e.getMessage());
            return new ArrayList<>();
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
    private String mockCountry(String city) {
        if(city.equalsIgnoreCase("Wien") || city.equalsIgnoreCase("Graz")) return "AT";
        if(city.equalsIgnoreCase("New York") || city.equalsIgnoreCase("Miami")) return "US";
        if(city.equalsIgnoreCase("London")) return "UK";
        if(city.equalsIgnoreCase("Paris")) return "FR";
        if(city.equalsIgnoreCase("Berlin")) return "DE";
        return "N/A";
    }
    private String mockWeatherData(String city, String unit) {
        Random rand = new Random();
        boolean isMetric = "C".equalsIgnoreCase(unit);

        double tempBase = 10 + rand.nextInt(20);
        double temp = isMetric ? tempBase : (tempBase * 9.0/5.0) + 32;

        double feelsLikeBase = tempBase - 2;
        double feelsLike = isMetric ? feelsLikeBase : (feelsLikeBase * 9.0/5.0) + 32;

        int humidity = 40 + rand.nextInt(40);
        int windSpeed = rand.nextInt(30);
        int precipProb = rand.nextInt(100);

        String[] conditions = {"Sunny", "Cloudy", "Rainy"};
        String condition = conditions[rand.nextInt(conditions.length)];
        String country = mockCountry(city);

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        return String.format(Locale.US,
                "CITY=%s;COUNTRY=%s;TEMP=%.1f;FEELS_LIKE=%.1f;HUMIDITY=%d;WIND=%d;PRECIP=%d;CONDITION=%s;UNIT=%s;TIME=%s",
                city, country, temp, feelsLike, humidity, windSpeed, precipProb, condition, isMetric ? "C" : "F", time);
    }
}
