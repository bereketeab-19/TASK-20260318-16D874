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
class CookingResumptionTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void checkpoint_then_resume_returns_same_state() throws Exception {
    var create = mockMvc.perform(post("/cooking/processes").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andReturn();

    long processId = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(post("/cooking/checkpoint")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(Map.of(
                "processId", processId,
                "currentStepIndex", 3,
                "status", "PAUSED"
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentStepIndex").value(3))
        .andExpect(jsonPath("$.status").value("PAUSED"));

    mockMvc.perform(get("/cooking/processes/" + processId)
            .with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentStepIndex").value(3))
        .andExpect(jsonPath("$.status").value("PAUSED"));
  }
}

