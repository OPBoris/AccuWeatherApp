package fhtw.accuweatherapp.handler;
import fhtw.accuweatherapp.Callback;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import server_client.clients.ClientConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class UIFavoritesHandler {

    private final ClientConnection connection;
    private boolean isCurrentCityFavorite = false;

    public UIFavoritesHandler(ClientConnection connection) {
        this.connection = connection;
    }

    public void toggleFavorite(String city, Callback<String> onSuccess, Callback<String> onError) {
        if (city == null || city.trim().length() < 3) {
            onError.call("Please provide a valid city name.");
            return;
        }

        String trimmedCity = city.trim();
        String command;
        if (isCurrentCityFavorite) {
            command = "REMOVE_FAVORITE " + trimmedCity;
        } else {
            command = "ADD_FAVORITE " + trimmedCity;
        }

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
                String action;
                if (isCurrentCityFavorite) {
                    action = "added";
                } else {
                    action = "removed";
                }
                onSuccess.call(trimmedCity + " " + action + " " + "to favourites.");
            } else {
                onError.call("Error: " + resp);
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String errorMessage;
            if (ex != null) {
                errorMessage = ex.getMessage();
            } else {
                errorMessage = "Unknown error";
            }
            onError.call("Error: " + errorMessage);
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

        task.setOnFailed(e -> {
            isCurrentCityFavorite = false;
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
                        List<String> result = new ArrayList<>();
                        Collections.addAll(result, data.split(","));
                        return result;
                    }
                }
                return new ArrayList<>();
            }
        };

        task.setOnSucceeded(e -> {
            listView.getItems().clear();
            listView.getItems().addAll(task.getValue());
        });

        task.setOnFailed(e -> listView.getItems().clear());

        new Thread(task, "load-favorites").start();
    }


    public void updateStarStyle(Button btnFavorite, boolean isDarkMode) {
        if (btnFavorite == null) return;

        if (isCurrentCityFavorite) {
            btnFavorite.setStyle("-fx-text-fill: gold; -fx-font-weight: bold;");
            btnFavorite.setText("★");
        } else {
            String color;
            if (isDarkMode) {
                color = "white";
            } else {
                color = "black";
            }
            btnFavorite.setStyle("-fx-text-fill: " + color + ";");
            btnFavorite.setText("☆");
        }
    }
}
