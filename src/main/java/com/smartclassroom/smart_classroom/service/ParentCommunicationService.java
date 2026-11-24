package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.*;
import com.smartclassroom.smart_classroom.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Private teacher-parent conversations and complaint workflows. */
@Service
public class ParentCommunicationService {
    private final UserRepository userRepository;
    private final GameAssignmentRepository assignmentRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ParentComplaintRepository complaintRepository;
    private final ComplaintMessageRepository complaintMessageRepository;
    private final TeacherStudentService teacherStudentService;

    public ParentCommunicationService(UserRepository userRepository,
            GameAssignmentRepository assignmentRepository,
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            ParentComplaintRepository complaintRepository,
            ComplaintMessageRepository complaintMessageRepository,
            TeacherStudentService teacherStudentService) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.complaintRepository = complaintRepository;
        this.complaintMessageRepository = complaintMessageRepository;
        this.teacherStudentService = teacherStudentService;
    }

    @Transactional(readOnly = true)
    public ParentDirectory getTeacherParentDirectory(String teacherEmail, String studentName,
            String parentName, Integer grade, String section) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        var authorizedStudents = teacherStudentService.getStudents(
                teacher.getEmail(), null, null, null).students();
        String parentFilter = cleanSearch(parentName);
        String studentFilter = cleanSearch(studentName);
        var matchingParentIds = authorizedStudents.stream()
                .filter(student -> grade == null || grade.equals(student.grade()))
                .filter(student -> section == null || section.isBlank()
                        || section.trim().equalsIgnoreCase(student.section()))
                .filter(student -> studentFilter.isEmpty()
                        || student.fullName().toLowerCase(Locale.ROOT).contains(studentFilter))
                .flatMap(student -> student.parents().stream())
                .filter(parent -> parentFilter.isEmpty()
                        || parent.fullName().toLowerCase(Locale.ROOT).contains(parentFilter)
                        || parent.email().toLowerCase(Locale.ROOT).contains(parentFilter))
                .map(TeacherStudentService.ParentContact::id)
                .collect(java.util.stream.Collectors.toSet());
        var linkedChildCounts = authorizedStudents.stream()
                .flatMap(student -> student.parents().stream().map(parent -> parent.id()))
                .collect(java.util.stream.Collectors.groupingBy(id -> id,
                        java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.counting(), Long::intValue)));
        var existingConversationKeys = conversationRepository
                .findByTeacher_IdOrderByUpdatedAtDesc(teacher.getId()).stream()
                .map(conversation -> conversation.getParticipant().getId() + ":"
                        + conversation.getStudent().getId())
                .collect(java.util.stream.Collectors.toSet());
        List<ParentDirectoryEntry> entries = authorizedStudents.stream()
                .flatMap(student -> student.parents().stream()
                        .filter(parent -> matchingParentIds.contains(parent.id()))
                        .map(parent -> new ParentDirectoryEntry(parent.id(), parent.fullName(),
                                parent.email(), student.id(), student.fullName(), student.grade(),
                                student.section(), linkedChildCounts.getOrDefault(parent.id(), 1),
                                false)))
                .map(entry -> new ParentDirectoryEntry(entry.parentId(), entry.parentName(),
                        entry.parentEmail(), entry.studentId(), entry.studentName(), entry.grade(),
                        entry.section(), entry.linkedChildCount(),
                        existingConversationKeys.contains(entry.parentId() + ":" + entry.studentId())))
                .sorted(Comparator.comparing(ParentDirectoryEntry::parentName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ParentDirectoryEntry::studentName,
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new ParentDirectory(entries);
    }

    @Transactional
    public Long openTeacherConversation(String teacherEmail, Long studentId, Long parentId) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        User student = requireAuthorizedStudent(teacher, studentId);
        User parent = requireUserById(parentId, Role.PARENT);
        requireLinkedParent(student, parent);
        Conversation conversation = conversationRepository
                .findByTeacher_IdAndParticipant_IdAndStudent_Id(
                        teacher.getId(), parent.getId(), student.getId())
                .orElseGet(() -> {
                    Conversation created = new Conversation();
                    created.setTeacher(teacher);
                    created.setParticipant(parent);
                    created.setStudent(student);
                    return conversationRepository.save(created);
                });
        return conversation.getId();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> getTeacherConversations(String teacherEmail) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        var students = teacherStudentService.getStudents(teacher.getEmail(), null, null, null).students();
        List<Conversation> conversations = conversationRepository
                .findByTeacher_IdOrderByUpdatedAtDesc(teacher.getId());
        return conversations.stream().filter(conversation -> students.stream()
                        .anyMatch(student -> student.id().equals(conversation.getStudent().getId())
                                && student.parents().stream().anyMatch(parent ->
                                        parent.id().equals(conversation.getParticipant().getId()))))
                .map(conversation -> conversationSummary(conversation,
                        messageRepository.countByConversation_IdAndReceiver_IdAndReadAtIsNull(
                                conversation.getId(), teacher.getId()), true))
                .toList();
    }

    @Transactional
    public ConversationView getTeacherConversation(String teacherEmail, Long conversationId) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        Conversation conversation = getConversation(conversationId);
        if (!conversation.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Conversation not found.");
        }
        User student = requireAuthorizedStudent(teacher, conversation.getStudent().getId());
        requireLinkedParent(student, conversation.getParticipant());
        markConversationRead(conversation, teacher);
        return conversationView(conversation);
    }

    @Transactional
    public ConversationView getParentConversation(String parentEmail, Long conversationId) {
        User parent = requireUser(parentEmail, Role.PARENT);
        Conversation conversation = getConversation(conversationId);
        if (!conversation.getParticipant().getId().equals(parent.getId())) {
            throw new IllegalArgumentException("Conversation not found.");
        }
        requireLinkedChild(parent, conversation.getStudent().getId());
        User student = requireAuthorizedStudent(conversation.getTeacher(), conversation.getStudent().getId());
        requireLinkedParent(student, parent);
        markConversationRead(conversation, parent);
        return conversationView(conversation);
    }

    @Transactional
    public void sendTeacherMessage(String teacherEmail, Long conversationId, String content) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        Conversation conversation = getConversation(conversationId);
        if (!conversation.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Conversation not found.");
        }
        User student = requireAuthorizedStudent(teacher, conversation.getStudent().getId());
        requireLinkedParent(student, conversation.getParticipant());
        saveConversationMessage(conversation, teacher, conversation.getParticipant(), content);
    }

    @Transactional
    public void sendParentMessage(String parentEmail, Long conversationId, String content) {
        User parent = requireUser(parentEmail, Role.PARENT);
        Conversation conversation = getConversation(conversationId);
        if (!conversation.getParticipant().getId().equals(parent.getId())) {
            throw new IllegalArgumentException("Conversation not found.");
        }
        requireLinkedChild(parent, conversation.getStudent().getId());
        User student = requireAuthorizedStudent(conversation.getTeacher(), conversation.getStudent().getId());
        requireLinkedParent(student, parent);
        saveConversationMessage(conversation, parent, conversation.getTeacher(), content);
    }

    @Transactional(readOnly = true)
    public List<ConversationSummary> getParentConversations(String parentEmail) {
        User parent = requireUser(parentEmail, Role.PARENT);
        return conversationRepository.findByParticipant_IdOrderByUpdatedAtDesc(parent.getId())
                .stream()
                .filter(conversation -> parent.getChildren().stream().anyMatch(child ->
                        child.getId().equals(conversation.getStudent().getId())))
                .map(conversation -> conversationSummary(conversation,
                        messageRepository.countByConversation_IdAndReceiver_IdAndReadAtIsNull(
                                conversation.getId(), parent.getId()), false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailableTeacher> getAvailableTeachers(String parentEmail, Long studentId) {
        User parent = requireUser(parentEmail, Role.PARENT);
        User student = requireLinkedChild(parent, studentId);
        if (student.getGrade() == null || student.getSection() == null
                || student.getSection().isBlank()) {
            return List.of();
        }
        return assignmentRepository.findByGradeAndSectionIgnoreCaseAndActiveTrueOrderByDeadlineAsc(
                        student.getGrade(), student.getSection().trim())
                .stream().map(GameAssignment::getTeacher).filter(teacher -> teacher != null)
                .distinct().sorted(Comparator.comparing(User::getFullName,
                        String.CASE_INSENSITIVE_ORDER))
                .map(teacher -> new AvailableTeacher(teacher.getId(), teacher.getFullName()))
                .toList();
    }

    @Transactional
    public Long openParentConversation(String parentEmail, Long studentId, Long teacherId) {
        User parent = requireUser(parentEmail, Role.PARENT);
        User student = requireLinkedChild(parent, studentId);
        User teacher = requireUserById(teacherId, Role.TEACHER);
        requireAuthorizedStudent(teacher, student.getId());
        requireLinkedParent(student, parent);
        Conversation conversation = conversationRepository
                .findByTeacher_IdAndParticipant_IdAndStudent_Id(
                        teacher.getId(), parent.getId(), student.getId())
                .orElseGet(() -> {
                    Conversation created = new Conversation();
                    created.setTeacher(teacher);
                    created.setParticipant(parent);
                    created.setStudent(student);
                    return conversationRepository.save(created);
                });
        return conversation.getId();
    }

    @Transactional(readOnly = true)
    public List<ParentChildOption> getParentChildren(String parentEmail) {
        User parent = requireUser(parentEmail, Role.PARENT);
        return parent.getChildren().stream().filter(child -> child.getRole() == Role.STUDENT)
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(child -> new ParentChildOption(child.getId(), child.getFullName(),
                        child.getGrade(), child.getSection()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplaintSummary> getTeacherComplaints(String teacherEmail) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        return complaintRepository.findByAssignedTeacher_IdOrderByUpdatedAtDesc(teacher.getId())
                .stream().filter(complaint -> complaintAuthorizedForTeacher(teacher, complaint))
                .map(complaint -> complaintSummary(complaint, teacher.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplaintSummary> getParentComplaints(String parentEmail) {
        User parent = requireUser(parentEmail, Role.PARENT);
        return complaintRepository.findByParent_IdOrderByUpdatedAtDesc(parent.getId())
                .stream().filter(complaint -> parent.getChildren().stream().anyMatch(child ->
                        child.getId().equals(complaint.getStudent().getId())))
                .map(complaint -> complaintSummary(complaint, parent.getId()))
                .toList();
    }

    @Transactional
    public ComplaintView getTeacherComplaint(String teacherEmail, Long complaintId) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        ParentComplaint complaint = getComplaint(complaintId);
        if (!complaint.getAssignedTeacher().getId().equals(teacher.getId())
                || !complaintAuthorizedForTeacher(teacher, complaint)) {
            throw new IllegalArgumentException("Complaint not found.");
        }
        markComplaintRead(complaint, teacher);
        return complaintView(complaint);
    }

    @Transactional
    public ComplaintView getParentComplaint(String parentEmail, Long complaintId) {
        User parent = requireUser(parentEmail, Role.PARENT);
        ParentComplaint complaint = getComplaint(complaintId);
        if (!complaint.getParent().getId().equals(parent.getId())) {
            throw new IllegalArgumentException("Complaint not found.");
        }
        requireLinkedChild(parent, complaint.getStudent().getId());
        markComplaintRead(complaint, parent);
        return complaintView(complaint);
    }

    @Transactional
    public Long createComplaint(String parentEmail, Long studentId, Long teacherId,
            String title, String description) {
        User parent = requireUser(parentEmail, Role.PARENT);
        User student = requireLinkedChild(parent, studentId);
        User teacher = requireUserById(teacherId, Role.TEACHER);
        requireAuthorizedStudent(teacher, studentId);
        requireLinkedParent(student, parent);
        String cleanTitle = validateText(title, 180, "Complaint title");
        String cleanDescription = validateText(description, 4000, "Description");
        ParentComplaint complaint = new ParentComplaint();
        complaint.setParent(parent);
        complaint.setStudent(student);
        complaint.setAssignedTeacher(teacher);
        complaint.setTitle(cleanTitle);
        complaint.setDescription(cleanDescription);
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint = complaintRepository.save(complaint);
        saveComplaintMessage(complaint, parent, cleanDescription, ComplaintStatus.OPEN);
        return complaint.getId();
    }

    @Transactional
    public void replyToComplaintAsTeacher(String teacherEmail, Long complaintId, String content) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        ParentComplaint complaint = getComplaint(complaintId);
        if (!complaint.getAssignedTeacher().getId().equals(teacher.getId())
                || !complaintAuthorizedForTeacher(teacher, complaint)) {
            throw new IllegalArgumentException("Complaint not found.");
        }
        saveComplaintMessage(complaint, teacher, validateText(content, 4000, "Reply"), null);
    }

    @Transactional
    public void replyToComplaintAsParent(String parentEmail, Long complaintId, String content) {
        User parent = requireUser(parentEmail, Role.PARENT);
        ParentComplaint complaint = getComplaint(complaintId);
        if (!complaint.getParent().getId().equals(parent.getId())) {
            throw new IllegalArgumentException("Complaint not found.");
        }
        requireLinkedChild(parent, complaint.getStudent().getId());
        saveComplaintMessage(complaint, parent, validateText(content, 4000, "Reply"), null);
    }

    @Transactional
    public void updateComplaintStatus(String teacherEmail, Long complaintId, ComplaintStatus status) {
        User teacher = requireUser(teacherEmail, Role.TEACHER);
        ParentComplaint complaint = getComplaint(complaintId);
        if (!complaint.getAssignedTeacher().getId().equals(teacher.getId())
                || !complaintAuthorizedForTeacher(teacher, complaint)) {
            throw new IllegalArgumentException("Complaint not found.");
        }
        if (status == null) throw new IllegalArgumentException("Choose a valid complaint status.");
        complaint.setStatus(status);
        complaint.setUpdatedAt(LocalDateTime.now());
        complaintRepository.save(complaint);
        saveComplaintMessage(complaint, teacher, "Status updated to " + status.name() + ".", status);
    }

    private User requireAuthorizedStudent(User teacher, Long studentId) {
        TeacherStudentService.StudentDetail detail = teacherStudentService
                .getStudent(teacher.getEmail(), studentId);
        return userRepository.findByIdAndRole(detail.student().id(), Role.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException("Student not found in your assigned classes."));
    }

    private User requireLinkedChild(User parent, Long studentId) {
        return parent.getChildren().stream()
                .filter(child -> child.getId().equals(studentId) && child.getRole() == Role.STUDENT)
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "That student is not linked to your parent account."));
    }

    private void requireLinkedParent(User student, User parent) {
        if (parent.getRole() != Role.PARENT || student.getParents().stream()
                .noneMatch(linked -> linked.getId().equals(parent.getId()))) {
            throw new IllegalArgumentException("This parent is not linked to the selected student.");
        }
    }

    private boolean complaintAuthorizedForTeacher(User teacher, ParentComplaint complaint) {
        try {
            User student = requireAuthorizedStudent(teacher, complaint.getStudent().getId());
            requireLinkedParent(student, complaint.getParent());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private User requireUser(String email, Role role) {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(user -> user.getRole() == role)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
    }

    private User requireUserById(Long id, Role role) {
        return userRepository.findByIdAndRole(id, role)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
    }

    private Conversation getConversation(Long id) {
        return conversationRepository.findWithParticipantsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found."));
    }

    private ParentComplaint getComplaint(Long id) {
        return complaintRepository.findWithParticipantsById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found."));
    }

    private void saveConversationMessage(Conversation conversation, User sender,
            User receiver, String content) {
        ConversationMessage message = new ConversationMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(validateText(content, 4000, "Message"));
        messageRepository.save(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private void markConversationRead(Conversation conversation, User reader) {
        List<ConversationMessage> unread = messageRepository
                .findByConversation_IdAndReceiver_IdAndReadAtIsNull(conversation.getId(), reader.getId());
        if (!unread.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            unread.forEach(message -> message.setReadAt(now));
            messageRepository.saveAll(unread);
        }
    }

    private ConversationView conversationView(Conversation conversation) {
        List<MessageView> messages = messageRepository
                .findByConversation_IdOrderBySentAtAsc(conversation.getId()).stream()
                .map(message -> new MessageView(message.getSender().getId(),
                        message.getSender().getFullName(), message.getSender().getRole(),
                        message.getReceiver().getId(), message.getContent(), message.getSentAt(),
                        message.getReadAt()))
                .toList();
        return new ConversationView(conversation.getId(), conversation.getTeacher().getFullName(),
                conversation.getParticipant().getFullName(), conversation.getStudent().getId(),
                conversation.getStudent().getFullName(), messages);
    }

    private ConversationSummary conversationSummary(Conversation conversation,
            long unreadCount, boolean teacherView) {
        User other = teacherView ? conversation.getParticipant() : conversation.getTeacher();
        return new ConversationSummary(conversation.getId(), other.getFullName(),
                conversation.getStudent().getId(), conversation.getStudent().getFullName(),
                conversation.getUpdatedAt(), unreadCount);
    }

    private void saveComplaintMessage(ParentComplaint complaint, User sender,
            String content, ComplaintStatus statusAfter) {
        ComplaintMessage message = new ComplaintMessage();
        message.setComplaint(complaint);
        message.setSender(sender);
        message.setContent(content);
        message.setStatusAfter(statusAfter);
        complaintMessageRepository.save(message);
        complaint.setUpdatedAt(LocalDateTime.now());
        complaintRepository.save(complaint);
    }

    private void markComplaintRead(ParentComplaint complaint, User reader) {
        List<ComplaintMessage> unread = complaintMessageRepository
                .findByComplaint_IdAndSender_IdNotAndReadAtIsNull(complaint.getId(), reader.getId());
        if (!unread.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            unread.forEach(message -> message.setReadAt(now));
            complaintMessageRepository.saveAll(unread);
        }
    }

    private ComplaintView complaintView(ParentComplaint complaint) {
        List<ComplaintMessageView> history = complaintMessageRepository
                .findByComplaint_IdOrderBySentAtAsc(complaint.getId()).stream()
                .map(message -> new ComplaintMessageView(message.getSender().getFullName(),
                        message.getSender().getRole(), message.getContent(), message.getSentAt(),
                        message.getReadAt(), message.getStatusAfter()))
                .toList();
        return new ComplaintView(complaint.getId(), complaint.getTitle(),
                complaint.getDescription(), complaint.getStatus(), complaint.getCreatedAt(),
                complaint.getUpdatedAt(), complaint.getParent().getFullName(),
                complaint.getStudent().getId(), complaint.getStudent().getFullName(),
                complaint.getAssignedTeacher().getFullName(), history);
    }

    private ComplaintSummary complaintSummary(ParentComplaint complaint, Long viewerId) {
        return new ComplaintSummary(complaint.getId(), complaint.getTitle(),
                complaint.getStudent().getId(), complaint.getStudent().getFullName(),
                complaint.getParent().getFullName(), complaint.getStatus(),
                complaint.getCreatedAt(), complaint.getUpdatedAt(),
                complaintMessageRepository.countByComplaint_IdAndSender_IdNotAndReadAtIsNull(
                        complaint.getId(), viewerId));
    }

    private static String validateText(String value, int max, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        String cleaned = value.trim();
        if (cleaned.length() > max) throw new IllegalArgumentException(label + " cannot exceed " + max + " characters.");
        return cleaned;
    }

    private static String cleanSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ParentDirectoryEntry(Long parentId, String parentName, String parentEmail,
            Long studentId, String studentName, Integer grade, String section, int linkedChildCount,
            boolean conversationStarted) { }
    public record ParentDirectory(List<ParentDirectoryEntry> entries) { }
    public record AvailableTeacher(Long id, String fullName) { }
    public record ParentChildOption(Long id, String fullName, Integer grade, String section) { }
    public record ConversationSummary(Long id, String otherName, Long studentId,
            String studentName, LocalDateTime updatedAt, long unreadCount) { }
    public record MessageView(Long senderId, String senderName, Role senderRole,
            Long receiverId, String content, LocalDateTime sentAt, LocalDateTime readAt) { }
    public record ConversationView(Long id, String teacherName, String parentName,
            Long studentId, String studentName, List<MessageView> messages) { }
    public record ComplaintSummary(Long id, String title, Long studentId, String studentName,
            String parentName, ComplaintStatus status, LocalDateTime createdAt,
            LocalDateTime updatedAt, long unreadCount) { }
    public record ComplaintMessageView(String senderName, Role senderRole, String content,
            LocalDateTime sentAt, LocalDateTime readAt, ComplaintStatus statusAfter) { }
    public record ComplaintView(Long id, String title, String description, ComplaintStatus status,
            LocalDateTime createdAt, LocalDateTime updatedAt, String parentName, Long studentId,
            String studentName, String assignedTeacherName, List<ComplaintMessageView> history) { }
}
