package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.DIYActivityParticipation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface DIYActivityParticipationRepository extends JpaRepository<DIYActivityParticipation, Long> {
    boolean existsByActivity_IdAndStudent_Id(Long activityId, Long studentId);
    Optional<DIYActivityParticipation> findByActivity_IdAndStudent_Id(Long activityId, Long studentId);
    @EntityGraph(attributePaths = "activity")
    List<DIYActivityParticipation> findByStudent_IdOrderByCompletedAtDesc(Long studentId);

    @Query("select participation.activity.id from DIYActivityParticipation participation "
            + "where participation.student.id = :studentId")
    List<Long> findCompletedActivityIdsByStudent(@Param("studentId") Long studentId);
}
