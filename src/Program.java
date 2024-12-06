import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Program {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        // Load the API key from .env file
        String apiKey = loadApiKeyFromEnv();

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("API key not found in .env file!");
            return;
        }

        System.out.println("Enter your prompt: ");
        String prompt = in.nextLine();

        // Call Gemini API and get response
        String response = callGeminiAPI(apiKey, prompt);

        if (response != null) {
            // Extract text from JSON response
            String extractedText = extractTextFromResponse(response);

            System.out.println("Response from Gemini: ");
            System.out.println(extractedText);

            // Prompt for file save location
            System.out.println("Enter file path to save the response: ");
            String outputFilePath = in.nextLine();

            // Save response to file
            saveResponseToFile(extractedText, outputFilePath);
        } else {
            System.out.println("Error: Failed to get a response from the API.");
        }

        in.close();
    }

    // Extract text from JSON response
    private static String extractTextFromResponse(String response) {
        int startIndex = response.indexOf("\"text\":\"") + 8;
        int endIndex = response.indexOf("\"", startIndex);

        if (startIndex > 7 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex)
                    .replace("\\n", "\n")  // Unescape newline characters
                    .replace("\\\"", "\""); // Unescape quotes
        }

        return response; // Fallback to original response if extraction fails
    }
    // Load API key from .env file
    private static String loadApiKeyFromEnv() {
        try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("GEMINI_API_KEY=")) {
                    return line.split("=")[1].trim();
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading .env file: " + e.getMessage());
        }
        return null;
    }

    // Call Gemini API
    private static String callGeminiAPI(String apiKey, String prompt) {
        String urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey;

        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt + "\"}]}]}";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read API response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                // Print error details if request fails
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
                    StringBuilder error = new StringBuilder();
                    String errorLine;
                    while ((errorLine = br.readLine()) != null) {
                        error.append(errorLine.trim());
                    }
                    System.out.println("API Error: " + error.toString());
                }
            }
        } catch (IOException e) {
            System.out.println("Error while calling Gemini API: " + e.getMessage());
        }
        return null;
    }

    // Save response to file
    private static void saveResponseToFile(String response, String outputFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write(response);
            writer.newLine();
            System.out.println("Response saved to " + outputFilePath);
        } catch (IOException e) {
            System.out.println("Error while saving file: " + e.getMessage());
        }
    }
}