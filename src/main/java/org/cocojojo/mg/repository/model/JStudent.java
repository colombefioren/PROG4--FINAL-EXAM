package org.cocojojo.mg.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "\"student\"")
@PrimaryKeyJoinColumn(name = "\"id\"")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
public class JStudent extends JUser {
  @Column(name = "\"std\"", nullable = false, unique = true)
  private String std;

  @ManyToOne
  @JoinColumn(name = "\"promotion_id\"", nullable = false)
  private JPromotion promotion;
}
