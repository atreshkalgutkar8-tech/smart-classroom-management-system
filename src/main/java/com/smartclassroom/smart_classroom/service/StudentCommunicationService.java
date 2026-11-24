package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.Role;
import com.smartclassroom.smart_classroom.entity.StudentConversation;
import com.smartclassroom.smart_classroom.entity.StudentConversationMessage;
import com.smartclassroom.smart_classroom.entity.User;
import com.smartclassroom.smart_classroom.repository.StudentConversationMessageRepository;
import com.smartclassroom.smart_classroom.repository.StudentConversationRepository;
import com.smartclassroom.smart_classroom.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Private one-to-one communication between a teacher and an authorized student. */
@Service
public class StudentCommunicationService {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final StudentConversationRepository conversationRepository;
    private final StudentConversationMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TeacherStudentService teacherStudentService;

    public StudentCommunicationService(StudentConversationRepository conversationRepository,
            StudentConversationMessageRepository messageRepository,
            UserRepository userRepository, TeacherStudentService teacherStudentService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.teacherStudentService = teacherStudentService;
    }

    @Transactional
    public Long openOrCreateTeacherConversation(Long studentId) {
        User teacher = currentUser(Role.TEACHER);
        User student = requireTeacherAuthorizedStudent(teacher, studentId);
        return conversationRepository.findByTeacher_IdAndStudent_Id(teacher.getId(), student.getId())
                .map(StudentConversation::getId)
                .orElseGet(() -> {
                    StudentConversation conversation = new StudentConversation();
                    conversation.setTeacher(teacher);
                    conversation.setStudent(student);
                    return conversationRepository.save(conversation).getId();
                });
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> getTeacherConversations() {
        User teacher = currentUser(Role.TEACHER);
        Set<Long> authorizedStudentIds = teacherStudentService
                .getStudents(teacher.getEmail(), null, null, null).students().stream()
                .map(TeacherStudentService.StudentSummary::id)
                .collect(Collectors.toSet());
        return conversationRepository.findByTeacher_IdOrderByUpdatedAtDesc(teacher.getId()).stream()
                .filter(conversation -> authorizedStudentIds.contains(conversation.getStudent().getId()))
                .map(conversation -> summary(conversation,
                        messageRepository.countByConversation_IdAndConversation_Teacher_IdAndReceiver_IdAndReadAtIsNull(
                                conversation.getId(), teacher.getId(), teacher.getId()), true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> getStudentConversations() {
        User student = currentUser(Role.STUDENT);
        return conversationRepository.findByStudent_IdOrderByUpdatedAtDesc(student.getId()).stream()
                .map(conversation -> summary(conversation,
                        messageRepository.countByConversation_IdAndConversation_Student_IdAndReceiver_IdAndReadAtIsNull(
                                conversation.getId(), student.getId(), student.getId()), false))
                .toList();
    }

    /** Retrieves a teacher's authorized thread and marks received messages as read. */
    @Transactional
    public ConversationView getTeacherConversation(Long conversationId) {
        User teacher = currentUser(Role.TEACHER);
        StudentConversation conversation = requireTeacherConversation(conversationId, teacher);
        requireTeacherOwnsConversation(teacher, conversation);
        requireTeacherAuthorizedStudent(teacher, conversation.getStudent().getId());
        markReceivedMessagesAsRead(conversation, teacher);
        return view(conversation, true);
    }

    /** Retrieves only the authenticated student's thread and marks received messages as read. */
    @Transactional
    public ConversationView getStudentConversation(Long conversationId) {
        User student = currentUser(Role.STUDENT);
        StudentConversation conversation = requireStudentConversation(conversationId, student);
        requireStudentOwnsConversation(student, conversation);
        markReceivedMessagesAsRead(conversation, student);
        return view(conversation, false);
    }

    @Transactional
    public void sendTeacherMessage(Long conversationId, String content) {
        User teacher = currentUser(Role.TEACHER);
        StudentConversation conversation = requireTeacherConversation(conversationId, teacher);
        requireTeacherOwnsConversation(teacher, conversation);
        requireTeacherAuthorizedStudent(teacher, conversation.getStudent().getId());
        saveMessage(conversation, teacher, conversation.getStudent(), content);
    }

    @Transactional
    public void sendStudentMessage(Long conversationId, String content) {
        User student = currentUser(Role.STUDENT);
        StudentConversation conversation = requireStudentConversation(conversationId, student);
        requireStudentOwnsConversation(student, conversation);
        saveMessage(conversation, student, conversation.getTeacher(), content);
    }

    @Transactional
    public void markTeacherMessagesRead(Long conversationId) {
        User teacher = currentUser(Role.TEACHER);
        StudentConversation conversation = requireTeacherConversation(conversationId, teacher);
        requireTeacherOwnsConversation(teacher, conversation);
        requireTeacherAuthorizedStudent(teacher, conversation.getStudent().getId());
        markReceivedMessagesAsRead(conversation, teacher);
    }

    @Transactional
    public void markStudentMessagesRead(Long conversationId) {
        User student = currentUser(Role.STUDENT);
        StudentConversation conversation = requireStudentConversation(conversationId, student);
        requireStudentOwnsConversation(student, conversation);
        markReceivedMessagesAsRead(conversation, student);
    }

    @Transactional(readOnly = true)
    public long countUnreadForTeacher() {
        User teacher = currentUser(Role.TEACHER);
        Set<Long> authorizedStudentIds = teacherStudentService
                .getStudents(teacher.getEmail(), null, null, null).students().stream()
                .map(TeacherStudentService.StudentSummary::id)
                .collect(Collectors.toSet());
        return conversationRepository.findByTeacher_IdOrderByUpdatedAtDesc(teacher.getId()).stream()
                .filter(conversation -> authorizedStudentIds.contains(conversation.getStudent().getId()))
                .mapToLong(conversation -> messageRepository
                        .countByConversation_IdAndConversation_Teacher_IdAndReceiver_IdAndReadAtIsNull(
                                conversation.getId(), teacher.getId(), teacher.getId()))
                .sum();
    }

    @Transactional(readOnly = true)
    public long countUnreadForStudent() {
        User student = currentUser(Role.STUDENT);
        return messageRepository.countByConversation_Student_IdAndReceiver_IdAndReadAtIsNull(
                student.getId(), student.getId());
    }

    private User requireTeacherAuthorizedStudent(User teacher, Long studentId) {
        TeacherStudentService.StudentDetail detail = teacherStudentService
                .getStudent(teacher.getEmail(), studentId);
        return userRepository.findByIdAndRole(detail.student().id(), Role.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Student not found in your assigned classes."));
    }

    private StudentConversation requireTeacherConversation(Long conversationId, User teacher) {
        return conversationRepository.findByIdAndTeacher_Id(conversationId, teacher.getId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));
    }

    private StudentConversation requireStudentConversation(Long conversationId, User student) {
        return conversationRepository.findByIdAndStudent_Id(conversationId, student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));
    }

    private void requireTeacherOwnsConversation(User teacher, StudentConversation conversation) {
        if (!conversation.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Conversation not found.");
        }
    }

    private void requireStudentOwnsConversation(User student, StudentConversation conversation) {
        if (!conversation.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException("Conversation not found.");
        }
    }

    private User currentUser(Role requiredRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalArgumentException("Authenticated account not found.");
        }
        return userRepository.findByEmail(authentication.getName().trim().toLowerCase(Locale.ROOT))
                .filter(user -> user.getRole() == requiredRole)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated account not found."));
    }

    private void saveMessage(StudentConversation conversation, User sender,
            User receiver, String content) {
        String normalized = validateMessage(content);
        StudentConversationMessage message = new StudentConversationMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(normalized);
        messageRepository.save(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private void markReceivedMessagesAsRead(StudentConversation conversation, User reader) {
        List<StudentConversationMessage> unread;
        if (reader.getId().equals(conversation.getTeacher().getId())) {
            unread = messageRepository
                    .findByConversation_IdAndConversation_Teacher_IdAndReceiver_IdAndReadAtIsNull(
                            conversation.getId(), reader.getId(), reader.getId());
        } else {
            unread = messageRepository
                    .findByConversation_IdAndConversation_Student_IdAndReceiver_IdAndReadAtIsNull(
                            conversation.getId(), reader.getId(), reader.getId());
        }
        if (!unread.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            unread.forEach(message -> message.setReadAt(now));
            messageRepository.saveAll(unread);
        }
    }

    private ConversationSummary summary(StudentConversation conversation, long unreadCount,
            boolean teacherView) {
        StudentConversationMessage latest = teacherView
                ? messageRepository.findFirstByConversation_IdAndConversation_Teacher_IdOrderBySentAtDesc(
                        conversation.getId(), conversation.getTeacher().getId()).orElse(null)
                : messageRepository.findFirstByConversation_IdAndConversation_Student_IdOrderBySentAtDesc(
                        conversation.getId(), conversation.getStudent().getId()).orElse(null);
        return new ConversationSummary(conversation.getId(), conversation.getTeacher().getId(),
                conversation.getTeacher().getFullName(), conversation.getStudent().getId(),
                conversation.getStudent().getFullName(), conversation.getStudent().getGrade(),
                conversation.getStudent().getSection(), latest == null ? null : latest.getContent(),
                latest == null ? null : latest.getSentAt(), unreadCount);
    }

    private ConversationView view(StudentConversation conversation, boolean teacherView) {
        List<StudentConversationMessage> records = teacherView
                ? messageRepository.findByConversation_IdAndConversation_Teacher_IdOrderBySentAtAsc(
                        conversation.getId(), conversation.getTeacher().getId())
                : messageRepository.findByConversation_IdAndConversation_Student_IdOrderBySentAtAsc(
                        conversation.getId(), conversation.getStudent().getId());
        List<MessageView> messages = records.stream()
                .map(message -> new MessageView(message.getSender().getFullName(),
                        message.getSender().getRole(), message.getContent(), message.getSentAt(),
                        message.getReadAt()))
                .toList();
        return new ConversationView(conversation.getId(), conversation.getTeacher().getFullName(),
                conversation.getStudent().getFullName(), messages);
    }

    private static String validateMessage(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message cannot exceed 4000 characters.");
        }
        return normalized;
    }

    public record ConversationSummary(Long id, Long teacherId, String teacherName,
            Long studentId, String studentName, Integer grade, String section,
            String lastMessage, LocalDateTime lastMessageAt, long unreadCount) { }

    public record MessageView(String senderName, Role senderRole, String content,
            LocalDateTime sentAt, LocalDateTime readAt) { }

    public record ConversationView(Long id, String teacherName, String studentName,
            List<MessageView> messages) { }
}
