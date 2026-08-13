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
import org.cocojojo.mg.model.enums.Track;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "\"group\"")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class JGroup {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"")
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "\"promotion_id\"", nullable = false)
  private JPromotion promotion;

  @Column(name = "\"ref\"", nullable = false, unique = true)
  private String ref;

  @Column(name = "\"track\"")
  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Track track;

  @CreationTimestamp
  @Column(name = "\"created_at\"", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "\"updated_at\"", nullable = false)
  private Instant updatedAt;
}
