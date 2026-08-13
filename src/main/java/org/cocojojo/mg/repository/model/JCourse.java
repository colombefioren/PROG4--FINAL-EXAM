package org.cocojojo.mg.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.cocojojo.mg.model.enums.StudentLevel;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "\"course\"")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@SQLDelete(sql = "update \"course\" set is_deleted = true where id = ?")
@SQLRestriction("is_deleted = false")
public class JCourse {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private int credits;

  @Column(nullable = false)
  private int totalHours;

  @Column(name = "\"student_level\"")
  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private StudentLevel studentLevel;

  @EqualsAndHashCode.Exclude @Builder.Default private boolean isDeleted = false;
}
