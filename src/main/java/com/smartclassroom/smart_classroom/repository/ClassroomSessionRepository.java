
package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.ClassroomSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomSessionRepository
        extends JpaRepository<ClassroomSession, Long> {

    // Find all sessions created by a particular teacher
    List<ClassroomSession> findByTeacherEmail(String email);

    // Find a particular session belonging to a particular teacher
    Optional<ClassroomSession> findByIdAndTeacherEmail(Long id, String email);
}
