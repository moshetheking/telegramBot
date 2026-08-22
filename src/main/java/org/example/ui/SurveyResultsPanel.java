package org.example.ui;

import org.example.model.Survey;
import org.example.model.SurveyQuestion;
import org.example.service.SurveyManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static org.example.ui.UIConstants.*;

/**
 * פאנל תצוגת תוצאות סקר שהסתיים.
 * מציג לכל שאלה את אפשרויות התשובה ממוינות לפי שכיחות עם progress bars.
 */
public class SurveyResultsPanel extends JPanel {

    private final SurveyManager surveyManager;
    private final Runnable onNewSurvey;
    private JPanel resultsContentPanel;

    public SurveyResultsPanel(SurveyManager surveyManager, Runnable onNewSurvey) {
        this.surveyManager = surveyManager;
        this.onNewSurvey = onNewSurvey;

        setLayout(new BorderLayout(0, PADDING));
        setBackground(BG_PANEL);
        setBorder(createPaddedBorder(PADDING));

        // --- Header ---
        JPanel headerPanel = createPanel(BG_PANEL);
        headerPanel.setLayout(new BorderLayout());

        JLabel header = createLabel("📊  תוצאות הסקר", FONT_TITLE, TEXT_PRIMARY);
        header.setHorizontalAlignment(SwingConstants.RIGHT);

        JButton newSurveyButton = createStyledButton("📝  סקר חדש", ACCENT_BLUE);
        newSurveyButton.setPreferredSize(new Dimension(150, 38));
        newSurveyButton.addActionListener(e -> {
            surveyManager.clearCompletedSurvey();
            if (onNewSurvey != null) {
                onNewSurvey.run();
            }
        });

        headerPanel.add(header, BorderLayout.EAST);
        headerPanel.add(newSurveyButton, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // --- Content ---
        resultsContentPanel = createPanel(BG_PANEL);
        resultsContentPanel.setLayout(new BoxLayout(resultsContentPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = createStyledScrollPane(resultsContentPanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * מציג את תוצאות הסקר.
     */
    public void showResults(Survey survey) {
        resultsContentPanel.removeAll();

        if (survey == null) {
            JLabel noData = createLabel("אין נתונים להצגה.", FONT_BODY, TEXT_MUTED);
            noData.setAlignmentX(Component.CENTER_ALIGNMENT);
            resultsContentPanel.add(noData);
            return;
        }

        // --- Summary ---
        JPanel summaryPanel = createPanel(BG_CARD);
        summaryPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, PADDING * 2, PADDING_SMALL));
        summaryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_GREEN, 2),
                createPaddedBorder(PADDING)
        ));
        summaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        summaryPanel.add(createSummaryLabel("שאלות: " + survey.getQuestionCount(), ACCENT_BLUE));
        summaryPanel.add(createSummaryLabel("משתתפים: " + survey.getParticipantCount(), ACCENT_BLUE));
        summaryPanel.add(createSummaryLabel("השלימו: " + survey.getCompletedCount(), ACCENT_GREEN));
        summaryPanel.add(createSummaryLabel("לא השלימו: " + survey.getNotCompletedCount(), ACCENT_ORANGE));

        resultsContentPanel.add(summaryPanel);
        resultsContentPanel.add(Box.createVerticalStrut(PADDING));

        // --- Questions ---
        for (SurveyQuestion question : survey.getQuestions()) {
            resultsContentPanel.add(createQuestionResultPanel(question));
            resultsContentPanel.add(Box.createVerticalStrut(PADDING));
        }

        resultsContentPanel.revalidate();
        resultsContentPanel.repaint();
    }

    private JLabel createSummaryLabel(String text, Color color) {
        JLabel label = createLabel(text, FONT_BODY_BOLD, color);
        return label;
    }

    private JPanel createQuestionResultPanel(SurveyQuestion question) {
        JPanel panel = createPanel(BG_CARD);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                createPaddedBorder(PADDING)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 400));

        // כותרת שאלה
        JLabel qLabel = createLabel(
                "❓  שאלה " + (question.getQuestionIndex() + 1) + ": " + question.getQuestionText(),
                FONT_BODY_BOLD, TEXT_PRIMARY
        );
        qLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        panel.add(qLabel);

        JLabel totalLabel = createLabel(
                "סה\"כ הצבעות: " + question.getTotalVotes(),
                FONT_SMALL, TEXT_SECONDARY
        );
        totalLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        panel.add(totalLabel);
        panel.add(Box.createVerticalStrut(PADDING));

        // אפשרויות ממוינות לפי שכיחות
        List<int[]> sortedOptions = question.getOptionsSortedByVotes();
        boolean isFirst = true;

        for (int[] optionData : sortedOptions) {
            int optIndex = optionData[0];
            int votes = optionData[1];
            String optText = question.getOptions().get(optIndex);
            double percentage = question.getVotePercentage(optIndex);

            panel.add(createOptionBar(optText, votes, percentage, isFirst));
            panel.add(Box.createVerticalStrut(PADDING_SMALL));
            isFirst = false;
        }

        return panel;
    }

    private JPanel createOptionBar(String text, int votes, double percentage, boolean isTop) {
        JPanel panel = createPanel(BG_CARD);
        panel.setLayout(new BorderLayout(PADDING, 4));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        // תווית טקסט
        Color textColor = isTop ? ACCENT_GREEN : TEXT_PRIMARY;
        String prefix = isTop ? "🏆 " : "    ";
        JLabel textLabel = createLabel(prefix + text, FONT_BODY, textColor);
        textLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        // אחוזים ומספר הצבעות
        JLabel percentLabel = createLabel(
                String.format("%.0f%% (%d)", percentage, votes),
                FONT_BODY_BOLD, isTop ? ACCENT_GREEN : TEXT_SECONDARY
        );
        percentLabel.setHorizontalAlignment(SwingConstants.LEFT);
        percentLabel.setPreferredSize(new Dimension(100, 30));

        // Progress bar
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue((int) percentage);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 8));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        progressBar.setBackground(BG_INPUT);
        progressBar.setForeground(isTop ? ACCENT_GREEN : ACCENT_BLUE);
        progressBar.setBorderPainted(false);

        JPanel barWrapper = createPanel(BG_CARD);
        barWrapper.setLayout(new BorderLayout());
        barWrapper.add(textLabel, BorderLayout.NORTH);
        barWrapper.add(progressBar, BorderLayout.SOUTH);

        panel.add(barWrapper, BorderLayout.CENTER);
        panel.add(percentLabel, BorderLayout.WEST);

        return panel;
    }
}
