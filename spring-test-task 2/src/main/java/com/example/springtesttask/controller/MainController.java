package com.example.springtesttask.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

// This class represents the main controller for the Spring application
@RestController
// Allows CORS
@CrossOrigin(origins = "http://localhost:4200")
public class MainController {

    private static final String AGIFY_API_URL = "https://api.agify.io/";
    // A map that keeps track of the number of requests made for each name
    private static final Map<String, Integer> nameRequestCounts = new HashMap<>();

    // Handles GET requests to the root path
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAge(@RequestParam(name="name", defaultValue = "No Name") String name) {
        // Get age from the local text file
        String age = getAgeFromTextFile(name);
        if (age != null) {
            incrementNameRequestCount(name);
            // Create a response map with the name and age (like Json)
            Map<String, Object> response = new HashMap<>();
            response.put("name", name);
            response.put("age", age);
            // Return the response with a status of 200 (OK)
            return ResponseEntity.ok(response);
        } else { // If age is not found in the local text file
            try {
                incrementNameRequestCount(name);
                // Get age from the Agify API
                age = getAgeFromAgifyAPI(name);
                Map<String, Object> response = new HashMap<>();
                response.put("name", name);
                response.put("age", age);
                return ResponseEntity.ok(response);
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(null);
            }
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Integer>> getStats() {
        // Return a response with the name request counts map and a status of 200 (OK)
        return ResponseEntity.ok(nameRequestCounts);
    }

    @GetMapping("/max-age-name")
    public ResponseEntity<Map<String, Integer>> getMaxAgeName() {
        String maxAgeName = null;
        int maxAge = Integer.MIN_VALUE;
        // Iterate through the name request counts map
        for (Map.Entry<String, Integer> entry : nameRequestCounts.entrySet()) {
            String name = entry.getKey();
            int requestCount = entry.getValue();
            // Try to get the age from the text file first
            String ageStr = getAgeFromTextFile(name);
            // If the age is not in the text file, try to get it from the Agify API
            if (ageStr == null) {
                try {
                    ageStr = getAgeFromAgifyAPI(name);
                } catch (IOException e) {
                    // ignore and move on to the next name
                    continue;
                }
            }
            int age = Integer.parseInt(ageStr);
            // Check if the current age is the maximum so far
            if (age > maxAge) {
                maxAge = age;
                maxAgeName = name;
            }
        }
        // Create a map to store the response
        Map<String, Integer> response = new HashMap<>();
        if (maxAgeName != null) {
            response.put("age", maxAge);
        } else {
            response.put("age", 0);
        }
        return ResponseEntity.ok(response);
    }

    // Method to get the age from the text file
    private String getAgeFromTextFile(String name) {
        String fileName = "name_age.txt";

        // Get the path of the text file
        Path currentDir = Paths.get("").toAbsolutePath();
        Path srcDir = currentDir.resolve("src").resolve("main").resolve("java").resolve("com").resolve("example").resolve("springtesttask");
        Path txtFilePath = srcDir.resolve("name_age.txt").toAbsolutePath();

        try {
            // Read all the lines from the text file
            List<String> lines = Files.readAllLines(txtFilePath);
            // Search for the line that matches the name
            for (String line : lines) {
                String[] parts = line.split("_");
                if (parts[0].equalsIgnoreCase(name)) {
                    return parts[1];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Method to get the age from the Agify API
    private String getAgeFromAgifyAPI(String name) throws IOException {
        String apiUrl = AGIFY_API_URL + "?name=" + name;
        // Connect to the Agify API
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream inputStream = connection.getInputStream()) {
                // Parse the response as a map
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> result = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>(){});
                // Check if the age is in the response
                if (result.get("age") != null) {
                    return result.get("age").toString();
                } else {
                    throw new IOException("Failed to get age from agify.io API for " + name + ", response code: " + responseCode);
                }
            }
        } else {
            throw new IOException("Failed to get age from agify.io API for " + name + ", response code: " + responseCode);
        }
    }

    // This method takes a name and increments its request count in the nameRequestCounts map.
    private void incrementNameRequestCount(String name) {
        // Convert the name to a standard format (f.e. Name, instead of name or nAME)
        name = name.substring(0,1).toUpperCase() + name.substring(1).toLowerCase();
        int count = nameRequestCounts.getOrDefault(name, 0);
        nameRequestCounts.put(name, count + 1);
    }

}
