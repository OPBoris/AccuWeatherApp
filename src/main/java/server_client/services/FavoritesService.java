package server_client.services;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class FavoritesService {
    private static final String DB_PATH = "src/main/DB/";

    public FavoritesService() {
        File dbDir = new File(DB_PATH);
        if (!dbDir.exists()) {
            if (!dbDir.mkdirs()) {
                System.out.println("Error: The system cannot create the DB folder at the specified path: " + DB_PATH);
            }
        }
    }

    public synchronized boolean addFavorite(String city, String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        String cleanCity = formatCityName(city);

        List<String> currentFavs = readListFromFile(favFile);
        if (currentFavs.contains(cleanCity)) {
            return true;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(favFile, true))) {
            writer.write(cleanCity);
            writer.newLine();
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

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(favFile, false))) {
            for (String fav : currentFavs) {
                writer.write(fav);
                writer.newLine();
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


    public List<String> getFavoritesList(String username) {
        String favFile = DB_PATH + username + "_favorites.txt";
        return readListFromFile(favFile);
    }


    public boolean isFavorite(String city, String username) {
        String cleanCity = formatCityName(city);
        List<String> favs = getFavoritesList(username);
        return favs.contains(cleanCity);
    }


    private String formatCityName(String city) {
        if (city == null) {
            return "";
        }
        return city.trim();
    }


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

