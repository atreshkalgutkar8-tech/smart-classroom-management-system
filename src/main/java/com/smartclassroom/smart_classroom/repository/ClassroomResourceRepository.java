package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.ClassroomResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomResourceRepository extends JpaRepository<ClassroomResource, Long> {

    List<ClassroomResource> findByClassroomSession_IdOrderByDisplayOrderAscIdAsc(Long sessionId);

    Optional<ClassroomResource> findByIdAndClassroomSession_Id(Long id, Long sessionId);

    long countByClassroomSession_Id(Long sessionId);
}
