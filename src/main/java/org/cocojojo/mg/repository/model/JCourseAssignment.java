package org.cocojojo.mg.repository.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
    name = "course_assignment",
    uniqueConstraints =
        @UniqueConstraint(
            columnNames = {"course_id", "teacher_id", "group_id", "academic_year", "semester"}))
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@SQLDelete(sql = "update \"course_assignment\" set is_deleted = true where id = ?")
@SQLRestriction("is_deleted = false")
public class JCourseAssignment {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "course_id", nullable = false)
  private JCourse course;

  @ManyToOne
  @JoinColumn(name = "group_id", nullable = false)
  private JGroup group;

  @ManyToOne
  @JoinColumn(name = "teacher_id", nullable = false)
  private JTeacher teacher;

  @Column(name = "academic_year", nullable = false)
  private int academicYear;

  @Column(nullable = false)
  private int semester;

  /**
   * Credits granted for this course as taught to this group this year (defaults to the course's
   * catalog credits, but can be overridden per prom)
   */
  @Column(nullable = false)
  private int credits;

  @EqualsAndHashCode.Exclude @CreationTimestamp private Instant creationDatetime;

  @EqualsAndHashCode.Exclude @Builder.Default private boolean isDeleted = false;
}
