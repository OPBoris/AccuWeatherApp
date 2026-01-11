package fhtw.accuweatherapp.handler;

import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import server_client.ClientConnection;

import java.util.function.Consumer;


public class UIOfflineHandler {

    private final ClientConnection connection;

    public UIOfflineHandler(ClientConnection connection) {
        this.connection = connection;
    }


    public void saveOfflineData(String city, String unit, Consumer<String> onSuccess,
                                Consumer<String> onError) {
        if (city == null || city.trim().length() < 3) {
            onError.accept("Please enter a valid city name to download offline data.");
            return;
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand("SAVE_OFFLINE " + city.trim() + " " + unit);
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
            String errorMsg = exception != null
                    ? "Error saving offline data: " + exception.getMessage()
                    : "Error saving offline data: Unknown error";
            onError.accept(errorMsg);
        });

        new Thread(task, "save-offline-call").start();
    }


    public void loadOfflineCurrentWeather(Consumer<String> onSuccess, Consumer<String> onError) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand("LOAD_OFFLINE");
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if (resp != null && !resp.isEmpty()) {
                onSuccess.accept(resp);
            } else {
                onError.accept("No offline data available.");
            }
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            String errorMsg = exception != null
                    ? "Error loading offline data: " + exception.getMessage()
                    : "Error loading offline data: Unknown error";
            onError.accept(errorMsg);
        });

        new Thread(task, "load-offline-call").start();
    }

    public void loadOfflineForecast(Consumer<String[]> onSuccess, Consumer<String> onError) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand("GET_OFFLINE_FORECAST");
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if (resp != null && !resp.isEmpty() && !resp.startsWith("ERROR")) {
                String[] dataArray = resp.split("\\|\\|\\|");
                onSuccess.accept(dataArray);
            } else {
                onError.accept("No offline forecast data available.");
            }
        });

        task.setOnFailed(e -> {
            onError.accept("Offline forecast error");
        });

        new Thread(task, "load-offline-forecast-call").start();
    }


    public void loadAllOfflineData(Consumer<String> onCurrentWeather,
                                   Consumer<String[]> onForecast,
                                   TextArea day1, TextArea day2, TextArea day3,
                                   TextArea day4, TextArea day5) {

        loadOfflineCurrentWeather(
                onCurrentWeather,
                onCurrentWeather
        );


        loadOfflineForecast(
                dataArray -> {
                    if (dataArray.length > 0) day1.setText(dataArray[0]);
                    if (dataArray.length > 1) day2.setText(dataArray[1]);
                    if (dataArray.length > 2) day3.setText(dataArray[2]);
                    if (dataArray.length > 3) day4.setText(dataArray[3]);
                    if (dataArray.length > 4) day5.setText(dataArray[4]);

                    if (dataArray.length < 5) day5.setText("N/A");
                    if (dataArray.length < 4) day4.setText("N/A");
                    if (dataArray.length < 3) day3.setText("N/A");
                    if (dataArray.length < 2) day2.setText("N/A");
                    if (dataArray.length < 1) day1.setText("N/A");
                },
                error -> {
                    day1.setText("No offline data");
                    day2.setText("N/A");
                    day3.setText("N/A");
                    day4.setText("N/A");
                    day5.setText("N/A");
                }
        );
    }
}
