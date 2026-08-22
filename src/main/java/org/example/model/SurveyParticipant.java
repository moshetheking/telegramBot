package org.example.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * מייצג משתתף בסקר ספציפי.
 * מצב המשתתף (שאלות שנענו, השלמה) שייך לסקר ולא לחבר הקהילה הגלובלי.
 */
public class SurveyParticipant {

    private final CommunityMember member;
    private final Set<Integer> answeredQuestions;
    private final int totalQuestions;
    private boolean reminderSent;

    public SurveyParticipant(CommunityMember member, int totalQuestions) {
        this.member = member;
        this.totalQuestions = totalQuestions;
        this.answeredQuestions = new HashSet<>();
        this.reminderSent = false;
    }

    public CommunityMember getMember() {
        return member;
    }

    public long getChatId() {
        return member.getChatId();
    }

    /**
     * רושם תשובה לשאלה. מחזיר true אם זו תשובה חדשה.
     */
    public synchronized boolean answerQuestion(int questionIndex) {
        return answeredQuestions.add(questionIndex);
    }

    /**
     * בודק אם המשתתף כבר ענה על שאלה מסוימת.
     */
    public boolean hasAnswered(int questionIndex) {
        return answeredQuestions.contains(questionIndex);
    }

    /**
     * מחזיר את מספר השאלות שנענו.
     */
    public int getAnsweredCount() {
        return answeredQuestions.size();
    }

    /**
     * מחזיר את מספר השאלות הכולל בסקר.
     */
    public int getTotalQuestions() {
        return totalQuestions;
    }

    /**
     * בודק אם המשתתף השלים את כל שאלות הסקר.
     */
    public boolean hasCompleted() {
        return answeredQuestions.size() >= totalQuestions;
    }

    /**
     * מחזיר מחרוזת התקדמות, למשל "2/3".
     */
    public String getProgressString() {
        return answeredQuestions.size() + "/" + totalQuestions;
    }

    /**
     * מחזיר מחרוזת מצב בעברית.
     */
    public String getStatusString() {
        if (hasCompleted()) {
            return "השלים";
        } else if (answeredQuestions.isEmpty()) {
            return "טרם ענה";
        } else {
            return "בתהליך";
        }
    }

    public Set<Integer> getAnsweredQuestions() {
        return Collections.unmodifiableSet(answeredQuestions);
    }

    public boolean isReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
}
