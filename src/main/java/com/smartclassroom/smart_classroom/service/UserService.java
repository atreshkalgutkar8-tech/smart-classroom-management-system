
package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.Role;
import com.smartclassroom.smart_classroom.entity.User;
import com.smartclassroom.smart_classroom.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Comparator;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(
            String fullName,
            String email,
            String rawPassword,
            Role role) {

        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "This email is already registered.");
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);

        return userRepository.save(user);
    }

    private void validateStudentClass(
            Integer grade,
            String section) {

        if (grade == null || grade < 1 || grade > 5) {
            throw new IllegalArgumentException(
                    "Grade must be between 1 and 5.");
        }

        if (section == null || section.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Section cannot be empty.");
        }

        if (section.trim().length() > 20) {
            throw new IllegalArgumentException(
                    "Section cannot exceed 20 characters.");
        }
    }

    // Administrator creates a student account
    public User createStudentAccount(
            String fullName,
            String email,
            String password,
            Integer grade,
            String section) {

        validateStudentClass(grade, section);

        User student = registerUser(
                fullName,
                email,
                password,
                Role.STUDENT
        );

        student.setGrade(grade);
        student.setSection(normalizeSection(section));

        return userRepository.save(student);
    }

    // Administrator updates student grade and section
    public User updateStudentClassDetails(
            Long studentId,
            Integer grade,
            String section) {

        validateStudentClass(grade, section);

        User student = userRepository
                .findByIdAndRole(studentId, Role.STUDENT)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student not found."));

        student.setGrade(grade);
        student.setSection(normalizeSection(section));

        return userRepository.save(student);
    }

    // Retrieve all student accounts
    public List<User> getAllStudents() {
        return userRepository.findByRole(Role.STUDENT);
    }

    // Retrieve all parent accounts
    public List<User> getAllParents() {
        return userRepository.findByRole(Role.PARENT);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing((User user) -> user.getRole().name())
                        .thenComparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // Search students by name or email
    public List<User> searchStudents(String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllStudents();
        }

        String search = keyword.trim().toLowerCase(Locale.ROOT);

        return getAllStudents().stream()
                .filter(student ->
                        student.getFullName().toLowerCase(Locale.ROOT)
                                .contains(search)
                        ||
                        student.getEmail().toLowerCase(Locale.ROOT)
                                .contains(search))
                .toList();
    }

    // Search parents by name or email
    public List<User> searchParents(String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllParents();
        }

        String search = keyword.trim().toLowerCase(Locale.ROOT);

        return getAllParents().stream()
                .filter(parent ->
                        parent.getFullName().toLowerCase(Locale.ROOT)
                                .contains(search)
                        ||
                        parent.getEmail().toLowerCase(Locale.ROOT)
                                .contains(search))
                .toList();
    }

    // Link a student to a parent
    @Transactional
    public void linkStudentToParent(
            Long parentId,
            Long studentId) {

        User parent = userRepository
                .findByIdAndRole(parentId, Role.PARENT)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Parent account not found."));

        User student = userRepository
                .findByIdAndRole(studentId, Role.STUDENT)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student account not found."));

        parent.addChild(student);

        userRepository.save(parent);
    }

    // Get only the children linked to the logged-in parent
    @Transactional(readOnly = true)
    public List<User> getChildrenForParent(String parentEmail) {

        User parent = userRepository
                .findByEmail(parentEmail.trim().toLowerCase())
                .filter(user -> user.getRole() == Role.PARENT)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Parent account not found."));

        return List.copyOf(parent.getChildren());
    }

    // Parent resets their linked child's password
    @Transactional
    public void resetStudentPasswordByParent(
            Long parentId,
            Long studentId,
            String newPassword) {

        User parent = userRepository
                .findByIdAndRole(parentId, Role.PARENT)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Parent account not found."));

        User student = userRepository
                .findByIdAndRole(studentId, Role.STUDENT)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student account not found."));

        boolean isLinked = student.getParents().stream()
                .anyMatch(p -> p.getId().equals(parent.getId()));

        if (!isLinked) {
            throw new IllegalArgumentException(
                    "You cannot reset the password of an unlinked student.");
        }

        student.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(student);
    }

    // Administrator resets a student's password
    public void resetStudentPasswordByAdmin(
            Long studentId,
            String newPassword) {

        User student = userRepository
                .findByIdAndRole(studentId, Role.STUDENT)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Student account not found."));

        student.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(student);
    }

    private String normalizeSection(String section) {
        return section.trim().toUpperCase(Locale.ROOT);
    }
}
