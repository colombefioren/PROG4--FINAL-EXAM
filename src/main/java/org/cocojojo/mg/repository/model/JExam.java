package org.cocojojo.mg.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exam")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class JExam {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "course_assignment_id", nullable = false)
  private JCourseAssignment courseAssignment;

  @Column(nullable = false)
  private String title;

  @Column(name = "exam_datetime", nullable = false)
  private Instant examDatetime;

  @Column(nullable = false)
  private BigDecimal coefficient;
}
