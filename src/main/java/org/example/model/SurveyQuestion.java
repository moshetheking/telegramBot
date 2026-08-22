package org.example.model;

import java.util.*;

/**
 * מייצג שאלה בודדת בסקר.
 * כוללת את נוסח השאלה, אפשרויות התשובה וספירת ההצבעות.
 */
public class SurveyQuestion {

    private final int questionIndex;
    private final String questionText;
    private final List<String> options;
    private final Map<Integer, Integer> voteCounts; // optionIndex -> count

    public SurveyQuestion(int questionIndex, String questionText, List<String> options) {
        this.questionIndex = questionIndex;
        this.questionText = questionText;
        this.options = new ArrayList<>(options);
        this.voteCounts = new HashMap<>();
        for (int i = 0; i < options.size(); i++) {
            voteCounts.put(i, 0);
        }
    }

    public int getQuestionIndex() {
        return questionIndex;
    }

    public String getQuestionText() {
        return questionText;
    }

    public List<String> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public int getOptionCount() {
        return options.size();
    }

    /**
     * רושם הצבעה לאפשרות מסוימת.
     */
    public synchronized void recordVote(int optionIndex) {
        if (optionIndex >= 0 && optionIndex < options.size()) {
            voteCounts.put(optionIndex, voteCounts.get(optionIndex) + 1);
        }
    }

    /**
     * מחזיר את מספר ההצבעות לאפשרות מסוימת.
     */
    public int getVoteCount(int optionIndex) {
        return voteCounts.getOrDefault(optionIndex, 0);
    }

    /**
     * מחזיר את סך כל ההצבעות על השאלה.
     */
    public int getTotalVotes() {
        return voteCounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * מחזיר את אחוז ההצבעות לאפשרות מסוימת.
     */
    public double getVotePercentage(int optionIndex) {
        int total = getTotalVotes();
        if (total == 0) return 0.0;
        return (getVoteCount(optionIndex) * 100.0) / total;
    }

    /**
     * מחזיר את האפשרויות ממוינות לפי שכיחות (מהגבוה לנמוך).
     * כל רשומה: [optionIndex, optionText, voteCount, percentage]
     */
    public List<int[]> getOptionsSortedByVotes() {
        List<int[]> sorted = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            sorted.add(new int[]{i, voteCounts.getOrDefault(i, 0)});
        }
        sorted.sort((a, b) -> Integer.compare(b[1], a[1]));
        return sorted;
    }
}
