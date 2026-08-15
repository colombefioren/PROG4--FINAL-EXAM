package org.cocojojo.mg.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.cocojojo.mg.model.enums.Semester;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "\"course_assignment\"",
    uniqueConstraints =
        @UniqueConstraint(
            name = "course_assignment_uk",
            columnNames = {"\"course_id\"", "\"group_id\"", "\"academic_year\"", "\"semester\""}))
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@SQLDelete(sql = "update \"course_assignment\" set \"is_deleted\" = true where \"id\" = ?")
@SQLRestriction("\"is_deleted\" = false")
public class JCourseAssignment {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"")
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "\"course_id\"", nullable = false)
  private JCourse course;

  @ManyToOne
  @JoinColumn(name = "\"group_id\"", nullable = false)
  private JGroup group;

  @ManyToMany
  @JoinTable(
      name = "\"course_assignment_teacher\"",
      joinColumns = @JoinColumn(name = "\"course_assignment_id\""),
      inverseJoinColumns = @JoinColumn(name = "\"teacher_id\""))
  @EqualsAndHashCode.Exclude
  @Builder.Default
  private List<JTeacher> teachers = new ArrayList<>();

  @Column(name = "\"academic_year\"", nullable = false)
  private Integer academicYear;

  @Column(name = "\"semester\"", nullable = false)
  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Semester semester;

  @Column(name = "\"credits\"", nullable = false)
  private Integer credits;

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
