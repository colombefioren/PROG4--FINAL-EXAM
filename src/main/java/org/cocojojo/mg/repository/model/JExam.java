package org.cocojojo.mg.repository.model;

import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cocojojo.mg.model.Fraction;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "\"exam\"")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class JExam {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"")
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "\"course_assignment_id\"", nullable = false)
  private JCourseAssignment courseAssignment;

  @Column(name = "\"title\"", nullable = false)
  private String title;

  @Column(name = "\"exam_datetime\"", nullable = false)
  private Instant examDatetime;

  @Setter(PRIVATE)
  @Column(name = "\"coefficient_numerator\"", nullable = false)
  private Integer coefficientNumerator;

  @Setter(PRIVATE)
  @Column(name = "\"coefficient_denominator\"", nullable = false)
  private Integer coefficientDenominator;

  @CreationTimestamp
  @Column(name = "\"created_at\"", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "\"updated_at\"", nullable = false)
  private Instant updatedAt;

  public Fraction getCoefficientFraction() {
    return new Fraction(coefficientNumerator, coefficientDenominator);
  }

  public void setCoefficientFraction(Fraction fraction) {
    setCoefficientNumerator(fraction.numerator());
    setCoefficientDenominator(fraction.denominator());
  }
}
