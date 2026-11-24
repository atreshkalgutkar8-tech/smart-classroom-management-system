package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.DIYActivity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface DIYActivityRepository extends JpaRepository<DIYActivity, Long> {
    @EntityGraph(attributePaths = "createdByTeacher")
    List<DIYActivity> findAllByOrderByGradeAscSubjectAscTopicAscTitleAsc();

    @EntityGraph(attributePaths = "createdByTeacher")
    List<DIYActivity> findByGradeOrderBySubjectAscTopicAscTitleAsc(Integer grade);

    @EntityGraph(attributePaths = "createdByTeacher")
    Optional<DIYActivity> findByIdAndGrade(Long id, Integer grade);

    @EntityGraph(attributePaths = "createdByTeacher")
    Optional<DIYActivity> findById(Long id);
}
