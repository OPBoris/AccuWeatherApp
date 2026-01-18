package server_client.exceptions;

public class WeatherAppException extends Exception {
    public WeatherAppException(String message) {
        super(message);
    }

    public WeatherAppException(String message, Throwable cause) {
        super(message, cause);
    }
}
