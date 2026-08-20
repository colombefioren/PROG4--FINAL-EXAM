package org.cocojojo.mg.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.endpoint.rest.controller.dto.CourseAssignmentResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.CurriculumStatusResponse;
import org.cocojojo.mg.endpoint.rest.controller.dto.TeacherResponse;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.endpoint.rest.security.JwtService;
import org.cocojojo.mg.model.enums.Semester;
import org.cocojojo.mg.service.CourseAssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourseAssignmentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseAssignmentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CourseAssignmentService service;
  @MockBean private JwtService jwtService;

  private final UUID id = UUID.randomUUID();
  private final UUID courseId = UUID.randomUUID();
  private final UUID groupId = UUID.randomUUID();
  private final UUID teacherId = UUID.randomUUID();

  private CourseAssignmentResponse response() {
    return CourseAssignmentResponse.builder()
        .id(id)
        .courseId(courseId)
        .courseCode("ALG1")
        .courseName("Algorithms")
        .groupId(groupId)
        .groupRef("G1")
        .teachers(
            List.of(
                TeacherResponse.builder()
                    .id(teacherId)
                    .firstname("Ada")
                    .lastname("Lovelace")
                    .email("a@hei.school")
                    .build()))
        .academicYear(2025)
        .semester(Semester.S1)
        .credits(6)
        .build();
  }

  @Test
  void getByFilter_returns_page() throws Exception {
    var page = new PageImpl<>(List.of(response()));
    given(service.getByFilter(any(), any(), any(), any(), any(Pageable.class))).willReturn(page);

    mockMvc
        .perform(get("/course-assignments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(id.toString()))
        .andExpect(jsonPath("$.content[0].courseCode").value("ALG1"))
        .andExpect(jsonPath("$.content[0].semester").value("S1"))
        .andExpect(jsonPath("$.content[0].teachers[0].lastname").value("Lovelace"));
  }

  @Test
  void getByFilter_accepts_query_parameters() throws Exception {
    var page = new PageImpl<>(List.of(response()));
    given(service.getByFilter(any(), any(), any(), any(), any(Pageable.class))).willReturn(page);

    mockMvc
        .perform(
            get("/course-assignments")
                .param("groupId", groupId.toString())
                .param("teacherId", teacherId.toString())
                .param("courseId", courseId.toString())
                .param("academicYear", "2025")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].groupId").value(groupId.toString()));
  }

  @Test
  void getById_returns_assignment() throws Exception {
    given(service.getById(id)).willReturn(response());

    mockMvc
        .perform(get("/course-assignments/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.credits").value(6));
  }

  @Test
  void getById_returns_not_found_when_missing() throws Exception {
    given(service.getById(id))
        .willThrow(new ResourceNotFoundException("CourseAssignment with id:" + id + " not found."));

    mockMvc
        .perform(get("/course-assignments/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("CourseAssignment with id:" + id + " not found."));
  }

  @Test
  void curriculumStatus_returns_status() throws Exception {
    var status =
        CurriculumStatusResponse.builder()
            .semester(Semester.S1)
            .assignedCredits(6)
            .targetCredits(6)
            .complete(true)
            .missingCourses(List.of())
            .assignments(List.of(response()))
            .build();
    given(service.curriculumStatus(groupId, 2025, Semester.S1)).willReturn(status);

    mockMvc
        .perform(
            get("/course-assignments/curriculum-status")
                .param("groupId", groupId.toString())
                .param("academicYear", "2025")
                .param("semester", "S1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.complete").value(true))
        .andExpect(jsonPath("$.assignedCredits").value(6))
        .andExpect(jsonPath("$.semester").value("S1"));
  }

  @Test
  void curriculumStatus_returns_bad_request_when_semester_missing() throws Exception {
    mockMvc
        .perform(
            get("/course-assignments/curriculum-status")
                .param("groupId", groupId.toString())
                .param("academicYear", "2025"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).curriculumStatus(any(), anyInt(), any());
  }

  @Test
  void upsert_saves_and_returns_assignments() throws Exception {
    given(service.upsert(any())).willReturn(List.of(response()));

    mockMvc
        .perform(
            put("/course-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "[{\"courseId\":\""
                        + courseId
                        + "\",\"groupId\":\""
                        + groupId
                        + "\",\"teacherIds\":[\""
                        + teacherId
                        + "\"],\"academicYear\":2025,\"semester\":\"S1\",\"credits\":6}]"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id.toString()))
        .andExpect(jsonPath("$[0].courseCode").value("ALG1"));
  }

  @Test
  void upsert_returns_bad_request_when_teacher_ids_empty() throws Exception {
    mockMvc
        .perform(
            put("/course-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "[{\"courseId\":\""
                        + courseId
                        + "\",\"groupId\":\""
                        + groupId
                        + "\",\"teacherIds\":[],\"academicYear\":2025,\"semester\":\"S1\",\"credits\":6}]"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).upsert(any());
  }

  @Test
  void upsert_returns_bad_request_when_academic_year_out_of_range() throws Exception {
    mockMvc
        .perform(
            put("/course-assignments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "[{\"courseId\":\""
                        + courseId
                        + "\",\"groupId\":\""
                        + groupId
                        + "\",\"teacherIds\":[\""
                        + teacherId
                        + "\"],\"academicYear\":1999,\"semester\":\"S1\",\"credits\":6}]"))
        .andExpect(status().isBadRequest());

    then(service).should(never()).upsert(any());
  }

  @Test
  void delete_returns_no_content() throws Exception {
    mockMvc.perform(delete("/course-assignments/{id}", id)).andExpect(status().isNoContent());

    then(service).should().delete(id);
  }
}
