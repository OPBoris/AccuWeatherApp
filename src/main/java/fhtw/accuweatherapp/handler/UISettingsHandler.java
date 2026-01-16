package fhtw.accuweatherapp.handler;

import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import server_client.ClientConnection;
import fhtw.accuweatherapp.Callback;



public class UISettingsHandler {

    private final ClientConnection connection;

    private String unit = "C";
    private boolean humidityChecked = false;
    private boolean windChecked = false;
    private boolean feelsLikeChecked = false;
    private boolean isDarkMode = false;
    private boolean historyChecked = false;

    public UISettingsHandler(ClientConnection connection) {
        this.connection = connection;
    }


    public String getUnit() {
        return unit;
    }

    public boolean isHumidityChecked() {
        return humidityChecked;
    }

    public boolean isWindChecked() {
        return windChecked;
    }

    public boolean isFeelsLikeChecked() {
        return feelsLikeChecked;
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    public boolean isHistoryChecked() {
        return historyChecked;
    }


    public void setUnit(String unit) {
        this.unit = unit;
    }


    public void setUnitCelsius(Callback<String> onResponse) {
        unit = "C";
        sendCommand("SET_UNIT C", onResponse);
    }


    public void setUnitFahrenheit(Callback<String> onResponse) {
        unit = "F";
        sendCommand("SET_UNIT F", onResponse);
    }


    public void toggleFeelsLike(Runnable onComplete) {
        feelsLikeChecked = !feelsLikeChecked;
        sendCommandAsync("CHECK_FEELS_LIKE", onComplete);
    }


    public void toggleHumidity(Runnable onComplete) {
        humidityChecked = !humidityChecked;
        sendCommandAsync("CHECK_HUMIDITY", onComplete);
    }


    public void toggleWind(Runnable onComplete) {
        windChecked = !windChecked;
        sendCommandAsync("CHECK_WIND", onComplete);
    }


    public void toggleHistoryMode(Callback<String> onMessage) {
        historyChecked = !historyChecked;
        if (historyChecked) {
            onMessage.call("History mode enabled. Click Search to view historical data.");
        } else {
            onMessage.call("Forecast mode enabled. Click Search to view forecast.");
        }
    }


    public void toggleDarkMode(BorderPane mainPane, Button btnUiMode, TextField txtCity,
                               Callback<String> onMessage) {
        isDarkMode = !isDarkMode;

        if (isDarkMode) {
            if (mainPane != null) {
                mainPane.setStyle("-fx-base: #2b2b2b; -fx-control-inner-background: #2b2b2b; -fx-background: #2b2b2b;");
            }
            if (btnUiMode != null) {
                btnUiMode.setText("Light");
                btnUiMode.setStyle("-fx-background-color: white; -fx-text-fill: black;");
            }
            if (txtCity != null) {
                txtCity.setStyle("-fx-text-fill: white; -fx-prompt-text-fill: #ffffff;");
            }
            onMessage.call("Dark Mode activated.");
        } else {
            if (mainPane != null) {
                mainPane.setStyle("");
            }
            if (btnUiMode != null) {
                btnUiMode.setText("Dark");
                btnUiMode.setStyle("-fx-background-color: black; -fx-text-fill: white;");
            }
            if (txtCity != null) {
                txtCity.setStyle("");
            }
            onMessage.call("Light Mode activated.");
        }
    }


    public void setStandardCity(String city, Callback<String> onResponse, Callback<String> onError) {
        if (city == null || city.trim().isEmpty()) {
            onError.call("Please enter a city.");
            return;
        }
        sendCommand("SET_STANDARD " + city.trim(), onResponse);
    }


    public void applySettings(boolean showHumidity, boolean showWind, boolean showFeelsLike,
                              String loadedUnit, CheckBox checkHumidity, CheckBox checkWind,
                              CheckBox checkFeelsLike) {
        this.humidityChecked = showHumidity;
        this.windChecked = showWind;
        this.feelsLikeChecked = showFeelsLike;
        this.unit = loadedUnit;

        if (checkHumidity != null) checkHumidity.setSelected(showHumidity);
        if (checkWind != null) checkWind.setSelected(showWind);
        if (checkFeelsLike != null) checkFeelsLike.setSelected(showFeelsLike);
    }

    private void sendCommand(String command, Callback<String> onResponse) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand(command);
            }
        };

        task.setOnSucceeded(e -> {
            if (onResponse != null) {
                onResponse.call(task.getValue());
            }
        });

        task.setOnFailed(e -> {
            if (onResponse != null) {
                Throwable ex = task.getException();
                String errorMsg;
                if (ex != null) {
                    errorMsg = "Error: " + ex.getMessage();
                } else {
                    errorMsg = "Unknown error";
                }
                onResponse.call(errorMsg);
            }
        });

        new Thread(task, "settings-cmd").start();
    }

    private void sendCommandAsync(String command, Runnable onComplete) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand(command);
            }
        };

        task.setOnSucceeded(e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });

        task.setOnFailed(e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });

        new Thread(task, "settings-async-cmd").start();
    }
}
