package org.cocojojo.mg.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.cocojojo.mg.model.enums.Track;

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
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "promotion_id", nullable = false)
  private JPromotion promotion;

  @Column(nullable = false, unique = true)
  private String ref;

  @Enumerated(EnumType.STRING)
  private Track track;
}
