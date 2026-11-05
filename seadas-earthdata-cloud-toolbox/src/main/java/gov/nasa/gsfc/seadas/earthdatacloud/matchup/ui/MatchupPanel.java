package gov.nasa.gsfc.seadas.earthdatacloud.matchup.ui;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/** What the matchup window shows: the steps down the left, the open step, Back, Next and Close. */
final class MatchupPanel extends JPanel {

    private final MatchupSession session;
    private final List<Step> steps;
    private final JList<Step> stepList;
    private final CardLayout cards = new CardLayout();
    private final JPanel stepPanel = new JPanel(cards);
    private final JButton back = new JButton("◂ Back");
    private final JButton next = new JButton("Next ▸");
    private final JLabel hint = new JLabel(" ");
    private final Runnable close;
    private int current;

    MatchupPanel(MatchupSession session, Runnable close) {
        super(new BorderLayout());
        this.session = session;
        this.close = close;
        ResultsStep results = new ResultsStep(session);
        steps = List.of(
                new ObservationsStep(session, () -> go(1)),
                new SatelliteStep(session),
                new RunStep(session, () -> go(3)),
                results);
        for (int i = 0; i < steps.size(); i++) {
            stepPanel.add(steps.get(i), String.valueOf(i));
        }

        stepList = new JList<>(steps.toArray(Step[]::new));
        stepList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stepList.setCellRenderer(new StepRenderer());
        stepList.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        stepList.setFixedCellWidth(170);
        stepList.setBackground(UIManager.getColor("Panel.background"));
        stepList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && stepList.getSelectedIndex() >= 0
                    && stepList.getSelectedIndex() != current) {
                go(stepList.getSelectedIndex());
            }
        });

        JPanel left = new JPanel(new BorderLayout());
        left.add(stepList, BorderLayout.CENTER);
        left.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Separator.foreground")));

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> this.close.run());
        back.addActionListener(e -> go(current - 1));
        next.addActionListener(e -> go(current + 1));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(back);
        buttons.add(next);
        buttons.add(closeButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 8));
        hint.setForeground(Color.GRAY);
        bottom.add(hint, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        add(left, BorderLayout.WEST);
        add(stepPanel, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        session.addListener(this::updateNavigation);
        setPreferredSize(new Dimension(980, 640));
        go(0);
    }

    MatchupSession session() {
        return session;
    }

    Step step(int index) {
        return steps.get(index);
    }

    void go(int index) {
        if (index < 0 || index >= steps.size()) {
            return;
        }
        String reason = steps.get(index).unavailableReason();
        if (reason != null) {
            stepList.setSelectedIndex(current);
            hint.setText(reason);
            return;
        }
        current = index;
        cards.show(stepPanel, String.valueOf(index));
        stepList.setSelectedIndex(index);
        steps.get(index).shown();
        updateNavigation();
    }

    private void updateNavigation() {
        back.setEnabled(current > 0);
        boolean last = current == steps.size() - 1;
        String reason = last ? null : steps.get(current + 1).unavailableReason();
        next.setEnabled(!last && reason == null);
        hint.setText(reason == null ? " " : reason);
        stepList.repaint();
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

    /** "1  Observations", grey when it can't be opened yet. */
    private final class StepRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                      boolean focus) {
            Step step = (Step) value;
            JLabel label = (JLabel) super.getListCellRendererComponent(list, (index + 1) + "   " + step.title(),
                    index, selected, false);
            label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            label.setFont(label.getFont().deriveFont(index == current ? Font.BOLD : Font.PLAIN));
            if (!selected && step.unavailableReason() != null) {
                label.setForeground(Color.GRAY);
            }
            return label;
        }
    }
}
