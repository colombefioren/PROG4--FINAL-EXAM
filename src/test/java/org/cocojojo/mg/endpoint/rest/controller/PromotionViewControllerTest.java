package org.cocojojo.mg.endpoint.rest.controller;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.service.GraduateListService;
import org.cocojojo.mg.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PromotionViewController.class)
@AutoConfigureMockMvc(addFilters = false)
class PromotionViewControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PromotionService promotionService;
  @MockBean private GraduateListService graduateListService;
  @MockBean private JwtService jwtService;

  private final UUID promotionId = UUID.randomUUID();

  private PromotionResponse promotion() {
    return PromotionResponse.builder()
        .id(promotionId)
        .ref("2025")
        .name("2025")
        .entryYear(2024)
        .build();
  }

  @Test
  void promotions_renders_promotions_view_with_graduate_eligibility() throws Exception {
    var promotion = promotion();
    given(promotionService.getAllWithoutPagination()).willReturn(List.of(promotion));
    given(graduateListService.isAcrossThreeYears(promotionId)).willReturn(true);

    mockMvc
        .perform(get("/ui/promotions"))
        .andExpect(status().isOk())
        .andExpect(view().name("promotions"))
        .andExpect(model().attribute("promotions", hasSize(1)))
        .andExpect(model().attribute("graduateEligibility", hasEntry(promotionId, true)));
  }

  @Test
  void promotions_renders_empty_view_when_no_promotions() throws Exception {
    given(promotionService.getAllWithoutPagination()).willReturn(List.of());

    mockMvc
        .perform(get("/ui/promotions"))
        .andExpect(status().isOk())
        .andExpect(view().name("promotions"))
        .andExpect(model().attribute("promotions", hasSize(0)))
        .andExpect(model().attributeExists("graduateEligibility"));
  }
}
