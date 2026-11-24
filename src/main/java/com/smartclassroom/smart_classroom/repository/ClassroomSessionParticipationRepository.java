package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.ClassroomSessionParticipation;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomSessionParticipationRepository
        extends JpaRepository<ClassroomSessionParticipation, Long> {

    List<ClassroomSessionParticipation> findByClassroomSession_IdOrderByParticipatedAtAsc(Long sessionId);

    List<ClassroomSessionParticipation> findByClassroomSession_IdAndStudent_IdIn(
            Long sessionId, Collection<Long> studentIds);

    boolean existsByClassroomSession_IdAndStudent_Id(Long sessionId, Long studentId);
}
