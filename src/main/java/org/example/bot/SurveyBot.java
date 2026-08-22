package org.example.bot;

import org.example.model.*;
import org.example.service.CommunityManager;
import org.example.service.SurveyManager;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;

/**
 * בוט Telegram לניהול סקרים.
 * מטפל בהצטרפות לקהילה, שליחת סקרים ואיסוף תשובות.
 */
public class SurveyBot implements LongPollingSingleThreadUpdateConsumer, SurveyManager.BotMessageSender {

    private final TelegramClient telegramClient;
    private final CommunityManager communityManager;
    private final SurveyManager surveyManager;

    // Emoji constants
    private static final String EMOJI_WAVE = "\uD83D\uDC4B";
    private static final String EMOJI_PARTY = "\uD83C\uDF89";
    private static final String EMOJI_PEOPLE = "\uD83D\uDC65";
    private static final String EMOJI_CHECK = "✅";
    private static final String EMOJI_BELL = "\uD83D\uDD14";
    private static final String EMOJI_CLIPBOARD = "\uD83D\uDCCB";
    private static final String EMOJI_LOCK = "\uD83D\uDD12";
    private static final String EMOJI_WARNING = "⚠️";
    private static final String EMOJI_STAR = "⭐";
    private static final String EMOJI_POINT = "\uD83D\uDC49";

    public SurveyBot(String botToken, CommunityManager communityManager, SurveyManager surveyManager) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.communityManager = communityManager;
        this.surveyManager = surveyManager;
        this.surveyManager.setBotMessageSender(this);
    }

    @Override
    public void consume(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            System.err.println("Error processing update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Text Message Handling ---

    private void handleTextMessage(Message message) {
        String text = message.getText().trim();
        long chatId = message.getChatId();
        User user = message.getFrom();

        // בדיקת הודעות הצטרפות
        if (text.equals("/start") || text.equalsIgnoreCase("היי") || text.equalsIgnoreCase("Hi")) {
            handleJoinRequest(chatId, user);
        } else {
            sendMessage(chatId,
                    EMOJI_WARNING + " לא הצלחתי להבין את ההודעה.\n\n" +
                    "כדי להצטרף לקהילה, שלח:\n" +
                    EMOJI_POINT + " /start\n" +
                    EMOJI_POINT + " היי\n" +
                    EMOJI_POINT + " Hi");
        }
    }

    private void handleJoinRequest(long chatId, User user) {
        if (communityManager.isMember(chatId)) {
            sendMessage(chatId,
                    EMOJI_CHECK + " אתה כבר חבר בקהילה!\n" +
                    "אין צורך להצטרף שוב. נעדכן אותך כשיתחיל סקר חדש " + EMOJI_CLIPBOARD);
            return;
        }

        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String username = user.getUserName();

        CommunityMember newMember = communityManager.addMember(chatId, firstName, lastName, username);
        if (newMember == null) {
            return; // לא אמור לקרות (כבר בדקנו), אבל ליתר ביטחון
        }

        // הודעת ברוכים הבאים לחבר החדש
        sendMessage(chatId,
                EMOJI_PARTY + " ברוכים הבאים לקהילה, " + newMember.getDisplayName() + "!\n\n" +
                EMOJI_PEOPLE + " מספר חברי הקהילה: " + communityManager.getMemberCount() + "\n\n" +
                "תקבל/י הודעה כשיתחיל סקר חדש " + EMOJI_CLIPBOARD);

        // הודעה לכל שאר חברי הקהילה
        notifyCommunityAboutNewMember(newMember);
    }

    private void notifyCommunityAboutNewMember(CommunityMember newMember) {
        String notification = EMOJI_WAVE + " חבר/ה חדש/ה הצטרף/ה לקהילה!\n\n" +
                EMOJI_STAR + " שם: " + newMember.getDisplayName() + "\n" +
                EMOJI_PEOPLE + " חברי הקהילה כעת: " + communityManager.getMemberCount();

        for (CommunityMember member : communityManager.getMembers()) {
            if (member.getChatId() != newMember.getChatId()) {
                sendMessage(member.getChatId(), notification);
            }
        }
    }

    // --- Callback Query Handling ---

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        String callbackId = callbackQuery.getId();

        // פורמט: "answer_Q_O" (Q=questionIndex, O=optionIndex)
        if (data.startsWith("answer_")) {
            handleSurveyAnswer(chatId, data, callbackId);
        }

        // תמיד לענות על ה-callback כדי לבטל את ה-loading
        answerCallback(callbackId, null);
    }

    private void handleSurveyAnswer(long chatId, String data, String callbackId) {
        try {
            String[] parts = data.split("_");
            if (parts.length != 3) return;

            int questionIndex = Integer.parseInt(parts[1]);
            int optionIndex = Integer.parseInt(parts[2]);

            Survey activeSurvey = surveyManager.getActiveSurvey();
            if (activeSurvey == null || !activeSurvey.isActive()) {
                answerCallback(callbackId, EMOJI_LOCK + " הסקר כבר הסתיים.");
                return;
            }

            SurveyParticipant participant = activeSurvey.getParticipant(chatId);
            if (participant == null) {
                answerCallback(callbackId, EMOJI_WARNING + " אינך משתתף/ת בסקר זה.");
                return;
            }

            if (participant.hasAnswered(questionIndex)) {
                answerCallback(callbackId, EMOJI_WARNING + " כבר ענית על שאלה זו.");
                return;
            }

            boolean recorded = surveyManager.recordAnswer(chatId, questionIndex, optionIndex);
            if (recorded) {
                SurveyQuestion question = activeSurvey.getQuestion(questionIndex);
                String optionText = question != null ? question.getOptions().get(optionIndex) : "";

                answerCallback(callbackId, EMOJI_CHECK + " תשובתך נרשמה: " + optionText);

                // הודעה למשתמש על התקדמות
                String progress = participant.getProgressString();
                if (participant.hasCompleted()) {
                    sendMessage(chatId,
                            EMOJI_PARTY + " סיימת לענות על כל שאלות הסקר!\n" +
                            "תודה רבה על ההשתתפות " + EMOJI_CHECK);
                } else {
                    sendMessage(chatId,
                            EMOJI_CHECK + " תשובתך לשאלה " + (questionIndex + 1) + " נרשמה.\n" +
                            EMOJI_CLIPBOARD + " התקדמות: " + progress);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid callback data: " + data);
        }
    }

    // --- BotMessageSender Implementation ---

    @Override
    public void sendSurveyToParticipant(long chatId, Survey survey) {
        // הודעת פתיחה
        sendMessage(chatId,
                EMOJI_CLIPBOARD + " *סקר חדש!*\n\n" +
                "מספר שאלות: " + survey.getQuestionCount() + "\n" +
                "זמן מענה: " + "5 דקות\n\n" +
                "ענה/י על השאלות הבאות:");

        // שליחת כל שאלה עם כפתורי תשובה
        for (SurveyQuestion question : survey.getQuestions()) {
            sendQuestionMessage(chatId, question);
        }
    }

    private void sendQuestionMessage(long chatId, SurveyQuestion question) {
        String text = EMOJI_POINT + " *שאלה " + (question.getQuestionIndex() + 1) + ":*\n" +
                question.getQuestionText();

        // יצירת כפתורי תשובה
        List<InlineKeyboardRow> rows = new ArrayList<>();
        List<String> options = question.getOptions();

        for (int i = 0; i < options.size(); i++) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(options.get(i))
                    .callbackData("answer_" + question.getQuestionIndex() + "_" + i)
                    .build();
            rows.add(new InlineKeyboardRow(button));
        }

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(markup)
                .build();

        try {
            telegramClient.execute(message);
        } catch (Exception e) {
            System.err.println("Failed to send question to " + chatId + ": " + e.getMessage());
        }
    }

    @Override
    public void sendReminder(long chatId, Survey survey) {
        int answered = 0;
        SurveyParticipant p = survey.getParticipant(chatId);
        if (p != null) {
            answered = p.getAnsweredCount();
        }

        sendMessage(chatId,
                EMOJI_BELL + " *תזכורת!*\n\n" +
                "טרם סיימת לענות על הסקר.\n" +
                "ענית על " + answered + " מתוך " + survey.getQuestionCount() + " שאלות.\n\n" +
                "נותרו כ-2 דקות להשלמת המענה! " + EMOJI_WARNING);
    }

    @Override
    public void sendSurveyClosed(long chatId) {
        sendMessage(chatId,
                EMOJI_LOCK + " *הסקר הסתיים!*\n\n" +
                "תודה לכל מי שהשתתף " + EMOJI_PARTY + "\n" +
                "התוצאות יפורסמו בקרוב.");
    }

    // --- Utility Methods ---

    private void sendMessage(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .build();
        try {
            telegramClient.execute(message);
        } catch (Exception e) {
            System.err.println("Failed to send message to " + chatId + ": " + e.getMessage());
        }
    }

    private void answerCallback(String callbackId, String text) {
        AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .text(text)
                .build();
        try {
            telegramClient.execute(answer);
        } catch (Exception e) {
            System.err.println("Failed to answer callback: " + e.getMessage());
        }
    }
}
