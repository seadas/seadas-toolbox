package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import gov.nasa.gsfc.seadas.earthdatacloud.matchup.HarmonyEnvironment;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * What the matchup window shows, laid out as the design mockup: the steps down
 * the left, the open step, and a footer with the Harmony environment, Help,
 * Back and Next.
 */
final class MatchupPanel extends JPanel {

    private static final int OBSERVATIONS = 0;
    private static final int SATELLITE = 1;
    private static final int RUN = 2;

    private final MatchupSession session;
    private final List<Step> steps;
    private final List<NavItem> navItems = new ArrayList<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel stepPanel = new JPanel(cards);
    private final Look.Wrap navNote = new Look.Wrap("");
    private final JButton help = new JButton("Help");
    private final JButton back = new JButton("‹ Back");
    private final JButton next = new JButton("Next ›");
    private final JButton closeButton = new JButton("Close");
    private final Runnable close;
    private int current;

    MatchupPanel(MatchupSession session, Runnable close) {
        super(new BorderLayout());
        this.session = session;
        this.close = close;
        steps = List.of(
                new ObservationsStep(session, () -> go(SATELLITE)),
                new SatelliteStep(session),
                new RunStep(session, () -> go(RUN + 1)),
                new ResultsStep(session));
        for (int i = 0; i < steps.size(); i++) {
            stepPanel.add(steps.get(i), String.valueOf(i));
        }

        add(nav(), BorderLayout.WEST);
        add(stepPanel, BorderLayout.CENTER);
        add(footer(), BorderLayout.SOUTH);

        session.addListener(this::updateNavigation);
        setPreferredSize(new Dimension(1280, 780));
        go(OBSERVATIONS);
    }

    private JComponent nav() {
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBackground(Look.NAV);
        nav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Look.NAV_LINE),
                BorderFactory.createEmptyBorder(16, 12, 16, 12)));
        for (int i = 0; i < steps.size(); i++) {
            NavItem item = new NavItem(i);
            navItems.add(item);
            nav.add(item);
            nav.add(Box.createVerticalStrut(4));
        }
        nav.add(Box.createVerticalGlue());
        navNote.setForeground(Look.MUTED);
        navNote.setFont(Look.font(-1, Font.PLAIN));
        navNote.setAlignmentX(LEFT_ALIGNMENT);
        navNote.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        nav.add(navNote);
        nav.setPreferredSize(new Dimension(220, 10));
        return nav;
    }

    private JComponent footer() {
        HarmonyEnvironment environment = session.options().environment();
        JLabel where = new JLabel("Harmony: " + (environment == HarmonyEnvironment.UAT
                ? "UAT (testing data only)" : "production") + " · Earthdata Login from ~/.netrc");
        where.setForeground(environment == HarmonyEnvironment.UAT ? Look.ORANGE_TEXT : new Color(0x3D4955));
        where.setToolTipText("Change the environment in the Earthdata Cloud options");

        help.addActionListener(e -> JOptionPane.showMessageDialog(this, Text.wrap(steps.get(current).help()),
                "Help: " + steps.get(current).title(), JOptionPane.INFORMATION_MESSAGE));
        back.addActionListener(e -> go(current - 1));
        next.addActionListener(e -> next());
        closeButton.addActionListener(e -> this.close.run());

        JPanel buttons = Look.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(help);
        buttons.add(back);
        buttons.add(next);
        buttons.add(closeButton);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Look.FOOTER);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Look.FOOTER_LINE),
                BorderFactory.createEmptyBorder(12, 16, 12, 10)));
        footer.add(where, BorderLayout.WEST);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    MatchupSession session() {
        return session;
    }

    Step step(int index) {
        return steps.get(index);
    }

    int current() {
        return current;
    }

    void go(int index) {
        if (index < 0 || index >= steps.size()) {
            return;
        }
        String reason = steps.get(index).unavailableReason();
        if (reason != null) {
            JOptionPane.showMessageDialog(this, reason, steps.get(index).title(), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        current = index;
        cards.show(stepPanel, String.valueOf(index));
        steps.get(index).shown();
        updateNavigation();
    }

    /** Next; from the satellite step it also starts the run, as its button says. */
    private void next() {
        if (current == SATELLITE && steps.get(RUN) instanceof RunStep run && run.canStart() && !session.started()) {
            go(RUN);
            run.start(false);
            return;
        }
        go(current + 1);
    }

    private void updateNavigation() {
        Step step = steps.get(current);
        boolean last = current == steps.size() - 1;
        back.setEnabled(current > 0);
        next.setVisible(!last);
        closeButton.setVisible(last);
        next.setText(step.nextLabel());
        String reason = last ? null : steps.get(current + 1).unavailableReason();
        next.setEnabled(!last && reason == null);
        next.setToolTipText(reason);
        String note = step.navNote();
        navNote.setText(note == null ? "" : note);
        navItems.forEach(NavItem::refresh);
        if (getRootPane() != null) {
            getRootPane().setDefaultButton(last ? null : next);
        }
    }

    /** Whether closing may go ahead; asks the user when work is going on, and stops it. */
    boolean confirmClose() {
        if (steps.stream().noneMatch(Step::busy)) {
            return true;
        }
        int answer = JOptionPane.showConfirmDialog(this, Text.wrap("Work is going on. Closing stops it and "
                        + "cancels this run's jobs in Harmony; the run is saved and can be resumed later.\n\nClose?"),
                "Close", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION) {
            return false;
        }
        steps.forEach(Step::stop);
        return true;
    }

    /** A step in the list: its number in a circle, then its name. */
    private final class NavItem extends JComponent {
        private final int index;
        private boolean hover;

        NavItem(int index) {
            this.index = index;
            setAlignmentX(LEFT_ALIGNMENT);
            setFont(Look.font(1, Font.PLAIN));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (index != current) {
                        go(index);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void refresh() {
            setToolTipText(steps.get(index).unavailableReason());
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(196, 44);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 44);
        }

        @Override
        protected void paintComponent(Graphics g) {
            boolean on = index == current;
            boolean available = steps.get(index).unavailableReason() == null;
            boolean done = index < current;
            Graphics2D g2 = Look.smooth(g);
            if (on || hover) {
                g2.setColor(on ? Look.BLUE_PALE : new Color(0xE3E8ED));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            int d = 24;
            int cx = 12;
            int cy = (getHeight() - d) / 2;
            Color circle = on ? Look.BLUE : done ? Look.BLUE_PALER : Color.WHITE;
            Color ring = on || done ? Look.BLUE : Look.BORDER_DARK;
            Color number = on ? Color.WHITE : done ? Look.BLUE : new Color(0x3D4955);
            g2.setColor(circle);
            g2.fillOval(cx, cy, d, d);
            g2.setColor(ring);
            g2.drawOval(cx, cy, d - 1, d - 1);
            Font small = Look.font(-1, Font.BOLD);
            g2.setFont(small);
            g2.setColor(number);
            String n = String.valueOf(index + 1);
            var metrics = g2.getFontMetrics();
            g2.drawString(n, cx + (d - metrics.stringWidth(n)) / 2,
                    cy + (d - metrics.getHeight()) / 2 + metrics.getAscent());
            Font font = on ? getFont().deriveFont(Font.BOLD) : getFont();
            g2.setFont(font);
            metrics = g2.getFontMetrics();
            g2.setColor(available || on ? Look.TEXT : Look.BORDER_DARK);
            g2.drawString(steps.get(index).title(), cx + d + 10,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g2.dispose();
        }
    }
}
