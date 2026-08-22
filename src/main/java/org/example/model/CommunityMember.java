package org.example.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * מייצג חבר בקהילה הגלובלית.
 * מכיל מידע בסיסי על המשתמש ומועד הצטרפותו.
 */
public class CommunityMember {

    private final long chatId;
    private final String firstName;
    private final String lastName;
    private final String username;
    private final LocalDateTime joinedAt;

    public CommunityMember(long chatId, String firstName, String lastName, String username) {
        this.chatId = chatId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.joinedAt = LocalDateTime.now();
    }

    public long getChatId() {
        return chatId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    /**
     * מחזיר את השם המלא של המשתמש.
     */
    public String getDisplayName() {
        if (lastName != null && !lastName.isEmpty()) {
            return firstName + " " + lastName;
        }
        return firstName;
    }

    /**
     * מחזיר את שם המשתמש ב-Telegram (עם @) או "—" אם לא קיים.
     */
    public String getDisplayUsername() {
        if (username != null && !username.isEmpty()) {
            return "@" + username;
        }
        return "—";
    }

    /**
     * מחזיר את מועד ההצטרפות בפורמט קריא.
     */
    public String getFormattedJoinTime() {
        return joinedAt.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommunityMember that = (CommunityMember) o;
        return chatId == that.chatId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(chatId);
    }

    @Override
    public String toString() {
        return getDisplayName() + " (" + getDisplayUsername() + ")";
    }
}
