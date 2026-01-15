package fhtw.accuweatherapp.handler;

import javafx.concurrent.Task;
import javafx.scene.control.ComboBox;
import server_client.ClientConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;


public class UIHistoryHandler {

    private final ClientConnection connection;

    public UIHistoryHandler(ClientConnection connection) {
        this.connection = connection;
    }


    public void updateCityComboBox(ComboBox<String> comboBox) {
        if (comboBox == null) return;

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                String response = connection.sendCommand("GET_HISTORY");
                if (response != null && response.startsWith("HISTORY:")) {
                    String cities = response.substring(8).trim();
                    if (!cities.isEmpty()) {
                        return Arrays.asList(cities.split(","));
                    }
                }
                return new ArrayList<>();
            }
        };

        task.setOnSucceeded(event -> {
            comboBox.setItems(
                    javafx.collections.FXCollections.observableArrayList(task.getValue())
            );
            comboBox.getSelectionModel().clearSelection();
            comboBox.setValue(null);
        });

        task.setOnFailed(event -> {
            System.err.println("Error while loading history: " + task.getException().getMessage());
            comboBox.setItems(javafx.collections.FXCollections.observableArrayList());
            comboBox.getSelectionModel().clearSelection();
            comboBox.setValue(null);
        });

        new Thread(task, "load-history").start();
    }

    public void showHistory(Consumer<String> onResponse) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand("GET_HISTORY");
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if (resp != null){
                onResponse.accept(resp);
            } else {
                onResponse.accept("No response from server.");
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null) {
                onResponse.accept("Error: " + ex.getMessage());
            } else {
                onResponse.accept("Unknown error occurred.");
            }
        });

        new Thread(task, "show-history").start();
    }


    public void exportHistoricalDataCSV(String city, String unit, Consumer<String> onSuccess,
                                        Consumer<String> onError) {
        if (city == null || city.trim().length() < 3) {
            onError.accept("Please enter a city name first to export historical data.");
            return;
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand("EXPORT_HISTORY " + city.trim() + " " + unit);
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if (resp != null && !resp.isEmpty()) {
                onSuccess.accept(resp);
            } else {
                onError.accept("No response from server.");
            }
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            String errorMsg;
            if (exception != null) {
                errorMsg = "Export error: " + exception.getMessage();
            } else {
                errorMsg = "Export error: Unknown error";
            }
            onError.accept(errorMsg);

        });

        new Thread(task, "export-csv-call").start();
    }
}
