package org.example.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * מייצג סקר שלם.
 * כולל שאלות, משתתפים ומצב הסקר.
 */
public class Survey {

    public enum Status {
        PENDING,    // נוצר אך טרם נשלח
        SCHEDULED,  // מתוזמן לשליחה בעיכוב
        ACTIVE,     // פעיל — נשלח למשתתפים
        COMPLETED   // הסתיים
    }

    private final List<SurveyQuestion> questions;
    private final Map<Long, SurveyParticipant> participants; // chatId -> participant
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledSendTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Survey(List<SurveyQuestion> questions) {
        this.questions = new ArrayList<>(questions);
        this.participants = new ConcurrentHashMap<>();
        this.status = Status.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // --- Status Management ---

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public boolean isScheduled() {
        return status == Status.SCHEDULED;
    }

    // --- Time Management ---

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getScheduledSendTime() {
        return scheduledSendTime;
    }

    public void setScheduledSendTime(LocalDateTime scheduledSendTime) {
        this.scheduledSendTime = scheduledSendTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    // --- Questions ---

    public List<SurveyQuestion> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public int getQuestionCount() {
        return questions.size();
    }

    public SurveyQuestion getQuestion(int index) {
        if (index >= 0 && index < questions.size()) {
            return questions.get(index);
        }
        return null;
    }

    // --- Participants ---

    /**
     * מוסיף משתתפים לסקר מרשימת חברי הקהילה.
     */
    public void setParticipants(List<CommunityMember> members) {
        participants.clear();
        for (CommunityMember member : members) {
            participants.put(member.getChatId(),
                    new SurveyParticipant(member, questions.size()));
        }
    }

    public SurveyParticipant getParticipant(long chatId) {
        return participants.get(chatId);
    }

    public Collection<SurveyParticipant> getParticipants() {
        return participants.values();
    }

    public int getParticipantCount() {
        return participants.size();
    }

    /**
     * מחזיר רשימת משתתפים ממוינת לפי שם.
     */
    public List<SurveyParticipant> getParticipantsSorted() {
        List<SurveyParticipant> sorted = new ArrayList<>(participants.values());
        sorted.sort(Comparator.comparing(p -> p.getMember().getDisplayName()));
        return sorted;
    }

    // --- Statistics ---

    /**
     * מספר המשתתפים שהשלימו את כל השאלות.
     */
    public int getCompletedCount() {
        return (int) participants.values().stream()
                .filter(SurveyParticipant::hasCompleted)
                .count();
    }

    /**
     * מספר המשתתפים שטרם השלימו.
     */
    public int getNotCompletedCount() {
        return getParticipantCount() - getCompletedCount();
    }

    /**
     * בודק אם כל המשתתפים השלימו את הסקר.
     */
    public boolean allCompleted() {
        return !participants.isEmpty() &&
                participants.values().stream().allMatch(SurveyParticipant::hasCompleted);
    }

    /**
     * מחזיר רשימת משתתפים שטרם השלימו ולא קיבלו תזכורת.
     */
    public List<SurveyParticipant> getParticipantsNeedingReminder() {
        return participants.values().stream()
                .filter(p -> !p.hasCompleted() && !p.isReminderSent())
                .toList();
    }
}
