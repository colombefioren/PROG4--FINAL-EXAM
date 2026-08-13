package org.cocojojo.mg.repository.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "teacher")
@PrimaryKeyJoinColumn(name = "id")
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
public class JTeacher extends JUser {
}
