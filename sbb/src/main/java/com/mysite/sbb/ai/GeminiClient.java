package com.mysite.sbb.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GeminiClient {

  private final WebClient webClient;
  private final ObjectMapper om = new ObjectMapper();

  @Value("${gemini.api-key}")
  private String apiKey;

  public GeminiClient(WebClient.Builder builder) {
    this.webClient = builder
        .baseUrl("https://generativelanguage.googleapis.com")
        .build();
  }

  public String generateJson(String model, String prompt, double temperature, int maxOutputTokens) {
    try {
      // Gemini generateContent 요청 바디(텍스트 프롬프트)
      System.out.println("[Gemini] called");
      var body = om.createObjectNode();
      body.putArray("contents")
          .addObject()
          .put("role", "user")
          .putArray("parts")
          .addObject()
          .put("text", prompt);

      var gen = body.putObject("generationConfig");
      gen.put("temperature", temperature);
      gen.put("maxOutputTokens", maxOutputTokens);

      String res = webClient.post()
          .uri("/v1beta/models/" + model + ":generateContent")
          .contentType(MediaType.APPLICATION_JSON)
          .header("x-goog-api-key", apiKey)
          .bodyValue(body)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      JsonNode root = om.readTree(res);
      JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
      return text.isMissingNode() ? "" : text.asText("");

    } catch (Exception e) {
      throw new RuntimeException("Gemini call failed", e);
    }
  }
}