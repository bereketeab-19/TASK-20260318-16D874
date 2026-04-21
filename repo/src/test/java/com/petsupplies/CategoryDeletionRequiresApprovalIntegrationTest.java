package com.petsupplies;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Category removal must go through admin dual-approval ({@code ACTIVE_CATEGORY_DELETION}), not a direct merchant DELETE.
 */
@AutoConfigureMockMvc
class CategoryDeletionRequiresApprovalIntegrationTest extends AbstractIntegrationTest {

  @Autowired MockMvc mockMvc;

  @Test
  void merchant_cannot_delete_category_via_direct_http_delete() throws Exception {
    mockMvc.perform(delete("/categories/1").with(httpBasic("merchantA", "merchantA123!")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }
}
