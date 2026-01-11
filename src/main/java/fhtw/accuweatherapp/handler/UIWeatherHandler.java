package fhtw.accuweatherapp.handler;

import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import server_client.ClientConnection;

import java.util.function.Consumer;


public class UIWeatherHandler {

    private final ClientConnection connection;

    public UIWeatherHandler(ClientConnection connection) {
        this.connection = connection;
    }


    public String validateCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return "Please enter a city.";
        }

        String trimmedCity = city.trim();

        if (trimmedCity.length() < 3) {
            return "Please type in at least 3 characters.";
        }

        if (!trimmedCity.matches("[a-zA-ZäöüÄÖÜß\\s\\-,]+")) {
            return "Please type only letters (no numbers or special characters).";
        }

        return null;
    }


    public void loadCurrentWeather(String city, String unit, boolean feelsLikeChecked,
                                   boolean humidityChecked, boolean windChecked,
                                   Consumer<String> onSuccess, Consumer<String> onError) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                StringBuilder cmd = new StringBuilder("GET_WEATHER ");
                cmd.append(city).append(" ").append(unit);
                cmd.append(" FEELS_LIKE=").append(feelsLikeChecked);
                cmd.append(" HUMIDITY=").append(humidityChecked);
                cmd.append(" WIND=").append(windChecked);
                return connection.sendCommand(cmd.toString());
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
                    ? "Connection error: " + exception.getMessage()
                    : "Connection error: Unknown error";
            onError.accept(errorMsg);
        });

        new Thread(task, "current-weather-call").start();
    }


    public void loadForecastOrHistory(String city, String unit, boolean historyChecked,
                                      boolean feelsLikeChecked, boolean humidityChecked,
                                      boolean windChecked, Consumer<String[]> onSuccess,
                                      Consumer<String> onError) {
        String command = historyChecked ? "GET_HISTORICAL " : "GET_FORECAST ";
        String dataType = historyChecked ? "historical data" : "forecast";

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                StringBuilder cmd = new StringBuilder(command);
                cmd.append(city).append(" ").append(unit);

                if (!historyChecked) {
                    cmd.append(" FEELS_LIKE=").append(feelsLikeChecked);
                    cmd.append(" HUMIDITY=").append(humidityChecked);
                    cmd.append(" WIND=").append(windChecked);
                }

                return connection.sendCommand(cmd.toString());
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if (resp != null && !resp.isEmpty() && !resp.startsWith("ERROR")) {
                String[] dataArray = resp.split("\\|\\|\\|");
                onSuccess.accept(dataArray);
            } else {
                String errorMsg = resp != null ? resp : "No " + dataType + " available";
                onError.accept(errorMsg);
            }
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            String errorMsg = exception != null ? "Error: " + exception.getMessage() : "Unknown error";
            onError.accept(errorMsg);
        });

        new Thread(task, dataType + "-call").start();
    }


    public void populateForecastFields(String[] dataArray, TextArea day1, TextArea day2,
                                       TextArea day3, TextArea day4, TextArea day5) {
        if (dataArray.length > 0) day1.setText(dataArray[0]);
        if (dataArray.length > 1) day2.setText(dataArray[1]);
        if (dataArray.length > 2) day3.setText(dataArray[2]);
        if (dataArray.length > 3) day4.setText(dataArray[3]);
        if (dataArray.length > 4) day5.setText(dataArray[4]);

        if (dataArray.length < 5) day5.setText("No data");
        if (dataArray.length < 4) day4.setText("No data");
        if (dataArray.length < 3) day3.setText("No data");
        if (dataArray.length < 2) day2.setText("No data");
        if (dataArray.length < 1) day1.setText("No data");
    }


    public void setForecastError(String errorMsg, TextArea day1, TextArea day2,
                                 TextArea day3, TextArea day4, TextArea day5) {
        day1.setText(errorMsg);
        day2.setText("N/A");
        day3.setText("N/A");
        day4.setText("N/A");
        day5.setText("N/A");
    }


    public void setForecastLoading(TextArea day1, TextArea day2, TextArea day3,
                                   TextArea day4, TextArea day5) {
        day1.setText("Loading...");
        day2.setText("Loading...");
        day3.setText("Loading...");
        day4.setText("Loading...");
        day5.setText("Loading...");
    }
}
