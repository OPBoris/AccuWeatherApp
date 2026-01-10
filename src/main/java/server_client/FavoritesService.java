package server_client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * FavoritesService - Manages user favorites
 * Handles adding, removing and retrieving favorite cities for users
 */
public class FavoritesService {
    private static final String DB_PATH = "src/main/DB/";

    public FavoritesService() {
        try {
            Files.createDirectories(Paths.get(DB_PATH));
        } catch (IOException e) {
            System.out.println("Error: The system cannot create the DB folder at the specified path: " + DB_PATH + " -> " + e.getMessage());
        }
    }

    /**
     * Add a city to user's favorites
     * @param city City name to add
     * @param username Username
     * @return true if successful, false otherwise
     */
    public synchronized boolean addFavorite(String city, String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        String cleanCity = formatCityName(city);

        List<String> currentFavs = readListFromFile(favFile);
        if (currentFavs.contains(cleanCity)) {
            return true; // Already in favorites
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(favFile, true))) {
            writer.println(cleanCity);
            return true;
        } catch (IOException e) {
            System.out.println("Error saving favorite: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remove a city from user's favorites
     * @param city City name to remove
     * @param username Username
     * @return true if successful, false if city was not in favorites
     */
    public synchronized boolean removeFavorite(String city, String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        String cleanCity = formatCityName(city);

        List<String> currentFavs = readListFromFile(favFile);
        if (!currentFavs.contains(cleanCity)) {
            return false; // Not in favorites
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

    /**
     * Get all favorites for a user
     * @param username Username
     * @return Comma-separated list of favorite cities
     */
    public String getFavorites(String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        List<String> favs = readListFromFile(favFile);
        return String.join(",", favs);
    }

    /**
     * Get all favorites as a list
     * @param username Username
     * @return List of favorite cities
     */
    public List<String> getFavoritesList(String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        return readListFromFile(favFile);
    }

    /**
     * Check if a city is in user's favorites
     * @param city City name to check
     * @param username Username
     * @return true if city is in favorites, false otherwise
     */
    public boolean isFavorite(String city, String username) {
        String cleanCity = formatCityName(city);
        List<String> favs = getFavoritesList(username);
        return favs.contains(cleanCity);
    }

    // =====================================================
    // PRIVATE HELPER METHODS
    // =====================================================

    /**
     * Format city name to ensure consistency
     * Trims whitespace and normalizes the format
     */
    private String formatCityName(String city) {
        if (city == null) {
            return "";
        }
        return city.trim();
    }

    /**
     * Read a list of strings from a file (one per line)
     * Returns empty list if file doesn't exist
     */
    private List<String> readListFromFile(String filePath) {
        List<String> lines = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file " + filePath + ": " + e.getMessage());
        }

        return lines;
    }
}

