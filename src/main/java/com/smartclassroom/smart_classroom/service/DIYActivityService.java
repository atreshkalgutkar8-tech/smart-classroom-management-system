package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.DIYActivity;
import com.smartclassroom.smart_classroom.entity.DIYActivityParticipation;
import com.smartclassroom.smart_classroom.entity.Role;
import com.smartclassroom.smart_classroom.entity.User;
import com.smartclassroom.smart_classroom.repository.DIYActivityParticipationRepository;
import com.smartclassroom.smart_classroom.repository.DIYActivityRepository;
import com.smartclassroom.smart_classroom.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DIYActivityService {

    private static final Set<String> SUBJECTS = Set.of("Mathematics", "Kannada", "English", "EVS");

    private final DIYActivityRepository activityRepository;
    private final DIYActivityParticipationRepository participationRepository;
    private final UserRepository userRepository;

    public DIYActivityService(DIYActivityRepository activityRepository,
            DIYActivityParticipationRepository participationRepository, UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.participationRepository = participationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ActivitySummary> teacherCatalogue() {
        return activityRepository.findAllByOrderByGradeAscSubjectAscTopicAscTitleAsc().stream()
                .map(ActivitySummary::from).toList();
    }

    @Transactional
    public void create(String teacherEmail, String title, Integer grade, String subject, String topic,
            String learningObjective, String requiredMaterials, String instructions) {
        User teacher = findUser(teacherEmail, Role.TEACHER, "Teacher account not found.");
        validateText(title, "Activity title", 180);
        validateText(topic, "Topic", 180);
        validateText(learningObjective, "Learning objective", 1000);
        validateText(requiredMaterials, "Required materials", 10000);
        validateText(instructions, "Instructions", 20000);
        if (grade == null || grade < 1 || grade > 5) {
            throw new IllegalArgumentException("Grade must be between 1 and 5.");
        }
        if (subject == null || !SUBJECTS.contains(subject.trim())) {
            throw new IllegalArgumentException("Select a valid subject.");
        }
        activityRepository.save(new DIYActivity(title.trim(), grade, subject.trim(), topic.trim(),
                learningObjective.trim(), requiredMaterials.trim(), instructions.trim(), teacher));
    }

    @Transactional(readOnly = true)
    public ActivityDetail teacherActivity(Long activityId) {
        return activityRepository.findById(activityId).map(ActivityDetail::from)
                .orElseThrow(() -> new IllegalArgumentException("DIY activity not found."));
    }

    @Transactional(readOnly = true)
    public List<StudentActivity> studentCatalogue(String studentEmail) {
        User student = findUser(studentEmail, Role.STUDENT, "Student account not found.");
        if (student.getGrade() == null || student.getGrade() < 1 || student.getGrade() > 5) {
            return List.of();
        }
        Set<Long> completedIds = participationRepository.findCompletedActivityIdsByStudent(student.getId())
                .stream().collect(Collectors.toSet());
        return activityRepository.findByGradeOrderBySubjectAscTopicAscTitleAsc(student.getGrade()).stream()
                .map(activity -> new StudentActivity(ActivitySummary.from(activity),
                        completedIds.contains(activity.getId()), null)).toList();
    }

    @Transactional(readOnly = true)
    public List<CompletedActivity> studentCompletionHistory(String studentEmail) {
        User student = findUser(studentEmail, Role.STUDENT, "Student account not found.");
        return participationRepository.findByStudent_IdOrderByCompletedAtDesc(student.getId()).stream()
                .map(record -> new CompletedActivity(record.getActivity().getTitle(),
                        record.getActivity().getSubject(), record.getActivity().getTopic(),
                        record.getCompletedAt())).toList();
    }

    @Transactional(readOnly = true)
    public StudentActivity studentActivity(Long activityId, String studentEmail) {
        User student = findUser(studentEmail, Role.STUDENT, "Student account not found.");
        if (student.getGrade() == null) throw new IllegalArgumentException("Your grade is not set.");
        DIYActivity activity = activityRepository.findByIdAndGrade(activityId, student.getGrade())
                .orElseThrow(() -> new IllegalArgumentException("Activity not found for your grade."));
        var completion = participationRepository.findByActivity_IdAndStudent_Id(activityId, student.getId());
        return new StudentActivity(ActivitySummary.from(activity), completion.isPresent(),
                completion.map(DIYActivityParticipation::getCompletedAt).orElse(null));
    }

    @Transactional
    public boolean complete(Long activityId, String studentEmail) {
        User student = findUser(studentEmail, Role.STUDENT, "Student account not found.");
        if (student.getGrade() == null) throw new IllegalArgumentException("Your grade is not set.");
        DIYActivity activity = activityRepository.findByIdAndGrade(activityId, student.getGrade())
                .orElseThrow(() -> new IllegalArgumentException("Activity not found for your grade."));
        if (participationRepository.existsByActivity_IdAndStudent_Id(activityId, student.getId())) return false;
        participationRepository.save(new DIYActivityParticipation(activity, student));
        return true;
    }

    private User findUser(String email, Role role, String message) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(user -> user.getRole() == role)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    private void validateText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        if (value.trim().length() > maxLength) throw new IllegalArgumentException(label + " is too long.");
    }

    public record ActivitySummary(Long id, String title, Integer grade, String subject, String topic,
            String learningObjective, String createdBy, LocalDateTime createdAt) {
        static ActivitySummary from(DIYActivity activity) {
            return new ActivitySummary(activity.getId(), activity.getTitle(), activity.getGrade(),
                    activity.getSubject(), activity.getTopic(), activity.getLearningObjective(),
                    activity.getCreatedByTeacher().getFullName(), activity.getCreatedAt());
        }
    }

    public record ActivityDetail(ActivitySummary activity, String requiredMaterials, String instructions) {
        static ActivityDetail from(DIYActivity activity) {
            return new ActivityDetail(ActivitySummary.from(activity), activity.getRequiredMaterials(),
                    activity.getInstructions());
        }
    }

    public record StudentActivity(ActivitySummary activity, boolean completed, LocalDateTime completedAt) { }
    public record CompletedActivity(String title, String subject, String topic, LocalDateTime completedAt) { }
}
