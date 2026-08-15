package org.cocojojo.mg.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "\"promotion\"")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@EqualsAndHashCode
@SQLDelete(sql = "update \"promotion\" set \"is_deleted\" = true where \"id\" = ?")
@SQLRestriction("\"is_deleted\" = false")
public class JPromotion {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"")
  private UUID id;

  @Column(name = "\"ref\"", nullable = false, unique = true)
  private String ref;

  @Column(name = "\"name\"", nullable = false, unique = true)
  private String name;

  @Column(name = "\"entry_year\"", nullable = false)
  private Integer entryYear;

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
