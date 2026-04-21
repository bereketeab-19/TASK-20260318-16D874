package com.petsupplies;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AchievementValidationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void missing_responsible_person_or_conclusion_fails() throws Exception {
    var body = new java.util.HashMap<String, Object>();
    body.put("userId", 1);
    body.put("title", "T");
    body.put("period", "2026-Q1");
    body.put("responsiblePerson", "");
    body.put("conclusion", "");

    mockMvc.perform(post("/achievements")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void assessment_form_export_format_is_supported() throws Exception {
    var create = Map.of(
        "userId", 1,
        "title", "Kitchen safety",
        "period", "2026-Q1",
        "responsiblePerson", "Lead Chef",
        "conclusion", "Meets practice standard"
    );
    String json = mockMvc.perform(post("/achievements")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(create)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(
            get("/achievements/" + id + "/export")
                .param("format", "achievement_assessment_form_v1")
                .with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.format").value("achievement_assessment_form_v1"))
        .andExpect(jsonPath("$.sections").isArray());
  }
}

