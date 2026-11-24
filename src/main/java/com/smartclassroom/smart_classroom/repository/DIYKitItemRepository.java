package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.DIYKitItem;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface DIYKitItemRepository extends JpaRepository<DIYKitItem, Long> {
    List<DIYKitItem> findByKit_IdOrderByItemNameAsc(Long kitId);

    @EntityGraph(attributePaths = "kit")
    List<DIYKitItem> findByKit_IdInOrderByItemNameAsc(Collection<Long> kitIds);
}
