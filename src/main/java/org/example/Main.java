package org.example;

import org.example.bot.SurveyBot;
import org.example.service.ChatGPTService;
import org.example.service.CommunityManager;
import org.example.service.SurveyManager;
import org.example.ui.MainFrame;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * נקודת כניסה ראשית — מפעיל את הבוט ואת ממשק ה-Swing.
 */
public class Main {

    public static void main(String[] args) {
        // טעינת הגדרות
        Properties config = loadConfig();
        String botToken = config.getProperty("bot.token", "");
        String openAiKey = config.getProperty("openai.api.key", "");

        if (botToken.isEmpty() || botToken.equals("YOUR_BOT_TOKEN_HERE")) {
            System.err.println("========================================");
            System.err.println("  ERROR: Bot token is not configured!");
            System.err.println("  Please update src/main/resources/config.properties");
            System.err.println("  with your Telegram bot token.");
            System.err.println("========================================");
            System.exit(1);
        }

        // יצירת services
        CommunityManager communityManager = new CommunityManager();
        SurveyManager surveyManager = new SurveyManager(communityManager);
        ChatGPTService chatGPTService = new ChatGPTService(openAiKey);

        // הפעלת Swing GUI
        SwingUtilities.invokeLater(() -> {
            try {
                // Dark look & feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // fallback to default
            }

            MainFrame frame = new MainFrame(communityManager, surveyManager, chatGPTService);
            frame.setVisible(true);

            System.out.println("✅ Swing GUI started successfully.");
        });

        // הפעלת הבוט ב-thread ראשי
        startBot(botToken, communityManager, surveyManager);
    }

    private static void startBot(String botToken, CommunityManager communityManager,
                                  SurveyManager surveyManager) {
        try {
            TelegramBotsLongPollingApplication botsApp = new TelegramBotsLongPollingApplication();
            SurveyBot bot = new SurveyBot(botToken, communityManager, surveyManager);
            botsApp.registerBot(botToken, bot);

            System.out.println("✅ Telegram Bot started successfully.");
            System.out.println("🤖 Bot is ready and listening for messages...");

            // Keep the application running
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("❌ Failed to start Telegram Bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("⚠️ config.properties not found in resources!");
            }
        } catch (IOException e) {
            System.err.println("⚠️ Failed to load config.properties: " + e.getMessage());
        }
        return props;
    }
}
