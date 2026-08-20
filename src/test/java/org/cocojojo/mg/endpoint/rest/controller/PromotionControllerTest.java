package org.cocojojo.mg.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.GraduateResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionRequest;
import org.cocojojo.mg.endpoint.rest.controller.dto.PromotionResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.service.GraduateListService;
import org.cocojojo.mg.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PromotionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PromotionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PromotionService service;
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

  private GraduateResponse graduate() {
    return GraduateResponse.builder()
        .rank(1)
        .std("STD24001")
        .firstname("Grace")
        .lastname("Hopper")
        .track(Track.EL)
        .generalAverage(new BigDecimal("15.25"))
        .build();
  }

  @Test
  void getAll_returns_page() throws Exception {
    var page = new PageImpl<>(List.of(promotion()));
    given(service.getAll(any(Pageable.class))).willReturn(page);

    mockMvc
        .perform(get("/promotions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].ref").value("2025"))
        .andExpect(jsonPath("$.content[0].entryYear").value(2024));
  }

  @Test
  void getById_returns_promotion() throws Exception {
    given(service.getById(promotionId)).willReturn(promotion());

    mockMvc
        .perform(get("/promotions/{id}", promotionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(promotionId.toString()))
        .andExpect(jsonPath("$.name").value("2025"));
  }

  @Test
  void getCourses_filters_by_level_and_track() throws Exception {
    var course =
        CourseResponse.builder()
            .id(UUID.randomUUID())
            .code("ALG1")
            .name("Algorithms")
            .credits(6)
            .totalHours(45)
            .studentLevel(StudentLevel.L1)
            .track(Track.EL)
            .build();
    given(service.getCourses(promotionId, StudentLevel.L1, Track.EL)).willReturn(List.of(course));

    mockMvc
        .perform(
            get("/promotions/{id}/courses", promotionId)
                .param("studentLevel", "L1")
                .param("track", "EL"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("ALG1"))
        .andExpect(jsonPath("$[0].studentLevel").value("L1"))
        .andExpect(jsonPath("$[0].track").value("EL"));
  }

  @Test
  void upsert_returns_promotion() throws Exception {
    given(service.upsert(any(PromotionRequest.class))).willReturn(promotion());

    mockMvc
        .perform(
            put("/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ref\":\"2025\",\"name\":\"2025\",\"entryYear\":2024}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ref").value("2025"));
  }

  @Test
  void upsert_returns_bad_request_when_ref_blank() throws Exception {
    mockMvc
        .perform(
            put("/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ref\":\"\",\"name\":\"2025\",\"entryYear\":2024}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).upsert(any(PromotionRequest.class));
  }

  @Test
  void upsert_returns_bad_request_when_entry_year_missing() throws Exception {
    mockMvc
        .perform(
            put("/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ref\":\"2025\",\"name\":\"2025\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).upsert(any(PromotionRequest.class));
  }

  @Test
  void getGraduates_returns_ranked_list() throws Exception {
    given(graduateListService.getGraduates(promotionId)).willReturn(List.of(graduate()));

    mockMvc
        .perform(get("/promotions/{promotionId}/graduates", promotionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].rank").value(1))
        .andExpect(jsonPath("$[0].std").value("STD24001"))
        .andExpect(jsonPath("$[0].track").value("EL"))
        .andExpect(jsonPath("$[0].generalAverage").value(15.25));
  }

  @Test
  void export_returns_signed_url() throws Exception {
    given(graduateListService.export(promotionId)).willReturn("https://s3/bucket/graduates.xlsx");

    mockMvc
        .perform(get("/promotions/{promotionId}/graduates/export", promotionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value("https://s3/bucket/graduates.xlsx"));
  }

  @Test
  void download_returns_xlsx_with_attachment_headers() throws Exception {
    byte[] bytes = "fake-xlsx".getBytes(StandardCharsets.UTF_8);
    given(graduateListService.buildXlsx(promotionId)).willReturn(bytes);
    given(graduateListService.buildFileName(promotionId)).willReturn("graduates-2025.xlsx");

    mockMvc
        .perform(get("/promotions/{promotionId}/graduates/download", promotionId))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"graduates-2025.xlsx\""))
        .andExpect(content().bytes(bytes));
  }
}
