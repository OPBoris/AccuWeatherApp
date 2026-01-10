package fhtw.accuweatherapp;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import server_client.ClientConnection;

public class UIController {
    @FXML
    private TextField txt_field_city;

    @FXML
    private TextArea txt_field_cur_weather;

    @FXML
    private TextArea txt_field_day1;

    @FXML
    private TextArea txt_field_day2;

    @FXML
    private TextArea txt_field_day3;

    @FXML
    private TextArea txt_field_day4;

    @FXML
    private TextArea txt_field_day5;

    @FXML
    private javafx.scene.control.CheckBox check_history;

    private String unit = "C";
    private boolean humidityChecked = false;
    private boolean windChecked = false;
    private boolean feelsLikeChecked = false;
    private boolean historyChecked = false;

    private final ClientConnection connection = new ClientConnection("localhost", 8080);

    @FXML
    protected void onSafeFavourite() {
        txt_field_cur_weather.setText("Pressed on Favourite.");
    }

    @FXML
    protected void onMoritz() {
        runCommand("MORITZ");
    }

    @FXML
    protected void onJan() {
        runCommand("JAN");
    }

    @FXML
    protected void onBoris() {
        runCommand("BORIS");
    }

    @FXML
    protected void onSearch() {
        String city = txt_field_city.getText();

        // Check if input is empty
        if (city == null || city.trim().isEmpty()) {
            txt_field_cur_weather.setText("Please enter a city.");
            return;
        }

        String trimmedCity = city.trim();

        // Check minimum length (at least 3 characters)
        if (trimmedCity.length() < 3) {
            txt_field_cur_weather.setText("Please type in at least 3 characters.");
            return;
        }

        // Check if input contains only valid characters (letters, spaces, hyphens, commas)
        if (!trimmedCity.matches("[a-zA-ZäöüÄÖÜß\\s\\-,]+")) {
            txt_field_cur_weather.setText("Please type only letters (no numbers or special characters).");
            return;
        }

        txt_field_cur_weather.setText("Loading weather data for: " + trimmedCity + "...");

        // Clear forecast fields
        txt_field_day1.setText("Loading...");
        txt_field_day2.setText("Loading...");
        txt_field_day3.setText("Loading...");
        txt_field_day4.setText("Loading...");
        txt_field_day5.setText("Loading...");

        // Load current weather
        loadCurrentWeather(trimmedCity);

        // Load 5-day forecast OR historical data (depending on History checkbox)
        loadForecastOrHistory(trimmedCity);
    }

    /**
     * Load current weather for a city
     */
    private void loadCurrentWeather(String city) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                // Build command with filter flags
                StringBuilder cmd = new StringBuilder("GET_WEATHER ");
                cmd.append(city).append(" ").append(unit);

                // Add filter flags
                cmd.append(" FEELS_LIKE=").append(feelsLikeChecked);
                cmd.append(" HUMIDITY=").append(humidityChecked);
                cmd.append(" WIND=").append(windChecked);

                return connection.sendCommand(cmd.toString());
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if (resp != null && !resp.isEmpty()) {
                txt_field_cur_weather.setText(resp);
            } else {
                txt_field_cur_weather.setText("No response from server.");
            }
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            if (exception != null) {
                txt_field_cur_weather.setText("Connection error: " + exception.getMessage());
            } else {
                txt_field_cur_weather.setText("Connection error: Unknown error");
            }
        });

        new Thread(task, "current-weather-call").start();
    }

    /**
     * Load 5-day forecast OR 5-day historical data for a city
     * Decision based on historyChecked flag
     */
    private void loadForecastOrHistory(String city) {
        // Determine which command to use
        String command = historyChecked ? "GET_HISTORICAL " : "GET_FORECAST ";
        String dataType = historyChecked ? "historical data" : "forecast";

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                // Build command with filter flags
                StringBuilder cmd = new StringBuilder(command);
                cmd.append(city).append(" ").append(unit);

                // Add filter flags (only for forecast, not for historical data)
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
                // Split data by "|||"
                String[] dataArray = resp.split("\\|\\|\\|");

                // Display each day in corresponding TextArea
                if (dataArray.length > 0) txt_field_day1.setText(dataArray[0]);
                if (dataArray.length > 1) txt_field_day2.setText(dataArray[1]);
                if (dataArray.length > 2) txt_field_day3.setText(dataArray[2]);
                if (dataArray.length > 3) txt_field_day4.setText(dataArray[3]);
                if (dataArray.length > 4) txt_field_day5.setText(dataArray[4]);

                // Clear remaining fields if less than 5 days available
                if (dataArray.length < 5) txt_field_day5.setText("No data");
                if (dataArray.length < 4) txt_field_day4.setText("No data");
                if (dataArray.length < 3) txt_field_day3.setText("No data");
                if (dataArray.length < 2) txt_field_day2.setText("No data");
                if (dataArray.length < 1) txt_field_day1.setText("No data");
            } else {
                // Error occurred
                String errorMsg = resp != null ? resp : "No " + dataType + " available";
                txt_field_day1.setText(errorMsg);
                txt_field_day2.setText("N/A");
                txt_field_day3.setText("N/A");
                txt_field_day4.setText("N/A");
                txt_field_day5.setText("N/A");
            }
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            String errorMsg = exception != null ? "Error: " + exception.getMessage() : "Unknown error";
            txt_field_day1.setText(errorMsg);
            txt_field_day2.setText("N/A");
            txt_field_day3.setText("N/A");
            txt_field_day4.setText("N/A");
            txt_field_day5.setText("N/A");
        });

        new Thread(task, dataType + "-call").start();
    }

    @FXML
    protected void onReport() {
        // If History checkbox is checked, export 30-day CSV
        if (historyChecked) {
            String city = txt_field_city.getText();
            if (city != null && city.trim().length() >= 3) {
                txt_field_cur_weather.setText("Exporting 30-day historical data to CSV...");
                exportHistoricalDataCSV(city.trim());
            } else {
                txt_field_cur_weather.setText("Please enter a city name first to export historical data.");
            }
        } else {
            // Show search history
            runCommand("GET_HISTORY");
        }
    }

    /**
     * Export 30-day historical weather data to CSV file
     */
    private void exportHistoricalDataCSV(String city) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand("EXPORT_HISTORY " + city + " " + unit);
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if (resp != null && !resp.isEmpty()) {
                txt_field_cur_weather.setText(resp);
            } else {
                txt_field_cur_weather.setText("No response from server.");
            }
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            if (exception != null) {
                txt_field_cur_weather.setText("Export error: " + exception.getMessage());
            } else {
                txt_field_cur_weather.setText("Export error: Unknown error");
            }
        });

        new Thread(task, "export-csv-call").start();
    }

    @FXML
    protected void onOffline() {
        String city = txt_field_city.getText();

        // Check if city is entered
        if (city == null || city.trim().isEmpty()) {
            // Try to load existing offline data
            loadOfflineData();
            return;
        }

        String trimmedCity = city.trim();

        // Check minimum length
        if (trimmedCity.length() < 3) {
            txt_field_cur_weather.setText("Please enter a valid city name to download offline data.");
            return;
        }

        // Save offline data for the entered city
        txt_field_cur_weather.setText("Downloading offline data for: " + trimmedCity + "...");
        saveOfflineData(trimmedCity);
    }

    /**
     * Save weather data for offline use
     */
    private void saveOfflineData(String city) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand("SAVE_OFFLINE " + city + " " + unit);
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if (resp != null && !resp.isEmpty()) {
                txt_field_cur_weather.setText(resp);
            } else {
                txt_field_cur_weather.setText("No response from server.");
            }
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            if (exception != null) {
                txt_field_cur_weather.setText("Error saving offline data: " + exception.getMessage());
            } else {
                txt_field_cur_weather.setText("Error saving offline data: Unknown error");
            }
        });

        new Thread(task, "save-offline-call").start();
    }

    /**
     * Load offline weather data from cache
     */
    private void loadOfflineData() {
        txt_field_cur_weather.setText("Loading offline data...");

        // Load current weather
        Task<String> currentTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand("LOAD_OFFLINE");
            }
        };

        currentTask.setOnSucceeded(e -> {
            String resp = currentTask.getValue();
            if (resp != null && !resp.isEmpty()) {
                txt_field_cur_weather.setText(resp);
            } else {
                txt_field_cur_weather.setText("No offline data available.");
            }
        });

        currentTask.setOnFailed(e -> {
            Throwable exception = currentTask.getException();
            if (exception != null) {
                txt_field_cur_weather.setText("Error loading offline data: " + exception.getMessage());
            } else {
                txt_field_cur_weather.setText("Error loading offline data: Unknown error");
            }
        });

        new Thread(currentTask, "load-offline-call").start();

        // Load offline forecast
        Task<String> forecastTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand("GET_OFFLINE_FORECAST");
            }
        };

        forecastTask.setOnSucceeded(e -> {
            String resp = forecastTask.getValue();
            if (resp != null && !resp.isEmpty() && !resp.startsWith("ERROR")) {
                String[] dataArray = resp.split("\\|\\|\\|");

                // Display each time slot in forecast fields
                if (dataArray.length > 0) txt_field_day1.setText(dataArray[0]);
                if (dataArray.length > 1) txt_field_day2.setText(dataArray[1]);
                if (dataArray.length > 2) txt_field_day3.setText(dataArray[2]);
                if (dataArray.length > 3) txt_field_day4.setText(dataArray[3]);
                if (dataArray.length > 4) txt_field_day5.setText(dataArray[4]);

                // Clear remaining fields if less data available
                if (dataArray.length < 5) txt_field_day5.setText("N/A");
                if (dataArray.length < 4) txt_field_day4.setText("N/A");
                if (dataArray.length < 3) txt_field_day3.setText("N/A");
                if (dataArray.length < 2) txt_field_day2.setText("N/A");
                if (dataArray.length < 1) txt_field_day1.setText("N/A");
            } else {
                // No offline forecast available
                txt_field_day1.setText("No offline data");
                txt_field_day2.setText("N/A");
                txt_field_day3.setText("N/A");
                txt_field_day4.setText("N/A");
                txt_field_day5.setText("N/A");
            }
        });

        forecastTask.setOnFailed(e -> {
            txt_field_day1.setText("Offline error");
            txt_field_day2.setText("N/A");
            txt_field_day3.setText("N/A");
            txt_field_day4.setText("N/A");
            txt_field_day5.setText("N/A");
        });

        new Thread(forecastTask, "load-offline-forecast-call").start();
    }

    @FXML
    protected void onUnitC() {
        unit = "C";
        txt_field_cur_weather.setText("Unit switched to °C.");
        // Reload weather data if city is present
        String city = txt_field_city.getText();
        if (city != null && !city.trim().isEmpty() && city.trim().length() >= 3) {
            onSearch();
        }
    }

    @FXML
    protected void onUnitF() {
        unit = "F";
        txt_field_cur_weather.setText("Unit switched to °F.");
        // Reload weather data if city is present
        String city = txt_field_city.getText();
        if (city != null && !city.trim().isEmpty() && city.trim().length() >= 3) {
            onSearch();
        }
    }

    @FXML
    protected void onCheckFeelsLike() {
        feelsLikeChecked = !feelsLikeChecked;
        String city = txt_field_city.getText();
        if (city != null && !city.trim().isEmpty() && city.trim().length() >= 3) {
            onSearch(); // Reload data with new filter
        }
    }

    @FXML
    protected void onCheckHumidity() {
        humidityChecked = !humidityChecked;
        String city = txt_field_city.getText();
        if (city != null && !city.trim().isEmpty() && city.trim().length() >= 3) {
            onSearch(); // Reload data with new filter
        }
    }

    @FXML
    protected void onCheckWind() {
        windChecked = !windChecked;
        String city = txt_field_city.getText();
        if (city != null && !city.trim().isEmpty() && city.trim().length() >= 3) {
            onSearch(); // Reload data with new filter
        }
    }

    @FXML
    protected void onCheckHistory() {
        historyChecked = !historyChecked;
        if (historyChecked) {
            txt_field_cur_weather.setText("History mode enabled. Click Search to view historical data.");
        } else {
            txt_field_cur_weather.setText("Forecast mode enabled. Click Search to view forecast.");
        }

        // Reload data if city is present
        String city = txt_field_city.getText();
        if (city != null && !city.trim().isEmpty() && city.trim().length() >= 3) {
            onSearch();
        }
    }

    private void runCommand(String command) {
        txt_field_cur_weather.setText("Sende: " + command + " ...");
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return connection.sendCommand(command);
            }
        };
        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if(resp != null) {
                txt_field_cur_weather.setText(resp);
            } else {
                txt_field_cur_weather.setText("Keine Antwort vom Server erhalten.");
            }
        });
        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            if (exception != null) {
                txt_field_cur_weather.setText("Verbindungsfehler: " + exception.getMessage());
            } else {
                txt_field_cur_weather.setText("Verbindungsfehler: Unbekannter Fehler");
            }
        });
        new Thread(task, "server-call").start();
    }
}