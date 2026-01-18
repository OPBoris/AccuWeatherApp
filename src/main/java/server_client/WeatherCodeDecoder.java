package server_client;

public class WeatherCodeDecoder {

    public static String decode(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45 -> "Foggy";
            case 48 -> "Depositing rime fog";
            case 51 -> "Light drizzle";
            case 53 -> "Moderate drizzle";
            case 55 -> "Dense drizzle";
            case 61 -> "Slight rain";
            case 63 -> "Moderate rain";
            case 65 -> "Heavy rain";
            case 71 -> "Slight snow";
            case 73 -> "Moderate snow";
            case 75 -> "Heavy snow";
            case 80 -> "Slight rain showers";
            case 81 -> "Moderate rain showers";
            case 82 -> "Violent rain showers";
            case 85 -> "Slight snow showers";
            case 86 -> "Heavy snow showers";
            case 95 -> "Thunderstorm";
            case 96 -> "Thunderstorm with slight hail";
            case 99 -> "Thunderstorm with heavy hail";
            default -> "Unknown (" + code + ")";
        };
    }
}
