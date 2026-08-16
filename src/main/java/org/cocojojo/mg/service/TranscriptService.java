package org.cocojojo.mg.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cocojojo.mg.endpoint.event.EventProducer;
import org.cocojojo.mg.endpoint.event.model.TranscriptRequested;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.springframework.stereotype.Service;

/**
 * Only enqueues the request — the actual PDF/S3/email work happens asynchronously in {@link
 * org.cocojojo.mg.service.event.TranscriptRequestedService} through the standard Poja event
 * mechanism.
 */
@Service
@RequiredArgsConstructor
public class TranscriptService {

  private final StudentService studentService;
  private final EventProducer<TranscriptRequested> eventProducer;

  public void requestTranscript(UUID studentId, StudentLevel level) {
    studentService.getEntityOrThrow(studentId); // 404s if not a student
    eventProducer.accept(
        List.of(
            TranscriptRequested.builder()
                .studentId(studentId.toString())
                .level(level.name())
                .build()));
  }
}
