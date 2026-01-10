package server_client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Service for managing weather forecasts
 * Handles 5-day forecast data processing from Open-Meteo API
 */
public class ForecastService {

    /**
     * Get 5-day forecast using Open-Meteo API
     * Returns forecast for the next 5 days
     *
     * @param forecastData Forecast JSON data from Open-Meteo API
     * @param unit "C" or "F" (temperature already converted by API)
     * @param showFeelsLike Show "Feels like" temperature (not available in daily forecast)
     * @param showHumidity Show humidity (not available in daily forecast)
     * @param showWind Show wind speed
     * @return Array of 5 daily forecasts separated by "|||"
     */
    public String processForecast(JsonNode forecastData, String unit,
                                  boolean showFeelsLike, boolean showHumidity, boolean showWind) {
        try {
            if (forecastData == null || !forecastData.has("daily")) {
                return "ERROR: Unable to fetch forecast data.";
            }

            StringBuilder result = new StringBuilder();
            JsonNode daily = forecastData.get("daily");

            JsonNode times = daily.get("time");
            JsonNode tempMax = daily.get("temperature_2m_max");
            JsonNode tempMin = daily.get("temperature_2m_min");
            JsonNode weatherCodes = daily.get("weather_code");
            JsonNode precipProb = daily.get("precipitation_probability_max");
            JsonNode windSpeed = daily.get("wind_speed_10m_max");

            int daysCount = Math.min(5, times.size());

            for (int dayIndex = 0; dayIndex < daysCount; dayIndex++) {
                String dateStr = times.get(dayIndex).asText();
                double tempMaxVal = tempMax.get(dayIndex).asDouble();
                double tempMinVal = tempMin.get(dayIndex).asDouble();
                int weatherCode = weatherCodes.get(dayIndex).asInt();
                int rainProbability = precipProb.get(dayIndex).asInt();
                double windSpeedVal = windSpeed.get(dayIndex).asDouble();

                // Decode weather code
                String weatherDescription = WeatherCodeDecoder.decode(weatherCode);

                // Build forecast string for this day
                StringBuilder dayForecast = new StringBuilder();
                dayForecast.append(dateStr).append("\n");
                dayForecast.append("Max: ").append(String.format("%.1f", tempMaxVal)).append("°").append(unit).append("\n");
                dayForecast.append("Min: ").append(String.format("%.1f", tempMinVal)).append("°").append(unit).append("\n");
                dayForecast.append("Weather: ").append(weatherDescription).append("\n");

                if (showWind) {
                    dayForecast.append("Wind: ").append(String.format("%.1f", windSpeedVal)).append(" km/h\n");
                }

                dayForecast.append("Rain: ").append(rainProbability).append("%");

                if (dayIndex > 0) {
                    result.append("|||");
                }
                result.append(dayForecast);
            }

            return result.toString();
        } catch (Exception e) {
            System.err.println("Error processing forecast: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}

