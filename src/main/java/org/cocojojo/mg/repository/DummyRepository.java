package org.cocojojo.mg.repository;

import java.util.List;
import org.cocojojo.mg.PojaGenerated;
import org.cocojojo.mg.repository.model.Dummy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@PojaGenerated
@Repository
public interface DummyRepository extends JpaRepository<Dummy, String> {

  @Override
  List<Dummy> findAll();
}
