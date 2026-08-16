package org.cocojojo.mg.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.event.EventProducer;
import org.cocojojo.mg.endpoint.event.model.TranscriptRequested;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TranscriptService {

  private final StudentService studentService;
  private final EventProducer<TranscriptRequested> eventProducer;

  public void requestTranscript(UUID studentId, StudentLevel level) {
    studentService.getEntityOrThrow(studentId);
    eventProducer.accept(
        List.of(
            TranscriptRequested.builder()
                .studentId(studentId.toString())
                .level(level.name())
                .build()));
  }
}
