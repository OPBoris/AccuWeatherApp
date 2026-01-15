package server_client;

import java.util.ArrayList;
import java.util.List;


public class JsonParser {

    public static int findMatchingBrace(String json, int start) {
        int count = 1;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') count++;
            else if (c == '}') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    public static String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int valueStart = keyIndex + searchKey.length();
        while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '\t')) {
            valueStart++;
        }

        if (json.charAt(valueStart) == '"') {
            valueStart++;
            int valueEnd = json.indexOf("\"", valueStart);
            if (valueEnd == -1) return null;
            return json.substring(valueStart, valueEnd);
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length() &&
                    json.charAt(valueEnd) != ',' &&
                    json.charAt(valueEnd) != '}' &&
                    json.charAt(valueEnd) != ']') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    public static double parseDoubleValue(String json, String key) {
        String value = extractValue(json, key);
        return value != null ? Double.parseDouble(value) : 0.0;
    }

    public static int parseIntValue(String json, String key) {
        String value = extractValue(json, key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    public static List<String> parseArrayValues(String json, String key) {
        List<String> values = new ArrayList<>();
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return values;

        int arrayStart = json.indexOf("[", keyIndex);
        if (arrayStart == -1) return values;

        int arrayEnd = json.indexOf("]", arrayStart);
        if (arrayEnd == -1) return values;

        String arrayContent = json.substring(arrayStart + 1, arrayEnd);

        StringBuilder currentValue = new StringBuilder();
        boolean inString = false;

        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);

            if (c == '"') {
                inString = !inString;
                currentValue.append(c);
            } else if (c == ',' && !inString) {
                String val = currentValue.toString().trim();
                if (!val.isEmpty()) {
                    values.add(val);
                }
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }

        String val = currentValue.toString().trim();
        if (!val.isEmpty()) {
            values.add(val);
        }

        return values;
    }
}

