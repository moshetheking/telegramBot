package org.example.service;

import org.example.model.CommunityMember;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * מנהל את הקהילה הגלובלית של המשתמשים.
 * מספק מנגנון listener לעדכון ממשק ה-Swing בזמן אמת.
 */
public class CommunityManager {

    /**
     * ממשק listener לאירועי קהילה.
     */
    public interface CommunityListener {
        void onMemberJoined(CommunityMember member);
    }

    private final Map<Long, CommunityMember> members = new ConcurrentHashMap<>();
    private final List<CommunityListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * מוסיף חבר חדש לקהילה.
     * @return החבר שנוסף, או null אם כבר קיים
     */
    public CommunityMember addMember(long chatId, String firstName, String lastName, String username) {
        if (members.containsKey(chatId)) {
            return null; // כבר חבר
        }

        CommunityMember member = new CommunityMember(chatId, firstName, lastName, username);
        members.put(chatId, member);

        // הודעה ל-listeners
        for (CommunityListener listener : listeners) {
            listener.onMemberJoined(member);
        }

        return member;
    }

    /**
     * בודק אם משתמש כבר חבר בקהילה.
     */
    public boolean isMember(long chatId) {
        return members.containsKey(chatId);
    }

    /**
     * מחזיר את כל חברי הקהילה כרשימה ממוינת לפי מועד הצטרפות.
     */
    public List<CommunityMember> getMembers() {
        List<CommunityMember> memberList = new ArrayList<>(members.values());
        memberList.sort(Comparator.comparing(CommunityMember::getJoinedAt));
        return memberList;
    }

    /**
     * מחזיר חבר לפי chatId.
     */
    public CommunityMember getMember(long chatId) {
        return members.get(chatId);
    }

    /**
     * מחזיר את מספר חברי הקהילה.
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * מוסיף listener לאירועי קהילה.
     */
    public void addListener(CommunityListener listener) {
        listeners.add(listener);
    }

    /**
     * מסיר listener.
     */
    public void removeListener(CommunityListener listener) {
        listeners.remove(listener);
    }
}
