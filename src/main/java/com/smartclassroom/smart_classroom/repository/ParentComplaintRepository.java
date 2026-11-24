package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.ParentComplaint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ParentComplaintRepository extends JpaRepository<ParentComplaint, Long> {
    @EntityGraph(attributePaths = {"parent", "student"})
    List<ParentComplaint> findByAssignedTeacher_IdOrderByUpdatedAtDesc(Long teacherId);

    @EntityGraph(attributePaths = {"parent", "assignedTeacher", "student"})
    List<ParentComplaint> findByParent_IdOrderByUpdatedAtDesc(Long parentId);

    @EntityGraph(attributePaths = {"parent", "student", "assignedTeacher"})
    Optional<ParentComplaint> findWithParticipantsById(Long id);
}
