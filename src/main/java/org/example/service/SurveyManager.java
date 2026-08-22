package org.example.service;

import org.example.model.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * מנהל סקרים — אחראי על מחזור החיים המלא של סקר:
 * יצירה, תזמון, שליחה, איסוף תשובות, תזכורות וסגירה.
 */
public class SurveyManager {

    /**
     * ממשק listener לאירועי סקר.
     */
    public interface SurveyListener {
        /** סקר נוצר */
        void onSurveyCreated(Survey survey);

        /** סקר נשלח למשתתפים */
        void onSurveySent(Survey survey);

        /** תשובה נרשמה */
        void onAnswerRecorded(Survey survey, SurveyParticipant participant, int questionIndex);

        /** סקר הסתיים */
        void onSurveyClosed(Survey survey);

        /** עדכון countdown (כל שנייה) */
        void onCountdownUpdate(long secondsRemaining, String type);

        /** סקר מתוזמן — הספירה לאחור לפני שליחה */
        void onScheduledCountdownUpdate(long secondsRemaining);

        /** תזכורת נשלחה */
        void onRemindersSent(int count);
    }

    /**
     * ממשק לשליחת הודעות דרך הבוט.
     */
    public interface BotMessageSender {
        void sendSurveyToParticipant(long chatId, Survey survey);
        void sendReminder(long chatId, Survey survey);
        void sendSurveyClosed(long chatId);
    }

    private final CommunityManager communityManager;
    private final List<SurveyListener> listeners = new CopyOnWriteArrayList<>();
    private BotMessageSender botMessageSender;

    private Survey activeSurvey;
    private Timer surveyTimer;
    private Timer countdownTimer;
    private Timer scheduledSendTimer;
    private Timer scheduledCountdownTimer;
    private boolean reminderSent = false;

    private static final int SURVEY_DURATION_MINUTES = 5;
    private static final int REMINDER_AFTER_MINUTES = 3;

    public SurveyManager(CommunityManager communityManager) {
        this.communityManager = communityManager;
    }

    public void setBotMessageSender(BotMessageSender sender) {
        this.botMessageSender = sender;
    }

    // --- Survey Lifecycle ---

    /**
     * יוצר סקר חדש עם השאלות הנתונות.
     * @throws IllegalStateException אם כבר קיים סקר פעיל
     */
    public synchronized Survey createSurvey(List<SurveyQuestion> questions) {
        if (hasActiveSurvey()) {
            throw new IllegalStateException("לא ניתן ליצור סקר חדש — קיים סקר פעיל.");
        }
        if (communityManager.getMemberCount() < 3) {
            throw new IllegalStateException("נדרשים לפחות 3 חברים בקהילה כדי להתחיל סקר.");
        }

        activeSurvey = new Survey(questions);
        activeSurvey.setParticipants(communityManager.getMembers());

        notifyListeners(l -> l.onSurveyCreated(activeSurvey));

        return activeSurvey;
    }

    /**
     * מתחיל את הסקר — שולח מיידית או בעיכוב.
     * @param delayMinutes 0 לשליחה מיידית, אחרת מספר דקות לעיכוב
     */
    public synchronized void startSurvey(int delayMinutes) {
        if (activeSurvey == null) {
            throw new IllegalStateException("לא נוצר סקר.");
        }

        if (delayMinutes <= 0) {
            sendSurveyNow();
        } else {
            scheduleSurvey(delayMinutes);
        }
    }

    private void scheduleSurvey(int delayMinutes) {
        activeSurvey.setStatus(Survey.Status.SCHEDULED);
        LocalDateTime sendTime = LocalDateTime.now().plusMinutes(delayMinutes);
        activeSurvey.setScheduledSendTime(sendTime);

        // Countdown לפני שליחה
        scheduledCountdownTimer = new Timer("scheduled-countdown", true);
        scheduledCountdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long secondsLeft = LocalDateTime.now().until(sendTime, ChronoUnit.SECONDS);
                if (secondsLeft <= 0) {
                    scheduledCountdownTimer.cancel();
                    return;
                }
                notifyListeners(l -> l.onScheduledCountdownUpdate(secondsLeft));
            }
        }, 0, 1000);

        // טיימר לשליחה
        scheduledSendTimer = new Timer("scheduled-send", true);
        scheduledSendTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (SurveyManager.this) {
                    if (scheduledCountdownTimer != null) {
                        scheduledCountdownTimer.cancel();
                    }
                    sendSurveyNow();
                }
            }
        }, delayMinutes * 60 * 1000L);
    }

    private void sendSurveyNow() {
        activeSurvey.setStatus(Survey.Status.ACTIVE);
        activeSurvey.setStartTime(LocalDateTime.now());
        reminderSent = false;

        // שליחת הסקר לכל המשתתפים
        if (botMessageSender != null) {
            for (SurveyParticipant participant : activeSurvey.getParticipants()) {
                botMessageSender.sendSurveyToParticipant(participant.getChatId(), activeSurvey);
            }
        }

        notifyListeners(l -> l.onSurveySent(activeSurvey));

        // טיימר countdown פעיל
        startActiveCountdown();

        // טיימר סגירה אחרי 5 דקות
        surveyTimer = new Timer("survey-close", true);
        surveyTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                closeSurvey();
            }
        }, SURVEY_DURATION_MINUTES * 60 * 1000L);
    }

    private void startActiveCountdown() {
        LocalDateTime endTime = activeSurvey.getStartTime().plusMinutes(SURVEY_DURATION_MINUTES);

        countdownTimer = new Timer("active-countdown", true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (activeSurvey == null || !activeSurvey.isActive()) {
                    countdownTimer.cancel();
                    return;
                }

                long secondsLeft = LocalDateTime.now().until(endTime, ChronoUnit.SECONDS);
                if (secondsLeft < 0) secondsLeft = 0;

                // תזכורת אחרי 3 דקות
                long secondsElapsed = activeSurvey.getStartTime().until(LocalDateTime.now(), ChronoUnit.SECONDS);
                if (secondsElapsed >= REMINDER_AFTER_MINUTES * 60 && !reminderSent) {
                    sendReminders();
                }

                final long remaining = secondsLeft;
                notifyListeners(l -> l.onCountdownUpdate(remaining, "active"));
            }
        }, 0, 1000);
    }

    /**
     * רושם תשובה של משתמש לשאלה בסקר הפעיל.
     * @return true אם התשובה נרשמה בהצלחה
     */
    public synchronized boolean recordAnswer(long chatId, int questionIndex, int optionIndex) {
        if (activeSurvey == null || !activeSurvey.isActive()) {
            return false;
        }

        SurveyParticipant participant = activeSurvey.getParticipant(chatId);
        if (participant == null) {
            return false; // לא משתתף בסקר
        }

        if (participant.hasAnswered(questionIndex)) {
            return false; // כבר ענה על שאלה זו
        }

        // רשום את התשובה
        participant.answerQuestion(questionIndex);
        SurveyQuestion question = activeSurvey.getQuestion(questionIndex);
        if (question != null) {
            question.recordVote(optionIndex);
        }

        notifyListeners(l -> l.onAnswerRecorded(activeSurvey, participant, questionIndex));

        // בדיקה אם כל המשתתפים השלימו
        if (activeSurvey.allCompleted()) {
            closeSurvey();
        }

        return true;
    }

    /**
     * סוגר את הסקר הפעיל.
     */
    public synchronized void closeSurvey() {
        if (activeSurvey == null || activeSurvey.isCompleted()) {
            return;
        }

        activeSurvey.setStatus(Survey.Status.COMPLETED);
        activeSurvey.setEndTime(LocalDateTime.now());

        // עצור את כל הטיימרים
        cancelAllTimers();

        // הודעה על סגירה למשתתפים
        if (botMessageSender != null) {
            for (SurveyParticipant participant : activeSurvey.getParticipants()) {
                botMessageSender.sendSurveyClosed(participant.getChatId());
            }
        }

        final Survey closedSurvey = activeSurvey;
        notifyListeners(l -> l.onSurveyClosed(closedSurvey));
    }

    private void sendReminders() {
        if (reminderSent || activeSurvey == null || !activeSurvey.isActive()) {
            return;
        }
        reminderSent = true;

        List<SurveyParticipant> needReminder = activeSurvey.getParticipantsNeedingReminder();

        if (botMessageSender != null) {
            for (SurveyParticipant p : needReminder) {
                p.setReminderSent(true);
                botMessageSender.sendReminder(p.getChatId(), activeSurvey);
            }
        }

        notifyListeners(l -> l.onRemindersSent(needReminder.size()));
    }

    private void cancelAllTimers() {
        if (surveyTimer != null) {
            surveyTimer.cancel();
            surveyTimer = null;
        }
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        if (scheduledSendTimer != null) {
            scheduledSendTimer.cancel();
            scheduledSendTimer = null;
        }
        if (scheduledCountdownTimer != null) {
            scheduledCountdownTimer.cancel();
            scheduledCountdownTimer = null;
        }
    }

    // --- Queries ---

    public Survey getActiveSurvey() {
        return activeSurvey;
    }

    public boolean hasActiveSurvey() {
        return activeSurvey != null &&
                (activeSurvey.isActive() || activeSurvey.isScheduled());
    }

    /**
     * מחזיר את הסקר האחרון (גם אם הסתיים).
     */
    public Survey getLastSurvey() {
        return activeSurvey;
    }

    /**
     * מאפס את הסקר לאחר צפייה בתוצאות — מאפשר יצירת סקר חדש.
     */
    public synchronized void clearCompletedSurvey() {
        if (activeSurvey != null && activeSurvey.isCompleted()) {
            activeSurvey = null;
        }
    }

    // --- Listeners ---

    public void addListener(SurveyListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SurveyListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(java.util.function.Consumer<SurveyListener> action) {
        for (SurveyListener listener : listeners) {
            try {
                action.accept(listener);
            } catch (Exception e) {
                System.err.println("Error in survey listener: " + e.getMessage());
            }
        }
    }
}
