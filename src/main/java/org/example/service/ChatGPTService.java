package org.example.service;

import com.google.gson.*;
import org.example.model.SurveyQuestion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * שירות לאינטגרציה עם ChatGPT API ליצירת שאלות סקר אוטומטיות.
 */
public class ChatGPTService {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";

    private final String apiKey;
    private final HttpClient httpClient;

    public ChatGPTService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * מייצר שאלות סקר על בסיס נושא נתון.
     *
     * @param topic       נושא הסקר
     * @param numQuestions מספר השאלות (1-3)
     * @return רשימת שאלות סקר
     * @throws Exception אם הקריאה ל-API נכשלה
     */
    public List<SurveyQuestion> generateSurvey(String topic, int numQuestions) throws Exception {
        String prompt = buildPrompt(topic, numQuestions);

        String requestBody = buildRequestBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("ChatGPT API error (HTTP " + response.statusCode() + "): " + response.body());
        }

        return parseResponse(response.body());
    }

    private String buildPrompt(String topic, int numQuestions) {
        return """
                צור סקר בעברית בנושא: "%s"
                
                יש ליצור בדיוק %d שאלות.
                לכל שאלה יש ליצור בין 2 ל-4 אפשרויות תשובה.
                
                החזר את התוצאה בפורמט JSON בלבד, ללא טקסט נוסף:
                {
                  "questions": [
                    {
                      "text": "נוסח השאלה",
                      "options": ["אפשרות 1", "אפשרות 2", "אפשרות 3"]
                    }
                  ]
                }
                """.formatted(topic, numQuestions);
    }

    private String buildRequestBody(String prompt) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.add("messages", messages);
        body.addProperty("temperature", 0.7);

        return body.toString();
    }

    private List<SurveyQuestion> parseResponse(String responseBody) {
        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray choices = responseJson.getAsJsonArray("choices");

        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("לא התקבלה תשובה מ-ChatGPT");
        }

        String content = choices.get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        // נקה את התשובה מסימוני markdown אם יש
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        }
        if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        content = content.trim();

        JsonObject surveyJson = JsonParser.parseString(content).getAsJsonObject();
        JsonArray questionsArray = surveyJson.getAsJsonArray("questions");

        List<SurveyQuestion> questions = new ArrayList<>();
        for (int i = 0; i < questionsArray.size(); i++) {
            JsonObject q = questionsArray.get(i).getAsJsonObject();
            String text = q.get("text").getAsString();
            JsonArray optionsArray = q.getAsJsonArray("options");

            List<String> options = new ArrayList<>();
            for (JsonElement opt : optionsArray) {
                options.add(opt.getAsString());
            }

            questions.add(new SurveyQuestion(i, text, options));
        }

        return questions;
    }

    /**
     * בודק אם ה-API key הוגדר.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_OPENAI_API_KEY_HERE");
    }
}
