package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.ClassroomSession;
import com.smartclassroom.smart_classroom.entity.ClassroomSessionParticipation;
import com.smartclassroom.smart_classroom.entity.Role;
import com.smartclassroom.smart_classroom.entity.SessionStatus;
import com.smartclassroom.smart_classroom.entity.User;
import com.smartclassroom.smart_classroom.repository.ClassroomSessionParticipationRepository;
import com.smartclassroom.smart_classroom.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassroomSessionParticipationService {

    private final ClassroomSessionService sessionService;
    private final TeacherStudentService teacherStudentService;
    private final ClassroomSessionParticipationRepository participationRepository;
    private final UserRepository userRepository;

    public ClassroomSessionParticipationService(ClassroomSessionService sessionService,
            TeacherStudentService teacherStudentService,
            ClassroomSessionParticipationRepository participationRepository,
            UserRepository userRepository) {
        this.sessionService = sessionService;
        this.teacherStudentService = teacherStudentService;
        this.participationRepository = participationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ParticipationPage getPage(Long sessionId, String teacherEmail) {
        ClassroomSession session = sessionService.getTeacherOwnedSession(sessionId, teacherEmail);
        List<TeacherStudentService.StudentSummary> eligible = eligibleStudents(session, teacherEmail);
        List<ClassroomSessionParticipation> recorded = participationRepository
                .findByClassroomSession_IdOrderByParticipatedAtAsc(sessionId);
        Set<Long> recordedIds = recorded.stream().map(p -> p.getStudent().getId()).collect(Collectors.toSet());

        List<EligibleStudent> studentChoices = eligible.stream()
                .map(student -> new EligibleStudent(student.id(), student.fullName(), student.grade(),
                        student.section(), recordedIds.contains(student.id())))
                .toList();
        List<Participant> history = recorded.stream()
                .map(p -> new Participant(p.getStudent().getId(), p.getStudent().getFullName(),
                        p.getStudent().getGrade(), p.getStudent().getSection(), p.getParticipatedAt()))
                .toList();
        return new ParticipationPage(new SessionInfo(session.getId(), session.getTitle(), session.getGrade(),
                session.getSubject(), session.getTopic(), session.getStatus()), studentChoices, history);
    }

    @Transactional
    public int recordParticipants(Long sessionId, String teacherEmail, List<Long> submittedIds) {
        ClassroomSession session = sessionService.getTeacherOwnedSession(sessionId, teacherEmail);
        if (session.getStatus() == SessionStatus.PLANNED) {
            throw new IllegalStateException("Start the classroom session before recording participation.");
        }

        List<TeacherStudentService.StudentSummary> eligible = eligibleStudents(session, teacherEmail);
        Set<Long> eligibleIds = eligible.stream().map(TeacherStudentService.StudentSummary::id)
                .collect(Collectors.toSet());
        Set<Long> requestedIds = submittedIds == null ? Set.of()
                : submittedIds.stream().filter(id -> id != null).collect(Collectors.toCollection(LinkedHashSet::new));
        if (!eligibleIds.containsAll(requestedIds)) {
            throw new IllegalArgumentException("One or more selected students are outside your authorized classes.");
        }
        if (requestedIds.isEmpty()) return 0;

        Set<Long> alreadyRecorded = participationRepository
                .findByClassroomSession_IdAndStudent_IdIn(sessionId, requestedIds).stream()
                .map(p -> p.getStudent().getId()).collect(Collectors.toCollection(HashSet::new));
        List<ClassroomSessionParticipation> additions = new ArrayList<>();
        for (Long studentId : requestedIds) {
            if (alreadyRecorded.contains(studentId)) continue;
            User student = userRepository.findByIdAndRole(studentId, Role.STUDENT)
                    .orElseThrow(() -> new IllegalArgumentException("Selected student was not found."));
            additions.add(new ClassroomSessionParticipation(session, student));
        }
        participationRepository.saveAll(additions);
        return additions.size();
    }

    private List<TeacherStudentService.StudentSummary> eligibleStudents(
            ClassroomSession session, String teacherEmail) {
        return teacherStudentService.getStudents(teacherEmail, session.getGrade(), null, null).students();
    }

    public record SessionInfo(Long id, String title, Integer grade, String subject,
            String topic, SessionStatus status) { }
    public record EligibleStudent(Long id, String name, Integer grade, String section, boolean recorded) { }
    public record Participant(Long id, String name, Integer grade, String section, LocalDateTime participatedAt) { }
    public record ParticipationPage(SessionInfo session, List<EligibleStudent> eligibleStudents,
            List<Participant> participants) { }
}
