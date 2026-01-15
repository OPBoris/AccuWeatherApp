package server_client.services;

import com.fasterxml.jackson.databind.JsonNode;
import server_client.ApiClient;
import server_client.WeatherCodeDecoder;


public class CurrentWeatherService {
    private static final String CURRENT_WEATHER_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&temperature_unit=%s&timezone=auto";

    private final ApiClient apiClient;

    public CurrentWeatherService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }


    public String getCurrentWeather(double lat, double lon, String unit,
                                    boolean showHumidity, boolean showWind, boolean showFeelsLike) {
        try {
            String tempUnit;
            if (unit.equalsIgnoreCase("F")) {
                tempUnit = "fahrenheit";
            } else {
                tempUnit = "celsius";
            }
            String url = String.format(CURRENT_WEATHER_URL, lat, lon, tempUnit);
            JsonNode data = apiClient.makeOpenMeteoCall(url);
            return processCurrentWeather(data, unit, showFeelsLike, showHumidity, showWind);
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }


    private String processCurrentWeather(JsonNode data, String unit,
                                         boolean showFeelsLike, boolean showHumidity, boolean showWind) {
        if (data == null || !data.has("current")) {
            return "ERROR: Unable to fetch weather data.";
        }

        StringBuilder sb = new StringBuilder();


        JsonNode current = data.get("current");

        double temp = current.get("temperature_2m").asDouble();
        double feelsLike = current.get("apparent_temperature").asDouble();
        int humidity = current.get("relative_humidity_2m").asInt();
        double windSpeed = current.get("wind_speed_10m").asDouble();
        int weatherCode = current.get("weather_code").asInt();


        String weatherDescription = WeatherCodeDecoder.decode(weatherCode);


        sb.append("CURRENT WEATHER:\n");
        sb.append(String.format("Temperature: %.1f°%s\n", temp, unit));


        if (showFeelsLike) {
            sb.append(String.format("Feels like: %.1f°%s\n", feelsLike, unit));
        }

        sb.append("Weather: ").append(weatherDescription).append("\n");

        if (showHumidity) {
            sb.append(String.format("Humidity: %d%%\n", humidity));
        }

        if (showWind) {
            sb.append(String.format("Wind Speed: %.1f km/h\n", windSpeed));
        }

        return sb.toString();
    }
}

