package org.cocojojo.mg.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cocojojo.mg.model.enums.GroupFlowType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "group_flow")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class JGroupFlow {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "student_id", nullable = false)
  private JStudent student;

  @ManyToOne
  @JoinColumn(name = "group_id", nullable = false)
  private JGroup group;

  @Column(name = "group_flow_type")
  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private GroupFlowType groupFlowType;

  @CreationTimestamp
  @Column(name = "flow_datetime", nullable = false)
  private Instant flowDatetime;
}
