package org.example.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * קבועי עיצוב למשק ה-Swing.
 * פלטת צבעים כהה מודרנית עם עברית RTL.
 */
public class UIConstants {

    // --- Color Palette ---
    public static final Color BG_DARK = new Color(24, 24, 32);
    public static final Color BG_PANEL = new Color(32, 33, 46);
    public static final Color BG_CARD = new Color(42, 43, 58);
    public static final Color BG_INPUT = new Color(52, 53, 68);
    public static final Color BG_HOVER = new Color(62, 63, 78);

    public static final Color TEXT_PRIMARY = new Color(230, 230, 240);
    public static final Color TEXT_SECONDARY = new Color(160, 162, 180);
    public static final Color TEXT_MUTED = new Color(110, 112, 130);

    public static final Color ACCENT_BLUE = new Color(88, 130, 247);
    public static final Color ACCENT_BLUE_HOVER = new Color(108, 150, 255);
    public static final Color ACCENT_GREEN = new Color(72, 199, 142);
    public static final Color ACCENT_ORANGE = new Color(255, 170, 80);
    public static final Color ACCENT_RED = new Color(235, 87, 87);
    public static final Color ACCENT_PURPLE = new Color(156, 120, 243);

    public static final Color TABLE_ROW_ALT = new Color(36, 37, 52);
    public static final Color TABLE_SELECTION = new Color(58, 90, 160);
    public static final Color BORDER_COLOR = new Color(55, 56, 72);

    // --- Status Colors ---
    public static final Color STATUS_COMPLETED = ACCENT_GREEN;
    public static final Color STATUS_IN_PROGRESS = ACCENT_ORANGE;
    public static final Color STATUS_NOT_STARTED = TEXT_MUTED;

    // --- Fonts (Increased for better readability) ---
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 15);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_COUNTER = new Font("Segoe UI", Font.BOLD, 42);
    public static final Font FONT_COUNTDOWN = new Font("Consolas", Font.BOLD, 32);
    public static final Font FONT_TABLE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 14);

    // --- Dimensions ---
    public static final int PADDING = 20;
    public static final int PADDING_SMALL = 10;
    public static final int BORDER_RADIUS = 16;
    public static final Dimension BUTTON_SIZE = new Dimension(170, 44);

    // --- Factory Methods ---

    /**
     * יוצר כפתור מעוצב.
     */
    public static JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY_BOLD);
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(BUTTON_SIZE);
        // FlatLaf handles hover, borders, and rounded corners automatically
        return button;
    }

    /**
     * יוצר label מעוצב.
     */
    public static JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /**
     * יוצר פאנל עם רקע כהה.
     */
    public static JPanel createPanel(Color bg) {
        JPanel panel = new JPanel();
        panel.setBackground(bg);
        return panel;
    }

    /**
     * יוצר border מעוצב.
     */
    public static Border createPaddedBorder(int padding) {
        return BorderFactory.createEmptyBorder(padding, padding, padding, padding);
    }

    /**
     * יוצר border עם כותרת.
     */
    public static Border createTitledSectionBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(PADDING_SMALL, 0, PADDING_SMALL, 0),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                        BorderFactory.createEmptyBorder(PADDING_SMALL, 0, 0, 0)
                )
        );
    }

    /**
     * מעצב טבלה בסגנון כהה.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_TABLE);
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BG_PANEL);
        table.setGridColor(BORDER_COLOR);
        table.setSelectionBackground(TABLE_SELECTION);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // Renderer לשורות מחלופות
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? BG_PANEL : TABLE_ROW_ALT);
                }
                c.setForeground(TEXT_PRIMARY);
                ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);
                return c;
            }
        });

        // Header
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_TABLE_HEADER);
        header.setForeground(TEXT_SECONDARY);
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_BLUE));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(BG_CARD);
                c.setForeground(TEXT_SECONDARY);
                c.setFont(FONT_TABLE_HEADER);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);
                ((JLabel) c).setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_BLUE),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
                return c;
            }
        });
    }

    /**
     * יוצר ScrollPane מעוצב.
     */
    public static JScrollPane createStyledScrollPane(Component view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBackground(BG_PANEL);
        scrollPane.getViewport().setBackground(BG_PANEL);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(20); // Make scrolling fast
        return scrollPane;
    }

    /**
     * יוצר שדה טקסט מעוצב.
     */
    public static JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(FONT_BODY);
        field.setPreferredSize(new Dimension(200, 44));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        return field;
    }

    /**
     * יוצר TextArea מעוצב.
     */
    public static JTextArea createStyledTextArea(int rows, int cols) {
        JTextArea area = new JTextArea(rows, cols);
        area.setFont(FONT_BODY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        return area;
    }

    /**
     * Scrollbar מודרני.
     */
    // ModernScrollBarUI removed because FlatLaf handles scrollbars beautifully
}
