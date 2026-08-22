package org.example.ui;

import org.example.model.CommunityMember;
import org.example.service.CommunityManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import static org.example.ui.UIConstants.*;

/**
 * פאנל תצוגת הקהילה הגלובלית.
 * מציג רשימת חברים, שם משתמש, מועד הצטרפות ומונה חברים.
 * מתעדכן בזמן אמת כשמשתמש חדש מצטרף.
 */
public class CommunityPanel extends JPanel implements CommunityManager.CommunityListener {

    private final CommunityManager communityManager;
    private final DefaultTableModel tableModel;
    private final JTable membersTable;
    private final JLabel memberCountLabel;
    private final JLabel headerLabel;

    public CommunityPanel(CommunityManager communityManager) {
        this.communityManager = communityManager;
        this.communityManager.addListener(this);

        setLayout(new BorderLayout(0, PADDING));
        setBackground(BG_PANEL);
        setBorder(createPaddedBorder(PADDING));

        // --- Header ---
        JPanel headerPanel = createPanel(BG_PANEL);
        headerPanel.setLayout(new BorderLayout());

        headerLabel = createLabel("👥  הקהילה", FONT_TITLE, TEXT_PRIMARY);
        headerLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        memberCountLabel = createLabel("0 חברים", FONT_SUBTITLE, ACCENT_BLUE);
        memberCountLabel.setHorizontalAlignment(SwingConstants.LEFT);

        headerPanel.add(headerLabel, BorderLayout.EAST);
        headerPanel.add(memberCountLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // --- Table ---
        String[] columns = {"מועד הצטרפות", "Telegram Username", "שם"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        membersTable = new JTable(tableModel);
        styleTable(membersTable);
        membersTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        membersTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        membersTable.getColumnModel().getColumn(2).setPreferredWidth(180);

        JScrollPane scrollPane = createStyledScrollPane(membersTable);
        add(scrollPane, BorderLayout.CENTER);

        // --- Footer ---
        JPanel footerPanel = createPanel(BG_PANEL);
        footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JLabel footerNote = createLabel("הרשימה מתעדכנת בזמן אמת", FONT_SMALL, TEXT_MUTED);
        footerPanel.add(footerNote);
        add(footerPanel, BorderLayout.SOUTH);

        // טעינת חברים קיימים
        refreshTable();
    }

    /**
     * מרענן את טבלת החברים.
     */
    private void refreshTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            List<CommunityMember> members = communityManager.getMembers();
            for (CommunityMember member : members) {
                tableModel.addRow(new Object[]{
                        member.getFormattedJoinTime(),
                        member.getDisplayUsername(),
                        member.getDisplayName()
                });
            }
            memberCountLabel.setText(communityManager.getMemberCount() + " חברים");
        });
    }

    // --- CommunityListener ---

    @Override
    public void onMemberJoined(CommunityMember member) {
        refreshTable();
    }
}
