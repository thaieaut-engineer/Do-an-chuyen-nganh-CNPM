package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import model.schedule.ScheduleModels.Assignment;

final class ScheduleDetailDialog extends JDialog {
    ScheduleDetailDialog(Window owner, Assignment assignment, Runnable onCancel) {
        super(owner, "Chi tiết phân công " + assignment.id(), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent(assignment, onCancel));
        setMinimumSize(new Dimension(560, 430));
        setSize(660, 520);
        setLocationRelativeTo(owner);
    }

    private JPanel createContent(Assignment assignment, Runnable onCancel) {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(18, 20, 16, 20));

        JLabel title = new JLabel("Chi tiết phân công lịch làm việc");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(MainFrame.TEXT);
        root.add(title, BorderLayout.NORTH);

        JTextArea details = new JTextArea(format(assignment));
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        details.setForeground(MainFrame.TEXT);
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainFrame.BORDER),
                new EmptyBorder(14, 14, 14, 14)));
        JScrollPane scroll = new JScrollPane(details);
        scroll.setBorder(null);
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(4, 0, 0, 0));
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        if (onCancel != null && !"DaHuy".equals(assignment.status())) {
            JButton cancel = new JButton("Hủy phân công");
            view.component.UIStyle.dangerButton(cancel);
            cancel.addActionListener(event -> {
                dispose();
                onCancel.run();
            });
            actions.add(cancel);
        }
        footer.add(actions, BorderLayout.WEST);

        JButton close = new JButton("Đóng");
        view.component.UIStyle.secondaryButton(close);
        close.addActionListener(event -> dispose());
        JPanel closePanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        closePanel.setOpaque(false);
        closePanel.add(close);
        footer.add(closePanel, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private String format(Assignment a) {
        return "PHÂN CÔNG LỊCH LÀM VIỆC\n"
                + "Mã phân công: " + a.id() + "\n"
                + "Trạng thái: " + safe(a.status()) + "\n"
                + "Ngày làm: " + a.workDate() + "\n"
                + "Thời gian: " + a.start() + " - " + a.end() + "\n"
                + "Ca làm việc: " + safe(a.shiftName()) + "\n"
                + "Địa điểm: " + safe(a.location()) + "\n\n"
                + "BÁC SĨ\n"
                + "Họ tên: " + safe(a.doctorName()) + "\n"
                + "Chuyên khoa: " + safe(a.specialty()) + "\n\n"
                + "KHUNG GIỜ\n"
                + "Tổng số khung giờ: " + a.slotCount() + "\n"
                + "Đã đặt: " + a.bookedCount();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
