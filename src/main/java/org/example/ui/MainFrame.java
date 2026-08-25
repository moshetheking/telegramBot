package org.example.ui;

import org.example.model.Survey;
import org.example.service.ChatGPTService;
import org.example.service.CommunityManager;
import org.example.service.SurveyManager;

import javax.swing.*;
import java.awt.*;

import static org.example.ui.UIConstants.*;

/**
 * חלון ראשי של ממשק ניהול הסקרים.
 * מחולק לשני אזורים:
 * - שמאל: פאנל קהילה (תמיד מוצג)
 * - ימין: פאנל דינמי (יצירת סקר / סקר פעיל / תוצאות)
 */
public class MainFrame extends JFrame {

    private final CommunityManager communityManager;
    private final SurveyManager surveyManager;
    private final ChatGPTService chatGPTService;

    private CommunityPanel communityPanel;
    private SurveyCreationPanel surveyCreationPanel;
    private ActiveSurveyPanel activeSurveyPanel;
    private SurveyResultsPanel surveyResultsPanel;

    private JPanel rightPanel;
    private CardLayout rightCardLayout;

    private static final String CARD_CREATE = "create";
    private static final String CARD_ACTIVE = "active";
    private static final String CARD_RESULTS = "results";

    public MainFrame(CommunityManager communityManager, SurveyManager surveyManager,
                      ChatGPTService chatGPTService) {
        this.communityManager = communityManager;
        this.surveyManager = surveyManager;
        this.chatGPTService = chatGPTService;

        setupFrame();
        buildUI();
    }

    private void setupFrame() {
        setTitle("📊 מערכת ניהול סקרים — Telegram Bot");
        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);

        // Set RTL orientation
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));

        // --- Header ---
        add(createHeaderPanel(), BorderLayout.NORTH);

        // --- Main Split Pane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBackground(BG_DARK);
        splitPane.setDividerSize(3);
        splitPane.setDividerLocation(850); // Set initial location to give Survey Creation more space
        splitPane.setResizeWeight(0.8); // Ensure left side (Survey Creation) gets 80% of extra space
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);

        // Right side: Community (in RTL, right is displayed on the left side of screen)
        communityPanel = new CommunityPanel(communityManager);
        splitPane.setRightComponent(communityPanel);

        // Left side: Dynamic content
        rightCardLayout = new CardLayout();
        rightPanel = new JPanel(rightCardLayout);
        rightPanel.setBackground(BG_DARK);

        surveyCreationPanel = new SurveyCreationPanel(
                surveyManager, communityManager, chatGPTService,
                this::showActiveSurveyPanel
        );

        activeSurveyPanel = new ActiveSurveyPanel(
                surveyManager,
                this::showResultsPanel
        );

        surveyResultsPanel = new SurveyResultsPanel(
                surveyManager,
                this::showCreateSurveyPanel
        );

        rightPanel.add(surveyCreationPanel, CARD_CREATE);
        rightPanel.add(activeSurveyPanel, CARD_ACTIVE);
        rightPanel.add(surveyResultsPanel, CARD_RESULTS);

        splitPane.setLeftComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);

        // --- Footer ---
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = createPanel(BG_CARD);
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_BLUE),
                createPaddedBorder(PADDING)
        ));
        header.setPreferredSize(new Dimension(0, 60));

        JLabel titleLabel = createLabel("📊  מערכת ניהול סקרים", FONT_TITLE, TEXT_PRIMARY);
        titleLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel subtitleLabel = createLabel("Telegram Bot Survey Manager", FONT_SMALL, TEXT_MUTED);
        subtitleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        header.add(titleLabel, BorderLayout.EAST);
        header.add(subtitleLabel, BorderLayout.WEST);

        return header;
    }

    private JPanel createFooterPanel() {
        JPanel footer = createPanel(BG_CARD);
        footer.setLayout(new FlowLayout(FlowLayout.CENTER));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        footer.setPreferredSize(new Dimension(0, 30));

        JLabel footerLabel = createLabel("🤖 בוט פעיל ומחובר", FONT_SMALL, ACCENT_GREEN);
        footer.add(footerLabel);

        return footer;
    }

    // --- Navigation ---

    private void showActiveSurveyPanel() {
        SwingUtilities.invokeLater(() -> {
            activeSurveyPanel.refreshAll();
            rightCardLayout.show(rightPanel, CARD_ACTIVE);
        });
    }

    private void showResultsPanel() {
        SwingUtilities.invokeLater(() -> {
            Survey survey = surveyManager.getLastSurvey();
            surveyResultsPanel.showResults(survey);
            rightCardLayout.show(rightPanel, CARD_RESULTS);
        });
    }

    private void showCreateSurveyPanel() {
        SwingUtilities.invokeLater(() -> {
            rightCardLayout.show(rightPanel, CARD_CREATE);
        });
    }
}
