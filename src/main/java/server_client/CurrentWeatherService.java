package server_client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service for managing current weather data
 * Handles current weather data processing from Open-Meteo API
 */
public class CurrentWeatherService {

    /**
     * Process current weather data from Open-Meteo API response
     *
     * @param data JSON response from Open-Meteo API
     * @param unit "C" or "F"
     * @param showFeelsLike Show "Feels like" temperature
     * @param showHumidity Show humidity
     * @param showWind Show wind speed
     * @return Formatted weather string
     */
    public String processCurrentWeather(JsonNode data, String unit,
                                        boolean showFeelsLike, boolean showHumidity, boolean showWind) {
        if (data == null || !data.has("current")) {
            return "ERROR: Unable to fetch weather data.";
        }

        StringBuilder sb = new StringBuilder();

        // Extract current weather data from Open-Meteo format
        JsonNode current = data.get("current");

        double temp = current.get("temperature_2m").asDouble();
        double feelsLike = current.get("apparent_temperature").asDouble();
        int humidity = current.get("relative_humidity_2m").asInt();
        double windSpeed = current.get("wind_speed_10m").asDouble();
        int weatherCode = current.get("weather_code").asInt();

        // Decode weather code
        String weatherDescription = WeatherCodeDecoder.decode(weatherCode);

        // Build current weather output
        sb.append("CURRENT WEATHER:\n");
        sb.append(String.format("Temperature: %.1f°%s\n", temp, unit));

        // Optional fields based on filters
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

