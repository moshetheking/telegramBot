package org.example.ui;

import org.example.model.SurveyQuestion;
import org.example.service.ChatGPTService;
import org.example.service.CommunityManager;
import org.example.service.SurveyManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.example.ui.UIConstants.*;

/**
 * פאנל ליצירת סקר חדש.
 * תומך ביצירה ידנית ויצירה אוטומטית באמצעות ChatGPT.
 */
public class SurveyCreationPanel extends JPanel {

    private final SurveyManager surveyManager;
    private final CommunityManager communityManager;
    private final ChatGPTService chatGPTService;
    private final Runnable onSurveyStarted;

    // מצב יצירה
    private JRadioButton manualRadio;
    private JRadioButton chatGPTRadio;
    private JPanel modeContentPanel;
    private CardLayout modeCardLayout;

    // מצב ידני
    private JSpinner questionCountSpinner;
    private List<JTextArea> questionFields = new ArrayList<>();
    private List<List<JTextField>> optionFields = new ArrayList<>();
    private JPanel manualQuestionsPanel;

    // מצב ChatGPT
    private JTextField topicField;
    private JSpinner gptQuestionCountSpinner;
    private JButton generateButton;
    private JPanel generatedPreviewPanel;
    private List<SurveyQuestion> generatedQuestions;

    // תזמון
    private JRadioButton sendNowRadio;
    private JRadioButton sendLaterRadio;
    private JSpinner delaySpinner;

    // כפתור שליחה
    private JButton startButton;
    private JLabel statusLabel;

    public SurveyCreationPanel(SurveyManager surveyManager, CommunityManager communityManager,
                                ChatGPTService chatGPTService, Runnable onSurveyStarted) {
        this.surveyManager = surveyManager;
        this.communityManager = communityManager;
        this.chatGPTService = chatGPTService;
        this.onSurveyStarted = onSurveyStarted;

        setLayout(new BorderLayout(0, PADDING));
        setBackground(BG_PANEL);
        setBorder(createPaddedBorder(PADDING));

        // --- Header ---
        JLabel header = createLabel("📝  יצירת סקר חדש", FONT_TITLE, TEXT_PRIMARY);
        header.setHorizontalAlignment(SwingConstants.RIGHT);
        add(header, BorderLayout.NORTH);

        // --- Main Content (scrollable) ---
        JPanel contentPanel = createPanel(BG_PANEL);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        contentPanel.add(createModeSelectionPanel());
        contentPanel.add(Box.createVerticalStrut(PADDING));
        contentPanel.add(createModeContentPanel());
        contentPanel.add(Box.createVerticalStrut(PADDING));
        contentPanel.add(createTimingPanel());
        contentPanel.add(Box.createVerticalStrut(PADDING));
        contentPanel.add(createActionPanel());

        JScrollPane scrollPane = createStyledScrollPane(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createModeSelectionPanel() {
        JPanel panel = createPanel(BG_CARD);
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT, PADDING, PADDING_SMALL));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                createPaddedBorder(PADDING_SMALL)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel label = createLabel("אופן יצירה:", FONT_BODY_BOLD, TEXT_PRIMARY);

        manualRadio = new JRadioButton("ידני");
        chatGPTRadio = new JRadioButton("ChatGPT");
        styleRadioButton(manualRadio);
        styleRadioButton(chatGPTRadio);
        manualRadio.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(manualRadio);
        group.add(chatGPTRadio);

        manualRadio.addActionListener(e -> modeCardLayout.show(modeContentPanel, "manual"));
        chatGPTRadio.addActionListener(e -> modeCardLayout.show(modeContentPanel, "chatgpt"));

        panel.add(chatGPTRadio);
        panel.add(manualRadio);
        panel.add(label);

        return panel;
    }

    private JPanel createModeContentPanel() {
        modeCardLayout = new CardLayout();
        modeContentPanel = new JPanel(modeCardLayout);
        modeContentPanel.setBackground(BG_PANEL);

        modeContentPanel.add(createManualPanel(), "manual");
        modeContentPanel.add(createChatGPTPanel(), "chatgpt");

        return modeContentPanel;
    }

    // --- Manual Mode ---

    private JPanel createManualPanel() {
        JPanel panel = createPanel(BG_PANEL);
        panel.setLayout(new BorderLayout(0, PADDING));

        // בחירת מספר שאלות
        JPanel countPanel = createPanel(BG_CARD);
        countPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, PADDING, PADDING_SMALL));
        countPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                createPaddedBorder(PADDING_SMALL)
        ));

        JLabel countLabel = createLabel("מספר שאלות:", FONT_BODY_BOLD, TEXT_PRIMARY);
        questionCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));
        styleSpinner(questionCountSpinner);
        questionCountSpinner.addChangeListener(e -> rebuildManualQuestions());

        countPanel.add(questionCountSpinner);
        countPanel.add(countLabel);
        panel.add(countPanel, BorderLayout.NORTH);

        // שאלות
        manualQuestionsPanel = createPanel(BG_PANEL);
        manualQuestionsPanel.setLayout(new BoxLayout(manualQuestionsPanel, BoxLayout.Y_AXIS));
        panel.add(manualQuestionsPanel, BorderLayout.CENTER);

        rebuildManualQuestions();

        return panel;
    }

    private void rebuildManualQuestions() {
        int count = (int) questionCountSpinner.getValue();
        manualQuestionsPanel.removeAll();
        questionFields.clear();
        optionFields.clear();

        for (int i = 0; i < count; i++) {
            manualQuestionsPanel.add(createQuestionInputPanel(i));
            if (i < count - 1) {
                manualQuestionsPanel.add(Box.createVerticalStrut(PADDING_SMALL));
            }
        }

        manualQuestionsPanel.revalidate();
        manualQuestionsPanel.repaint();
    }

    private JPanel createQuestionInputPanel(int questionIndex) {
        JPanel panel = createPanel(BG_CARD);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                createPaddedBorder(PADDING)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        // כותרת שאלה
        JLabel qLabel = createLabel("שאלה " + (questionIndex + 1) + ":", FONT_BODY_BOLD, ACCENT_BLUE);
        qLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        panel.add(qLabel);
        panel.add(Box.createVerticalStrut(PADDING_SMALL));

        // שדה השאלה
        JTextArea questionArea = createStyledTextArea(2, 30);
        questionFields.add(questionArea);
        JScrollPane qScroll = new JScrollPane(questionArea);
        qScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        qScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        panel.add(qScroll);
        panel.add(Box.createVerticalStrut(PADDING_SMALL));

        // תשובות
        JLabel aLabel = createLabel("אפשרויות תשובה (2-4):", FONT_SMALL, TEXT_SECONDARY);
        aLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        panel.add(aLabel);
        panel.add(Box.createVerticalStrut(4));

        List<JTextField> opts = new ArrayList<>();
        for (int j = 0; j < 4; j++) {
            JTextField optField = createStyledTextField("");
            optField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            String optionLabel = "אפשרות " + (j + 1);
            if (j >= 2) optionLabel += " (אופציונלי)";

            JPanel optRow = createPanel(BG_CARD);
            optRow.setLayout(new BorderLayout(PADDING_SMALL, 0));
            optRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

            JLabel optLabel = createLabel(optionLabel, FONT_SMALL, TEXT_MUTED);
            optLabel.setPreferredSize(new Dimension(140, 36));

            optRow.add(optField, BorderLayout.CENTER);
            optRow.add(optLabel, BorderLayout.EAST);

            opts.add(optField);
            panel.add(optRow);
            panel.add(Box.createVerticalStrut(4));
        }
        optionFields.add(opts);

        return panel;
    }

    // --- ChatGPT Mode ---

    private JPanel createChatGPTPanel() {
        JPanel panel = createPanel(BG_PANEL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // נושא
        JPanel topicPanel = new JPanel(new BorderLayout(PADDING, PADDING_SMALL)) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        topicPanel.setBackground(BG_CARD);
        topicPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                createPaddedBorder(PADDING)
        ));

        JLabel topicLabel = createLabel("🤖 נושא הסקר:", FONT_BODY_BOLD, TEXT_PRIMARY);
        topicField = createStyledTextField("");
        topicField.setToolTipText("לדוגמה: העדפות טכנולוגיות בקרב מהנדסי תוכנה");

        JPanel topicRow = createPanel(BG_CARD);
        topicRow.setLayout(new BorderLayout(PADDING_SMALL, 0));

        JLabel numLabel = createLabel("מספר שאלות:", FONT_BODY, TEXT_SECONDARY);
        gptQuestionCountSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 3, 1));
        styleSpinner(gptQuestionCountSpinner);

        JPanel numPanel = createPanel(BG_CARD);
        numPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, PADDING_SMALL, 0));
        numPanel.add(gptQuestionCountSpinner);
        numPanel.add(numLabel);

        topicPanel.add(topicLabel, BorderLayout.NORTH);
        topicPanel.add(topicField, BorderLayout.CENTER);
        topicPanel.add(numPanel, BorderLayout.SOUTH);

        panel.add(topicPanel);
        panel.add(Box.createVerticalStrut(PADDING_SMALL));

        // כפתור יצירה
        generateButton = createStyledButton("✨  ייצר שאלות", ACCENT_PURPLE);
        generateButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        generateButton.setMaximumSize(new Dimension(200, 40));
        generateButton.addActionListener(e -> generateWithChatGPT());

        JPanel btnPanel = createPanel(BG_PANEL);
        btnPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnPanel.add(generateButton);
        panel.add(btnPanel);
        panel.add(Box.createVerticalStrut(PADDING_SMALL));

        // תצוגה מקדימה
        generatedPreviewPanel = createPanel(BG_PANEL);
        generatedPreviewPanel.setLayout(new BoxLayout(generatedPreviewPanel, BoxLayout.Y_AXIS));
        panel.add(generatedPreviewPanel);

        if (!chatGPTService.isConfigured()) {
            JLabel warningLabel = createLabel("⚠️ מפתח OpenAI API לא הוגדר — עדכן ב-config.properties",
                    FONT_SMALL, ACCENT_ORANGE);
            warningLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
            panel.add(warningLabel);
        }

        return panel;
    }

    private void generateWithChatGPT() {
        String topic = topicField.getText().trim();
        if (topic.isEmpty()) {
            showStatus("⚠️ יש להזין נושא לסקר", ACCENT_ORANGE);
            return;
        }

        if (!chatGPTService.isConfigured()) {
            showStatus("⚠️ מפתח OpenAI API לא הוגדר", ACCENT_RED);
            return;
        }

        generateButton.setEnabled(false);
        generateButton.setText("⏳  מייצר...");

        SwingWorker<List<SurveyQuestion>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<SurveyQuestion> doInBackground() throws Exception {
                int numQ = (int) gptQuestionCountSpinner.getValue();
                return chatGPTService.generateSurvey(topic, numQ);
            }

            @Override
            protected void done() {
                generateButton.setEnabled(true);
                generateButton.setText("✨  ייצר שאלות");
                try {
                    generatedQuestions = get();
                    showGeneratedPreview();
                    showStatus("✅ שאלות נוצרו בהצלחה!", ACCENT_GREEN);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String msg = (ex.getCause() != null && ex.getCause().getMessage() != null) 
                            ? ex.getCause().getMessage() 
                            : ex.getMessage();
                    showStatus("❌ שגיאה: " + msg, ACCENT_RED);
                }
            }
        };
        worker.execute();
    }

    private void showGeneratedPreview() {
        generatedPreviewPanel.removeAll();

        JLabel previewLabel = createLabel("📋  תצוגה מקדימה:", FONT_BODY_BOLD, ACCENT_GREEN);
        previewLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        generatedPreviewPanel.add(previewLabel);
        generatedPreviewPanel.add(Box.createVerticalStrut(PADDING_SMALL));

        for (SurveyQuestion q : generatedQuestions) {
            JPanel qPanel = createPanel(BG_CARD);
            qPanel.setLayout(new BoxLayout(qPanel, BoxLayout.Y_AXIS));
            qPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR),
                    createPaddedBorder(PADDING)
            ));

            JTextArea qTextArea = new JTextArea("שאלה " + (q.getQuestionIndex() + 1) + ": " + q.getQuestionText());
            qTextArea.setFont(FONT_BODY_BOLD);
            qTextArea.setForeground(TEXT_PRIMARY);
            qTextArea.setBackground(BG_CARD);
            qTextArea.setLineWrap(true);
            qTextArea.setWrapStyleWord(true);
            qTextArea.setEditable(false);
            qTextArea.setFocusable(false);
            qTextArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            
            qPanel.add(qTextArea);
            qPanel.add(Box.createVerticalStrut(PADDING_SMALL));

            for (int i = 0; i < q.getOptions().size(); i++) {
                JTextArea optArea = new JTextArea("   • " + q.getOptions().get(i));
                optArea.setFont(FONT_BODY);
                optArea.setForeground(TEXT_SECONDARY);
                optArea.setBackground(BG_CARD);
                optArea.setLineWrap(true);
                optArea.setWrapStyleWord(true);
                optArea.setEditable(false);
                optArea.setFocusable(false);
                optArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                qPanel.add(optArea);
            }

            generatedPreviewPanel.add(qPanel);
            generatedPreviewPanel.add(Box.createVerticalStrut(PADDING_SMALL));
        }

        generatedPreviewPanel.revalidate();
        generatedPreviewPanel.repaint();
    }

    // --- Timing ---

    private JPanel createTimingPanel() {
        JPanel panel = createPanel(BG_CARD);
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT, PADDING, PADDING_SMALL));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                createPaddedBorder(PADDING_SMALL)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel label = createLabel("⏰ מועד שליחה:", FONT_BODY_BOLD, TEXT_PRIMARY);

        sendNowRadio = new JRadioButton("מיידי");
        sendLaterRadio = new JRadioButton("בעיכוב");
        styleRadioButton(sendNowRadio);
        styleRadioButton(sendLaterRadio);
        sendNowRadio.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(sendNowRadio);
        group.add(sendLaterRadio);

        JLabel minLabel = createLabel("דקות:", FONT_BODY, TEXT_SECONDARY);
        delaySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 60, 1));
        styleSpinner(delaySpinner);
        delaySpinner.setEnabled(false);

        sendLaterRadio.addActionListener(e -> delaySpinner.setEnabled(true));
        sendNowRadio.addActionListener(e -> delaySpinner.setEnabled(false));

        panel.add(delaySpinner);
        panel.add(minLabel);
        panel.add(sendLaterRadio);
        panel.add(sendNowRadio);
        panel.add(label);

        return panel;
    }

    // --- Action ---

    private JPanel createActionPanel() {
        JPanel panel = createPanel(BG_PANEL);
        panel.setLayout(new BorderLayout(PADDING, PADDING_SMALL));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        startButton = createStyledButton("🚀  שלח סקר", ACCENT_BLUE);
        startButton.setPreferredSize(new Dimension(200, 45));
        startButton.addActionListener(e -> startSurvey());

        statusLabel = createLabel("", FONT_BODY, TEXT_SECONDARY);
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel btnPanel = createPanel(BG_PANEL);
        btnPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        btnPanel.add(startButton);

        panel.add(btnPanel, BorderLayout.WEST);
        panel.add(statusLabel, BorderLayout.CENTER);

        return panel;
    }

    private void startSurvey() {
        try {
            // ולידציה
            if (communityManager.getMemberCount() < 3) {
                showStatus("⚠️ נדרשים לפחות 3 חברים בקהילה כדי להתחיל סקר.", ACCENT_ORANGE);
                return;
            }

            if (surveyManager.hasActiveSurvey()) {
                showStatus("⚠️ קיים סקר פעיל. יש להמתין לסיומו.", ACCENT_ORANGE);
                return;
            }

            // בניית שאלות
            List<SurveyQuestion> questions;
            if (chatGPTRadio.isSelected() && generatedQuestions != null) {
                questions = generatedQuestions;
            } else if (manualRadio.isSelected()) {
                questions = buildManualQuestions();
            } else {
                showStatus("⚠️ יש ליצור שאלות לפני שליחת הסקר.", ACCENT_ORANGE);
                return;
            }

            if (questions.isEmpty()) {
                showStatus("⚠️ יש למלא לפחות שאלה אחת עם 2 אפשרויות תשובה.", ACCENT_ORANGE);
                return;
            }

            // יצירת הסקר
            surveyManager.createSurvey(questions);

            // תזמון
            int delay = 0;
            if (sendLaterRadio.isSelected()) {
                delay = (int) delaySpinner.getValue();
            }

            surveyManager.startSurvey(delay);

            if (delay > 0) {
                showStatus("⏳ הסקר מתוזמן לשליחה בעוד " + delay + " דקות.", ACCENT_BLUE);
            } else {
                showStatus("✅ הסקר נשלח למשתתפים!", ACCENT_GREEN);
            }

            // מעבר לפאנל סקר פעיל
            if (onSurveyStarted != null) {
                onSurveyStarted.run();
            }

        } catch (IllegalStateException ex) {
            showStatus("⚠️ " + ex.getMessage(), ACCENT_ORANGE);
        } catch (Exception ex) {
            showStatus("❌ שגיאה: " + ex.getMessage(), ACCENT_RED);
        }
    }

    private List<SurveyQuestion> buildManualQuestions() {
        List<SurveyQuestion> questions = new ArrayList<>();

        for (int i = 0; i < questionFields.size(); i++) {
            String questionText = questionFields.get(i).getText().trim();
            if (questionText.isEmpty()) continue;

            List<String> options = new ArrayList<>();
            for (JTextField optField : optionFields.get(i)) {
                String opt = optField.getText().trim();
                if (!opt.isEmpty()) {
                    options.add(opt);
                }
            }

            if (options.size() >= 2) {
                questions.add(new SurveyQuestion(i, questionText, options));
            }
        }

        // re-index
        List<SurveyQuestion> reindexed = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            SurveyQuestion q = questions.get(i);
            reindexed.add(new SurveyQuestion(i, q.getQuestionText(), q.getOptions()));
        }

        return reindexed;
    }

    private void showStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        });
    }

    // --- Styling helpers ---

    private void styleRadioButton(JRadioButton radio) {
        radio.setFont(FONT_BODY);
        // FlatLaf handles colors natively
        radio.setFocusPainted(false);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(FONT_BODY);
        spinner.setPreferredSize(new Dimension(60, 30));
        // FlatLaf handles editor colors natively
    }
}
