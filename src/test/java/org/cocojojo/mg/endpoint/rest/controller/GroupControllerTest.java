package org.cocojojo.mg.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.GroupResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.StudentResponse;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.Track;
import org.cocojojo.mg.service.GroupService;
import org.cocojojo.mg.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GroupService service;
  @MockBean private StudentService studentService;
  @MockBean private JwtService jwtService;

  private final UUID id = UUID.randomUUID();
  private final UUID promotionId = UUID.randomUUID();

  @Test
  void get_all_returns_page() throws Exception {
    var response = response();
    given(service.getAll(isNull(), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(response)));

    mockMvc
        .perform(get("/groups"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(id.toString()))
        .andExpect(jsonPath("$.content[0].ref").value("G1"));
  }

  @Test
  void update_returns_updated_group() throws Exception {
    given(service.upsert(any())).willReturn(response());

    mockMvc
        .perform(
            put("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"promotionId\":\"" + promotionId + "\",\"ref\":\"G1\",\"track\":\"TN\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ref").value("G1"));
  }

  @Test
  void update_returns_bad_request_when_body_is_invalid() throws Exception {
    mockMvc
        .perform(
            put("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"promotionId\":null,\"ref\":\"\"}"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).upsert(any());
  }

  @Test
  void get_students_returns_group_members() throws Exception {
    var student =
        StudentResponse.builder()
            .id(UUID.randomUUID())
            .std("STD-01")
            .firstname("Alan")
            .lastname("Turing")
            .email("alan@hei.school")
            .build();
    given(studentService.getByGroup(id)).willReturn(List.of(student));

    mockMvc
        .perform(get("/groups/{id}/students", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].std").value("STD-01"))
        .andExpect(jsonPath("$[0].firstname").value("Alan"));
  }

  private GroupResponse response() {
    return GroupResponse.builder()
        .id(id)
        .promotionId(promotionId)
        .promotionName("Promotion")
        .ref("G1")
        .track(Track.TN)
        .build();
  }
}
