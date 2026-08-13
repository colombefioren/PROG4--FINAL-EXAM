package org.cocojojo.mg.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course_assignment")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
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
}
