package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.DIYKit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DIYKitRepository extends JpaRepository<DIYKit, Long> {
    List<DIYKit> findAllByOrderByNameAsc();
}
