package org.example.ui;

import org.example.model.Survey;
import org.example.model.SurveyParticipant;
import org.example.service.SurveyManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import static org.example.ui.UIConstants.*;

/**
 * פאנל מעקב אחר סקר פעיל.
 * מציג countdown, סטטיסטיקות ומצב משתתפים בזמן אמת.
 */
public class ActiveSurveyPanel extends JPanel implements SurveyManager.SurveyListener {

    private final SurveyManager surveyManager;
    private final Runnable onSurveyClosed;

    // Countdown & Status
    private JLabel countdownLabel;
    private JLabel countdownTypeLabel;
    private JLabel statusBanner;

    // Statistics
    private JLabel statParticipants;
    private JLabel statCompleted;
    private JLabel statPending;
    private JLabel statReminder;

    // Participants table
    private DefaultTableModel tableModel;
    private JTable participantsTable;

    public ActiveSurveyPanel(SurveyManager surveyManager, Runnable onSurveyClosed) {
        this.surveyManager = surveyManager;
        this.onSurveyClosed = onSurveyClosed;
        this.surveyManager.addListener(this);

        setLayout(new BorderLayout(0, PADDING));
        setBackground(BG_PANEL);
        setBorder(createPaddedBorder(PADDING));

        // --- Header ---
        JLabel header = createLabel("📊  סקר פעיל", FONT_TITLE, TEXT_PRIMARY);
        header.setHorizontalAlignment(SwingConstants.RIGHT);
        add(header, BorderLayout.NORTH);

        // --- Center Content ---
        JPanel centerPanel = createPanel(BG_PANEL);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        centerPanel.add(createCountdownPanel());
        centerPanel.add(Box.createVerticalStrut(PADDING));
        centerPanel.add(createStatsPanel());
        centerPanel.add(Box.createVerticalStrut(PADDING));
        centerPanel.add(createParticipantsTablePanel());

        JScrollPane scrollPane = createStyledScrollPane(centerPanel);
        add(scrollPane, BorderLayout.CENTER);

        // --- Status Banner (for sent/closed messages) ---
        statusBanner = createLabel("", FONT_BODY_BOLD, ACCENT_GREEN);
        statusBanner.setHorizontalAlignment(SwingConstants.CENTER);
        statusBanner.setOpaque(true);
        statusBanner.setBackground(BG_CARD);
        statusBanner.setBorder(createPaddedBorder(PADDING_SMALL));
        statusBanner.setVisible(false);
        add(statusBanner, BorderLayout.SOUTH);
    }

    private JPanel createCountdownPanel() {
        JPanel panel = createPanel(BG_CARD);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_BLUE, 2),
                createPaddedBorder(PADDING)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        countdownTypeLabel = createLabel("⏳ זמן שנותר", FONT_BODY, TEXT_SECONDARY);
        countdownTypeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        countdownLabel = createLabel("05:00", FONT_COUNTDOWN, ACCENT_BLUE);
        countdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(countdownTypeLabel);
        panel.add(Box.createVerticalStrut(PADDING_SMALL));
        panel.add(countdownLabel);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = createPanel(BG_CARD);
        panel.setLayout(new GridLayout(1, 4, PADDING, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                createPaddedBorder(PADDING)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        statParticipants = createStatCard("משתתפים", "0", ACCENT_BLUE);
        statCompleted = createStatCard("השלימו", "0", ACCENT_GREEN);
        statPending = createStatCard("טרם השלימו", "0", ACCENT_ORANGE);
        statReminder = createStatCard("תזכורות", "—", TEXT_MUTED);

        panel.add(statReminder);
        panel.add(statPending);
        panel.add(statCompleted);
        panel.add(statParticipants);

        return panel;
    }

    private JLabel createStatCard(String title, String value, Color color) {
        JLabel label = new JLabel("<html><div style='text-align:center'>"
                + "<span style='font-size:10px;color:#" + colorToHex(TEXT_SECONDARY) + "'>" + title + "</span><br>"
                + "<span style='font-size:18px;color:#" + colorToHex(color) + "'>" + value + "</span>"
                + "</div></html>");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(FONT_BODY);
        label.setForeground(TEXT_PRIMARY);
        return label;
    }

    private void updateStatCard(JLabel label, String title, String value, Color color) {
        label.setText("<html><div style='text-align:center'>"
                + "<span style='font-size:10px;color:#" + colorToHex(TEXT_SECONDARY) + "'>" + title + "</span><br>"
                + "<span style='font-size:18px;color:#" + colorToHex(color) + "'>" + value + "</span>"
                + "</div></html>");
    }

    private JPanel createParticipantsTablePanel() {
        JPanel panel = createPanel(BG_PANEL);
        panel.setLayout(new BorderLayout(0, PADDING_SMALL));

        JLabel tableHeader = createLabel("👥  מצב משתתפים:", FONT_SUBTITLE, TEXT_PRIMARY);
        tableHeader.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(tableHeader, BorderLayout.NORTH);

        String[] columns = {"מצב", "התקדמות", "שם"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        participantsTable = new JTable(tableModel);
        styleTable(participantsTable);

        // Custom renderer for status column with colors
        participantsTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? BG_PANEL : TABLE_ROW_ALT);
                }
                String status = value != null ? value.toString() : "";
                if (status.equals("השלים")) {
                    c.setForeground(STATUS_COMPLETED);
                } else if (status.equals("בתהליך")) {
                    c.setForeground(STATUS_IN_PROGRESS);
                } else {
                    c.setForeground(STATUS_NOT_STARTED);
                }
                ((JLabel) c).setFont(FONT_BODY_BOLD);
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        });

        participantsTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        participantsTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        participantsTable.getColumnModel().getColumn(2).setPreferredWidth(180);

        JScrollPane scrollPane = createStyledScrollPane(participantsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * מרענן את כל הנתונים בפאנל.
     */
    public void refreshAll() {
        Survey survey = surveyManager.getActiveSurvey();
        if (survey == null) return;

        SwingUtilities.invokeLater(() -> {
            // סטטיסטיקות
            updateStatCard(statParticipants, "משתתפים",
                    String.valueOf(survey.getParticipantCount()), ACCENT_BLUE);
            updateStatCard(statCompleted, "השלימו",
                    String.valueOf(survey.getCompletedCount()), ACCENT_GREEN);
            updateStatCard(statPending, "טרם השלימו",
                    String.valueOf(survey.getNotCompletedCount()), ACCENT_ORANGE);

            // טבלה
            tableModel.setRowCount(0);
            for (SurveyParticipant p : survey.getParticipantsSorted()) {
                tableModel.addRow(new Object[]{
                        p.getStatusString(),
                        p.getProgressString(),
                        p.getMember().getDisplayName()
                });
            }
        });
    }

    // --- SurveyListener ---

    @Override
    public void onSurveyCreated(Survey survey) {
        refreshAll();
    }

    @Override
    public void onSurveySent(Survey survey) {
        SwingUtilities.invokeLater(() -> {
            statusBanner.setText("✅ הסקר נשלח בהצלחה למשתתפים!");
            statusBanner.setForeground(ACCENT_GREEN);
            statusBanner.setVisible(true);
            countdownTypeLabel.setText("⏳ זמן שנותר לסיום הסקר");
        });
        refreshAll();
    }

    @Override
    public void onAnswerRecorded(Survey survey, SurveyParticipant participant, int questionIndex) {
        refreshAll();
    }

    @Override
    public void onSurveyClosed(Survey survey) {
        SwingUtilities.invokeLater(() -> {
            countdownLabel.setText("00:00");
            countdownLabel.setForeground(TEXT_MUTED);
            statusBanner.setText("🔒 הסקר הסתיים! לחץ לצפייה בתוצאות.");
            statusBanner.setForeground(ACCENT_BLUE);
            statusBanner.setVisible(true);
            statusBanner.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // לחיצה על הבאנר לצפייה בתוצאות
            for (var ml : statusBanner.getMouseListeners()) {
                statusBanner.removeMouseListener(ml);
            }
            statusBanner.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (onSurveyClosed != null) {
                        onSurveyClosed.run();
                    }
                }
            });
        });
        refreshAll();
    }

    @Override
    public void onCountdownUpdate(long secondsRemaining, String type) {
        SwingUtilities.invokeLater(() -> {
            String timeStr = formatTime(secondsRemaining);
            countdownLabel.setText(timeStr);

            // צבע לפי זמן שנותר
            if (secondsRemaining <= 60) {
                countdownLabel.setForeground(ACCENT_RED);
            } else if (secondsRemaining <= 120) {
                countdownLabel.setForeground(ACCENT_ORANGE);
            } else {
                countdownLabel.setForeground(ACCENT_BLUE);
            }
        });
    }

    @Override
    public void onScheduledCountdownUpdate(long secondsRemaining) {
        SwingUtilities.invokeLater(() -> {
            countdownTypeLabel.setText("⏳ זמן שנותר עד לשליחת הסקר");
            String timeStr = formatTime(secondsRemaining);
            countdownLabel.setText(timeStr);
            countdownLabel.setForeground(ACCENT_PURPLE);

            statusBanner.setText("📅 הסקר מתוזמן — ממתין לשליחה...");
            statusBanner.setForeground(ACCENT_PURPLE);
            statusBanner.setVisible(true);
        });
    }

    @Override
    public void onRemindersSent(int count) {
        SwingUtilities.invokeLater(() -> {
            updateStatCard(statReminder, "תזכורות", String.valueOf(count), ACCENT_ORANGE);
        });
    }

    // --- Utility ---

    private String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static String colorToHex(Color c) {
        return String.format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
