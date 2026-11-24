
package com.smartclassroom.smart_classroom.repository;

import com.smartclassroom.smart_classroom.entity.User;
import com.smartclassroom.smart_classroom.entity.Role;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    // Find user during login
    Optional<User> findByEmail(String email);

    // Check whether email already exists
    boolean existsByEmail(String email);

    // Find all students
    List<User> findByRole(Role role);

    @EntityGraph(attributePaths = "parents")
    @Query("select distinct student from User student where student.role = :role and student.grade in :grades")
    List<User> findByRoleAndGradeIn(@Param("role") Role role,
            @Param("grades") Collection<Integer> grades);

    // Find a particular student
    Optional<User> findByIdAndRole(Long id, Role role);

    // Find all children linked to a parent
    List<User> findByParents_Id(Long parentId);
}
