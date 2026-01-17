package server_client.api;

public class ApiUrls {


    public static final String CURRENT_WEATHER =
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&temperature_unit=%s&timezone=auto";

    public static final String FORECAST =
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&daily=temperature_2m_max,temperature_2m_min,apparent_temperature_max,relative_humidity_2m_max,weather_code,precipitation_probability_max,wind_speed_10m_max&temperature_unit=%s&timezone=auto&forecast_days=5";

    public static final String HISTORICAL =
            "https://archive-api.open-meteo.com/v1/archive?latitude=%s&longitude=%s&start_date=%s&end_date=%s&daily=temperature_2m_max,temperature_2m_min,temperature_2m_mean,precipitation_sum,rain_sum,windspeed_10m_max,weathercode&timezone=auto";


    public static final String GEOCODING =
            "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=en&format=json";

    private ApiUrls() {
    }
}

