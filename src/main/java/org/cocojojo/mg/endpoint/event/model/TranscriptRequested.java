package org.cocojojo.mg.endpoint.event.model;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Fired when a student's yearly-result transcript should be emailed to them. {@link #level} is one
 * of {@code L1}/{@code L2}/{@code L3} — the level whose result is rendered in the PDF.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class TranscriptRequested extends PojaEvent {
  private String studentId;
  private String level;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(60);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(2);
  }
}
