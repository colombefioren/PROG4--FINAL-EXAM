package org.cocojojo.mg.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.cocojojo.mg.model.enums.Track;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "\"course\"")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@SQLDelete(sql = "update \"course\" set \"is_deleted\" = true where \"id\" = ?")
@SQLRestriction("\"is_deleted\" = false")
public class JCourse {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"")
  private UUID id;

  // (code) uniqueness is enforced by a partial unique index that only covers
  // live rows (see V66), so re-creating a course with the same code after a soft
  // delete works.
  @Column(name = "\"code\"", nullable = false)
  private String code;

  @Column(name = "\"name\"", nullable = false)
  private String name;

  @Column(name = "\"credits\"", nullable = false)
  private Integer credits;

  @Column(name = "\"total_hours\"")
  private Integer totalHours;

  @Column(name = "\"student_level\"", nullable = false)
  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private StudentLevel studentLevel;

  @Column(name = "\"track\"")
  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Track track;

  @EqualsAndHashCode.Exclude
  @Builder.Default
  @Column(name = "\"is_deleted\"")
  private boolean isDeleted = false;

  @EqualsAndHashCode.Exclude
  @CreationTimestamp
  @Column(name = "\"created_at\"", nullable = false, updatable = false)
  private Instant createdAt;

  @EqualsAndHashCode.Exclude
  @UpdateTimestamp
  @Column(name = "\"updated_at\"", nullable = false)
  private Instant updatedAt;
}
