package com.example.quizgen;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestClient restClient = RestClient.create();

    /**
     * Sends notes to Gemini AI to generate a specific 'count' of MCQs in JSON format.
     */
    @SuppressWarnings("unchecked")
    public String generateQuizJson(String topic, String notes, int count) {
        // We dynamically insert the 'count' parameter into the prompt
        String prompt = "Generate a quiz titled '" + topic + "' based on the following notes.\n" +
                "The quiz must have exactly " + count + " multiple-choice questions.\n" +
                "Each question must have 4 options (A, B, C, D) and a single correct option index (0 = Option A, 1 = Option B, 2 = Option C, 3 = Option D).\n\n" +
                "Notes:\n" + notes + "\n\n" +
                "Return the response strictly as a JSON Array of objects matching this schema:\n" +
                "[\n" +
                "  {\n" +
                "    \"questionText\": \"string\",\n" +
                "    \"optionA\": \"string\",\n" +
                "    \"optionB\": \"string\",\n" +
                "    \"optionC\": \"string\",\n" +
                "    \"optionD\": \"string\",\n" +
                "    \"correctOptionIndex\": integer\n" +
                "  }\n" +
                "]";

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        try {
            String urlWithKey = apiUrl + "?key=" + apiKey;

            Map<String, Object> response = restClient.post()
                    .uri(urlWithKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
            throw new RuntimeException("Empty response received from Gemini API");
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }
}