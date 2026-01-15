package fhtw.accuweatherapp.handler;
import fhtw.accuweatherapp.Callback;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import server_client.ClientConnection;




public class UIOfflineHandler {

    private final ClientConnection connection;

    public UIOfflineHandler(ClientConnection connection) {
        this.connection = connection;
    }


    public void saveOfflineData(String city, String unit, Callback<String> onSuccess,
                                Callback<String> onError) {
        if (city == null || city.trim().length() < 3) {
            onError.call("Please enter a valid city name to download offline data.");
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
                onSuccess.call(resp);
            } else {
                onError.call("No response from server.");
            }
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            String errorMsg;
            if (exception != null) {
                errorMsg = "Error saving offline data: " + exception.getMessage();
            } else {
                errorMsg = "Error saving offline data: Unknown error";
            }
            onError.call(errorMsg);
        });

        new Thread(task, "save-offline-call").start();
    }


    public void loadOfflineCurrentWeather(Callback<String> onSuccess, Callback<String> onError) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand("LOAD_OFFLINE");
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if (resp != null && !resp.isEmpty()) {
                onSuccess.call(resp);
            } else {
                onError.call("No offline data available.");
            }
        });

        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            String errorMsg;
            if (exception != null) {
                errorMsg = "Error loading offline data: " + exception.getMessage();
            } else {
                errorMsg = "Error loading offline data: Unknown error";
            }
            onError.call(errorMsg);
        });

        new Thread(task, "load-offline-call").start();
    }

    public void loadAllOfflineData(Callback<String> onCurrentWeather,
                                   Callback<String[]> onForecast,
                                   TextArea day1, TextArea day2, TextArea day3,
                                   TextArea day4, TextArea day5) {

        loadOfflineCurrentWeather(onCurrentWeather, data -> {
        });
        day1.setText("No offline\nforecast data\navailable.");
        day2.setText("");
        day3.setText("");
        day4.setText("");
        day5.setText("");
    }
}
