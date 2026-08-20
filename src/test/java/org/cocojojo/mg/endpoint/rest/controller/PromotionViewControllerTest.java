package org.cocojojo.mg.endpoint.rest.controller;

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

@WebMvcTest(controllers = PromotionViewController.class)
@AutoConfigureMockMvc(addFilters = false)
class PromotionViewControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PromotionService promotionService;
  @MockBean private GraduateListService graduateListService;
  @MockBean private JwtService jwtService;

  @Test
  void promotions_renders_view_with_eligibility_map() throws Exception {
    var promotion =
        PromotionResponse.builder()
            .id(UUID.randomUUID())
            .ref("P1")
            .name("Promotion")
            .entryYear(2023)
            .build();
    given(promotionService.getAllWithoutPagination()).willReturn(List.of(promotion));
    given(graduateListService.isAcrossThreeYears(promotion.id())).willReturn(true);

    mockMvc
        .perform(get("/ui/promotions"))
        .andExpect(status().isOk())
        .andExpect(view().name("promotions"))
        .andExpect(model().attributeExists("promotions"))
        .andExpect(model().attributeExists("graduateEligibility"));
  }
}
