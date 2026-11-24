
package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.ClassroomSession;
import com.smartclassroom.smart_classroom.entity.SessionStatus;
import com.smartclassroom.smart_classroom.entity.User;
import com.smartclassroom.smart_classroom.entity.Role;

import com.smartclassroom.smart_classroom.repository.ClassroomSessionRepository;
import com.smartclassroom.smart_classroom.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClassroomSessionService {

    private final ClassroomSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public ClassroomSessionService(
            ClassroomSessionRepository sessionRepository,
            UserRepository userRepository) {

        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    // Find and verify the logged-in teacher
    private User findTeacher(String email) {

        User teacher = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Teacher not found."));

        if (teacher.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException(
                    "Only teachers can manage classroom sessions.");
        }

        return teacher;
    }

    // Get sessions belonging to a particular teacher
    public List<ClassroomSession> getSessionsByTeacher(String email) {

        findTeacher(email);

        return sessionRepository.findByTeacherEmail(email);
    }

    // Create a new classroom session
    public ClassroomSession createSession(
            String title,
            Integer grade,
            String subject,
            String topic,
            String email) {

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Session title is required.");
        }

        if (grade == null || grade < 1 || grade > 5) {
            throw new IllegalArgumentException(
                    "Grade must be between 1 and 5.");
        }

        if (subject == null ||
                !List.of("Mathematics", "Kannada", "English", "EVS")
                        .contains(subject)) {

            throw new IllegalArgumentException(
                    "Select a valid subject.");
        }

        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException(
                    "Topic is required.");
        }

        User teacher = findTeacher(email);

        ClassroomSession session = new ClassroomSession();

        session.setTitle(title.trim());
        session.setGrade(grade);
        session.setSubject(subject);
        session.setTopic(topic.trim());
        session.setStatus(SessionStatus.PLANNED);

        // Associate session with its creator
        session.setTeacher(teacher);

        return sessionRepository.save(session);
    }

    // Find a session belonging to the logged-in teacher
    private ClassroomSession getTeacherSession(
            Long id, String email) {

        findTeacher(email);

        return sessionRepository
                .findByIdAndTeacherEmail(id, email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Classroom session not found."));
    }

    /** Returns a session only after verifying the authenticated teacher owns it. */
    @Transactional(readOnly = true)
    public ClassroomSession getTeacherOwnedSession(Long id, String email) {
        return getTeacherSession(id, email);
    }

    // Start a planned session
    @Transactional
    public ClassroomSession startSession(
            Long id, String email) {

        ClassroomSession session = getTeacherSession(id, email);

        if (session.getStatus() != SessionStatus.PLANNED) {
            throw new IllegalStateException(
                    "Only planned sessions can be started.");
        }

        session.setStatus(SessionStatus.ONGOING);
        session.setStartedAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    // Complete an ongoing session
    @Transactional
    public ClassroomSession completeSession(
            Long id, String email) {

        ClassroomSession session = getTeacherSession(id, email);

        if (session.getStatus() != SessionStatus.ONGOING) {
            throw new IllegalStateException(
                    "Only ongoing sessions can be completed.");
        }

        LocalDateTime endedAt = LocalDateTime.now();
        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(endedAt);
        if (session.getPresentationStartedAt() != null
                && (session.getPresentationEndedAt() == null
                        || session.getPresentationEndedAt().isBefore(session.getPresentationStartedAt()))) {
            session.setPresentationEndedAt(endedAt);
        }

        return sessionRepository.save(session);
    }
}
