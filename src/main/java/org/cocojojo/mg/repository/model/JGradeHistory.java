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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "\"grade_history\"")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class JGradeHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"")
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "\"grade_id\"", nullable = false)
  private JGrade grade;

  @Column(name = "\"previous_value\"", precision = 4, scale = 2)
  private BigDecimal previousValue;

  @Column(name = "\"new_value\"", precision = 4, scale = 2)
  private BigDecimal newValue;

  @Column(name = "\"reason\"", nullable = false)
  private String reason;

  @ManyToOne
  @JoinColumn(name = "\"changed_by\"", nullable = false)
  private JUser changedBy;

  @CreationTimestamp
  @Column(name = "\"changed_at\"", nullable = false)
  private Instant changedAt;
}
