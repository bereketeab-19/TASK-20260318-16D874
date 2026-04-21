package com.petsupplies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsupplies.cooking.web.dto.CreateTechniqueCardRequest;
import com.petsupplies.cooking.web.dto.UpdateTechniqueCardRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class TechniqueCardIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void unauthenticated_get_technique_cards_is_401() throws Exception {
    mockMvc.perform(get("/technique-cards"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void buyer_cannot_access_technique_cards() throws Exception {
    mockMvc.perform(get("/technique-cards").with(httpBasic("buyer", "buyer123!")))
        .andExpect(status().isForbidden());
  }

  @Test
  void merchant_crud_and_tag_filter() throws Exception {
    var create1 = new CreateTechniqueCardRequest(
        "Dice onions",
        "Small uniform cubes for even cooking.",
        List.of("prep", "knife")
    );
    String created = mockMvc.perform(post("/technique-cards")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(create1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.tags.length()").value(2))
        .andReturn()
        .getResponse()
        .getContentAsString();
    long id1 = objectMapper.readTree(created).get("id").asLong();

    var create2 = new CreateTechniqueCardRequest(
        "Boil water",
        "Rolling boil before pasta.",
        List.of("boil")
    );
    mockMvc.perform(post("/technique-cards")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(create2)))
        .andExpect(status().isOk());

    mockMvc.perform(get("/technique-cards").param("tag", "prep").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Dice onions"));

    mockMvc.perform(patch("/technique-cards/" + id1)
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new UpdateTechniqueCardRequest("Updated title", null, null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated title"));

    mockMvc.perform(delete("/technique-cards/" + id1).with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deleted").value(true));
  }

  @Test
  void cross_tenant_card_is_not_visible() throws Exception {
    var create = new CreateTechniqueCardRequest("Secret", "Body", List.of("x"));
    String json = mockMvc.perform(post("/technique-cards")
            .with(httpBasic("merchantA", "merchantA123!"))
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(create)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    long id = objectMapper.readTree(json).get("id").asLong();

    mockMvc.perform(get("/technique-cards/" + id).with(httpBasic("merchantB", "merchantB123!")))
        .andExpect(status().isNotFound());
  }
}
