package fhtw.accuweatherapp.handler;

import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import server_client.ClientConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;


public class UIFavoritesHandler {

    private final ClientConnection connection;
    private boolean isCurrentCityFavorite = false;

    public UIFavoritesHandler(ClientConnection connection) {
        this.connection = connection;
    }

    public boolean isCurrentCityFavorite() {
        return isCurrentCityFavorite;
    }

    public void setCurrentCityFavorite(boolean favorite) {
        this.isCurrentCityFavorite = favorite;
    }


    public void toggleFavorite(String city, Consumer<String> onSuccess, Consumer<String> onError) {
        if (city == null || city.trim().isEmpty() || city.trim().length() < 3) {
            onError.accept("Please provide a valid city name.");
            return;
        }

        String trimmedCity = city.trim();
        String command = isCurrentCityFavorite
                ? "REMOVE_FAVORITE " + trimmedCity
                : "ADD_FAVORITE " + trimmedCity;

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand(command);
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if (resp.startsWith("OK")) {
                isCurrentCityFavorite = !isCurrentCityFavorite;
                String action = isCurrentCityFavorite ? "added" : "removed";
                onSuccess.accept(trimmedCity + " " + action + " " + "to favourites.");
            } else {
                onError.accept("Error: " + resp);
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            onError.accept("Error: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        new Thread(task, "toggle-favorite").start();
    }


    public void checkFavoriteStatus(String city, Runnable onComplete) {
        if (city == null || city.trim().isEmpty()) return;

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand("IS_FAVORITE " + city.trim());
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if (resp != null && resp.startsWith("IS_FAVORITE:")) {
                String boolPart = resp.substring(12).trim();
                isCurrentCityFavorite = Boolean.parseBoolean(boolPart);
            }
            if (onComplete != null) {
                onComplete.run();
            }
        });

        new Thread(task, "check-favorite").start();
    }


    public void loadFavorites(ListView<String> listView) {
        if (listView == null) return;

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                String response = connection.sendCommand("GET_FAVORITES");
                if (response != null && response.startsWith("FAVORITES:")) {
                    String data = response.substring(10).trim();
                    if (!data.isEmpty()) {
                        return Arrays.asList(data.split(","));
                    }
                }
                return new ArrayList<>();
            }
        };

        task.setOnSucceeded(e -> {
            listView.getItems().clear();
            listView.getItems().addAll(task.getValue());
        });

        new Thread(task, "load-favorites").start();
    }


    public void updateStarStyle(Button btnFavorite, boolean isDarkMode) {
        if (btnFavorite == null) return;

        if (isCurrentCityFavorite) {
            btnFavorite.setStyle("-fx-text-fill: gold; -fx-font-size: 16px; -fx-font-weight: bold;");
            btnFavorite.setText("★");
        } else {
            String color = isDarkMode ? "white" : "black";
            btnFavorite.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px;");
            btnFavorite.setText("☆");
        }
    }
}
