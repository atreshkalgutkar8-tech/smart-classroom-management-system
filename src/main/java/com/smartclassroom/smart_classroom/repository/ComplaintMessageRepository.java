package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.ComplaintMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintMessageRepository extends JpaRepository<ComplaintMessage, Long> {
    @EntityGraph(attributePaths = "sender")
    List<ComplaintMessage> findByComplaint_IdOrderBySentAtAsc(Long complaintId);

    List<ComplaintMessage> findByComplaint_IdAndSender_IdNotAndReadAtIsNull(
            Long complaintId, Long senderId);

    long countByComplaint_IdAndSender_IdNotAndReadAtIsNull(Long complaintId, Long senderId);
}
