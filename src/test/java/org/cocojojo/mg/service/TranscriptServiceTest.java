package org.cocojojo.mg.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.UUID;
import org.cocojojo.mg.endpoint.event.EventProducer;
import org.cocojojo.mg.endpoint.event.model.TranscriptRequested;
import org.cocojojo.mg.endpoint.rest.controller.exception.ResourceNotFoundException;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.repository.model.JStudent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranscriptServiceTest {

  @Mock private StudentService studentService;
  @Mock private EventProducer<TranscriptRequested> eventProducer;

  @InjectMocks private TranscriptService service;

  private UUID studentId;

  @BeforeEach
  void setUp() {
    studentId = UUID.randomUUID();
  }

  @Test
  void requestTranscript_publishes_event() {
    given(studentService.getEntityOrThrow(studentId))
        .willReturn(JStudent.builder().id(studentId).build());

    service.requestTranscript(studentId, StudentLevel.L3);

    then(eventProducer)
        .should()
        .accept(
            argThat(
                (List<TranscriptRequested> events) ->
                    events != null
                        && events.size() == 1
                        && events.get(0).getStudentId().equals(studentId.toString())
                        && events.get(0).getLevel().equals("L3")));
  }

  @Test
  void requestTranscript_rejects_unknown_student() {
    given(studentService.getEntityOrThrow(studentId))
        .willThrow(new ResourceNotFoundException("Student with id: " + studentId + " not found."));

    assertThrows(
        ResourceNotFoundException.class,
        () -> service.requestTranscript(studentId, StudentLevel.L3));
  }
}
