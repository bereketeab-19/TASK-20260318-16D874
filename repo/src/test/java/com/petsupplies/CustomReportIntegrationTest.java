package com.petsupplies;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class CustomReportIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void unauthenticated_is_401() throws Exception {
    mockMvc.perform(get("/merchant/custom-reports"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void buyer_is_403() throws Exception {
    mockMvc.perform(get("/merchant/custom-reports").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalid_definition_json_is_400() throws Exception {
    String body = objectMapper.writeValueAsString(
        Map.of(
            "name", "r1",
            "definitionJson", "{not json"
        ));
    mockMvc.perform(post("/merchant/custom-reports")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void create_execute_and_cross_tenant_isolation() throws Exception {
    String def = "{\"template\":\"INVENTORY_SUMMARY\"}";
    String createBody = objectMapper.writeValueAsString(
        Map.of("name", "My summary", "definitionJson", def));

    String json = mockMvc.perform(post("/merchant/custom-reports")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(createBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.definitionJson").exists())
        .andReturn()
        .getResponse()
        .getContentAsString();
    long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(get("/merchant/custom-reports/" + id).with(httpBasic("merchantB", "merchantB123!")))
        .andExpect(status().isNotFound());

    mockMvc.perform(post("/merchant/custom-reports/" + id + "/execute").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reportId").value(id))
        .andExpect(jsonPath("$.data.template").value("INVENTORY_SUMMARY"))
        .andExpect(jsonPath("$.data.merchantId").value("mrc_A"));
  }

  @Test
  void unknown_template_in_definition_is_400_on_execute() throws Exception {
    String def = "{\"template\":\"UNKNOWN_TEMPLATE_X\"}";
    String createBody = objectMapper.writeValueAsString(
        Map.of("name", "bad tpl", "definitionJson", def));

    String json = mockMvc.perform(post("/merchant/custom-reports")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(createBody))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(post("/merchant/custom-reports/" + id + "/execute").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void patch_can_deactivate_then_execute_fails() throws Exception {
    String def = "{\"template\":\"INVENTORY_SUMMARY\"}";
    String createBody = objectMapper.writeValueAsString(
        Map.of("name", "toggle me", "definitionJson", def));

    String json = mockMvc.perform(post("/merchant/custom-reports")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(createBody))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(patch("/merchant/custom-reports/" + id)
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content("{\"active\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    mockMvc.perform(post("/merchant/custom-reports/" + id + "/execute").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void movement_timeline_execute_requires_valid_date_range() throws Exception {
    String defBad = "{\"template\":\"INVENTORY_MOVEMENT_TIMELINE\"}";
    String createBody = objectMapper.writeValueAsString(Map.of("name", "tl bad", "definitionJson", defBad));
    String json = mockMvc.perform(post("/merchant/custom-reports")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(createBody))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(post("/merchant/custom-reports/" + id + "/execute").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void movement_timeline_execute_returns_series() throws Exception {
    String def = "{\"template\":\"INVENTORY_MOVEMENT_TIMELINE\",\"from\":\"2026-01-01\",\"to\":\"2026-04-01\"}";
    String createBody = objectMapper.writeValueAsString(Map.of("name", "tl ok", "definitionJson", def));
    String json = mockMvc.perform(post("/merchant/custom-reports")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(createBody))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(post("/merchant/custom-reports/" + id + "/execute").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.template").value("INVENTORY_MOVEMENT_TIMELINE"))
        .andExpect(jsonPath("$.data.series").isArray());
  }
}
