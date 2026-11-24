package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.ClassroomResource;
import com.smartclassroom.smart_classroom.entity.ClassroomResourceType;
import com.smartclassroom.smart_classroom.entity.ClassroomSession;
import com.smartclassroom.smart_classroom.entity.SessionStatus;
import com.smartclassroom.smart_classroom.repository.ClassroomResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClassroomPresentationService {

    private static final int MAX_TITLE_LENGTH = 180;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_CONTENT_LENGTH = 20000;
    private static final int MAX_URL_LENGTH = 2048;

    private final ClassroomResourceRepository resourceRepository;
    private final ClassroomSessionService sessionService;

    public ClassroomPresentationService(ClassroomResourceRepository resourceRepository,
            ClassroomSessionService sessionService) {
        this.resourceRepository = resourceRepository;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public ResourceManagerView getResourceManager(Long sessionId, String teacherEmail) {
        ClassroomSession session = sessionService.getTeacherOwnedSession(sessionId, teacherEmail);
        return new ResourceManagerView(sessionView(session), resourceViews(sessionId),
                session.getStatus() != SessionStatus.COMPLETED && !isPresentationActive(session));
    }

    @Transactional
    public Long addResource(Long sessionId, String teacherEmail, String title,
            ClassroomResourceType type, String description, String resourceUrl,
            String contentText) {
        ClassroomSession session = sessionService.getTeacherOwnedSession(sessionId, teacherEmail);
        ensureResourcesEditable(session);
        String cleanTitle = requiredText(title, MAX_TITLE_LENGTH, "Resource title");
        if (type == null) throw new IllegalArgumentException("Select a resource type.");
        String cleanDescription = optionalText(description, MAX_DESCRIPTION_LENGTH, "Description");
        String cleanUrl = optionalUrl(resourceUrl);
        String cleanContent = optionalText(contentText, MAX_CONTENT_LENGTH, "Resource text");
        validateResourceContent(type, cleanUrl, cleanContent);

        ClassroomResource resource = new ClassroomResource();
        resource.setClassroomSession(session);
        resource.setTitle(cleanTitle);
        resource.setResourceType(type);
        resource.setDescription(cleanDescription);
        resource.setResourceUrl(cleanUrl);
        resource.setContentText(cleanContent);
        resource.setDisplayOrder((int) resourceRepository.countByClassroomSession_Id(sessionId));
        return resourceRepository.save(resource).getId();
    }

    @Transactional
    public void deleteResource(Long sessionId, Long resourceId, String teacherEmail) {
        ClassroomSession session = sessionService.getTeacherOwnedSession(sessionId, teacherEmail);
        ensureResourcesEditable(session);
        ClassroomResource resource = resourceRepository
                .findByIdAndClassroomSession_Id(resourceId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found for this session."));
        resourceRepository.delete(resource);
        List<ClassroomResource> remaining = resourceRepository
                .findByClassroomSession_IdOrderByDisplayOrderAscIdAsc(sessionId);
        for (int index = 0; index < remaining.size(); index++) {
            remaining.get(index).setDisplayOrder(index);
        }
        resourceRepository.saveAll(remaining);
        if (session.getPresentationPosition() != null
                && session.getPresentationPosition() > remaining.size()) {
            session.setPresentationPosition(remaining.size());
        }
    }

    @Transactional
    public void startPresentation(Long sessionId, String teacherEmail) {
        ClassroomSession session = sessionService.getTeacherOwnedSession(sessionId, teacherEmail);
        if (session.getStatus() != SessionStatus.ONGOING) {
            throw new IllegalStateException("Start the classroom session before the presentation.");
        }
        if (isPresentationActive(session)) {
            throw new IllegalStateException("This presentation is already running.");
        }
        if (resourceRepository.countByClassroomSession_Id(sessionId) == 0) {
            throw new IllegalStateException("Add at least one learning resource before starting.");
        }
        session.setPresentationPosition(0);
        session.setPresentationStartedAt(LocalDateTime.now());
        session.setPresentationEndedAt(null);
    }

    @Transactional(readOnly = true)
    public PresentationView getPresentation(Long sessionId, String teacherEmail) {
        ClassroomSession session = sessionService.getTeacherOwnedSession(sessionId, teacherEmail);
        List<ClassroomResourceView> resources = resourceViews(sessionId);
        int position = normalizedPosition(session, resources.size());
        ClassroomResourceView current = position < resources.size() ? resources.get(position) : null;
        return new PresentationView(sessionView(session), current, position, resources.size(),
                isPresentationActive(session), isPresentationEnded(session));
    }

    @Transactional
    public void nextResource(Long sessionId, String teacherEmail) {
        ClassroomSession session = sessionService.getTeacherOwnedSession(sessionId, teacherEmail);
        ensurePresentationActive(session);
        long count = resourceRepository.countByClassroomSession_Id(sessionId);
        int position = normalizedPosition(session, (int) count);
        if (position < count) session.setPresentationPosition(position + 1);
    }

    @Transactional
    public void endPresentation(Long sessionId, String teacherEmail) {
        ClassroomSession session = sessionService.getTeacherOwnedSession(sessionId, teacherEmail);
        if (isPresentationActive(session)) {
            session.setPresentationEndedAt(LocalDateTime.now());
        }
    }

    private void ensureResourcesEditable(ClassroomSession session) {
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new IllegalStateException("Resources for a completed session cannot be changed.");
        }
        if (isPresentationActive(session)) {
            throw new IllegalStateException("End the presentation before changing its resources.");
        }
    }

    private void ensurePresentationActive(ClassroomSession session) {
        if (session.getStatus() != SessionStatus.ONGOING || !isPresentationActive(session)) {
            throw new IllegalStateException("This presentation is not currently running.");
        }
    }

    private static boolean isPresentationActive(ClassroomSession session) {
        return session.getPresentationStartedAt() != null
                && (session.getPresentationEndedAt() == null
                        || session.getPresentationEndedAt().isBefore(session.getPresentationStartedAt()));
    }

    private static boolean isPresentationEnded(ClassroomSession session) {
        return session.getPresentationStartedAt() != null
                && session.getPresentationEndedAt() != null
                && !session.getPresentationEndedAt().isBefore(session.getPresentationStartedAt());
    }

    private static int normalizedPosition(ClassroomSession session, int resourceCount) {
        int position = session.getPresentationPosition() == null ? 0 : session.getPresentationPosition();
        return Math.max(0, Math.min(position, resourceCount));
    }

    private List<ClassroomResourceView> resourceViews(Long sessionId) {
        return resourceRepository.findByClassroomSession_IdOrderByDisplayOrderAscIdAsc(sessionId)
                .stream().map(ClassroomPresentationService::resourceView).toList();
    }

    private static SessionContext sessionView(ClassroomSession session) {
        return new SessionContext(session.getId(), session.getTitle(), session.getGrade(),
                session.getSubject(), session.getTopic(), session.getStatus());
    }

    private static ClassroomResourceView resourceView(ClassroomResource resource) {
        return new ClassroomResourceView(resource.getId(), resource.getTitle(),
                resource.getResourceType(), resource.getDescription(), resource.getResourceUrl(),
                resource.getContentText(), resource.getDisplayOrder());
    }

    private static void validateResourceContent(ClassroomResourceType type,
            String url, String content) {
        boolean hasUrl = url != null;
        boolean hasText = content != null;
        switch (type) {
            case IMAGE, VIDEO, AUDIO -> {
                if (!hasUrl) throw new IllegalArgumentException(type + " resources require a media URL.");
            }
            case STORY -> {
                if (!hasText && !hasUrl) {
                    throw new IllegalArgumentException("A story needs text or a resource URL.");
                }
            }
            case OTHER -> {
                if (!hasText && !hasUrl) {
                    throw new IllegalArgumentException("Other learning material needs text or a resource URL.");
                }
            }
        }
    }

    private static String requiredText(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        String cleaned = value.trim();
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(label + " cannot exceed " + maxLength + " characters.");
        }
        return cleaned;
    }

    private static String optionalText(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value.trim();
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(label + " cannot exceed " + maxLength + " characters.");
        }
        return cleaned;
    }

    private static String optionalUrl(String value) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value.trim();
        if (cleaned.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("Resource URL is too long.");
        }
        try {
            URI uri = URI.create(cleaned);
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null
                    || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("Use a valid HTTP or HTTPS resource URL.");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Use a valid HTTP or HTTPS resource URL.");
        }
    }

    public record SessionContext(Long id, String title, Integer grade, String subject,
            String topic, SessionStatus status) { }

    public record ClassroomResourceView(Long id, String title, ClassroomResourceType resourceType,
            String description, String resourceUrl, String contentText, int displayOrder) { }

    public record ResourceManagerView(SessionContext session, List<ClassroomResourceView> resources,
            boolean editable) { }

    public record PresentationView(SessionContext session, ClassroomResourceView currentResource,
            int position, int resourceCount, boolean active, boolean ended) { }
}
