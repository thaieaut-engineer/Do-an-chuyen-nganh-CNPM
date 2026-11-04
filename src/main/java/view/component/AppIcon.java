package view.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

public final class AppIcon implements Icon {
    private final String name;
    private final Color color;
    private final int size;

    public AppIcon(String name, Color color) {
        this(name, color, 18);
    }

    public AppIcon(String name, Color color, int size) {
        this.name = name;
        this.color = color;
        this.size = size;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.scale(size / 20.0, size / 20.0);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        draw(g);
        g.dispose();
    }

    private void draw(Graphics2D g) {
        switch (name) {
            case "dashboard":
                g.drawRoundRect(2, 2, 7, 7, 2, 2);
                g.drawRoundRect(11, 2, 7, 5, 2, 2);
                g.drawRoundRect(2, 11, 7, 7, 2, 2);
                g.drawRoundRect(11, 9, 7, 9, 2, 2);
                break;
            case "patients":
                g.drawOval(7, 2, 6, 6);
                g.drawArc(3, 10, 14, 11, 0, 180);
                g.drawArc(1, 5, 7, 8, 35, 115);
                g.drawArc(12, 5, 7, 8, 30, 115);
                break;
            case "calendar":
                g.drawRoundRect(2, 4, 16, 14, 2, 2);
                g.drawLine(2, 8, 18, 8);
                g.drawLine(6, 2, 6, 6);
                g.drawLine(14, 2, 14, 6);
                g.fill(new Ellipse2D.Double(6, 11, 2, 2));
                g.fill(new Ellipse2D.Double(11, 11, 2, 2));
                break;
            case "add":
            case "medical":
                g.drawRoundRect(2, 2, 16, 16, 4, 4);
                g.drawLine(10, 5, 10, 15);
                g.drawLine(5, 10, 15, 10);
                break;
            case "clipboard":
                g.drawRoundRect(4, 3, 12, 15, 2, 2);
                g.drawRoundRect(7, 2, 6, 4, 1, 1);
                g.drawLine(7, 9, 13, 9);
                g.drawLine(7, 12, 13, 12);
                g.drawLine(7, 15, 11, 15);
                break;
            case "search":
                g.drawOval(2, 2, 11, 11);
                g.drawLine(11, 11, 18, 18);
                break;
            case "medicine":
                g.drawRoundRect(3, 5, 14, 10, 5, 5);
                g.drawLine(10, 5, 10, 15);
                g.drawLine(4, 10, 9, 10);
                break;
            case "box":
                g.drawLine(2, 6, 10, 2);
                g.drawLine(10, 2, 18, 6);
                g.drawLine(2, 6, 10, 10);
                g.drawLine(18, 6, 10, 10);
                g.drawLine(2, 6, 2, 15);
                g.drawLine(18, 6, 18, 15);
                g.drawLine(2, 15, 10, 19);
                g.drawLine(18, 15, 10, 19);
                g.drawLine(10, 10, 10, 19);
                break;
            case "flask":
                g.drawLine(7, 2, 13, 2);
                g.drawLine(8, 2, 8, 9);
                g.drawLine(12, 2, 12, 9);
                g.drawLine(8, 9, 3, 16);
                g.drawLine(12, 9, 17, 16);
                g.drawArc(3, 13, 14, 5, 180, 180);
                g.drawLine(6, 13, 14, 13);
                break;
            case "invoice":
                g.drawRoundRect(3, 2, 14, 16, 2, 2);
                g.drawLine(6, 6, 14, 6);
                g.drawLine(6, 9, 14, 9);
                g.drawLine(6, 12, 10, 12);
                g.drawLine(12, 14, 14, 14);
                break;
            case "wallet":
                g.drawRoundRect(2, 5, 16, 12, 2, 2);
                g.drawLine(3, 5, 5, 3);
                g.drawLine(5, 3, 17, 3);
                g.drawRoundRect(12, 9, 7, 5, 1, 1);
                g.fill(new Ellipse2D.Double(14, 10.5, 2, 2));
                break;
            case "shield":
                g.drawLine(10, 2, 17, 5);
                g.drawLine(17, 5, 16, 12);
                g.drawLine(16, 12, 10, 18);
                g.drawLine(10, 18, 4, 12);
                g.drawLine(4, 12, 3, 5);
                g.drawLine(3, 5, 10, 2);
                g.drawLine(10, 6, 10, 13);
                g.drawLine(7, 9, 13, 9);
                break;
            case "doctor":
                g.drawOval(7, 2, 6, 6);
                g.drawArc(4, 9, 12, 10, 180, 180);
                g.drawLine(7, 12, 7, 15);
                g.drawLine(7, 15, 10, 17);
                g.drawLine(10, 17, 13, 15);
                g.drawLine(13, 15, 13, 12);
                break;
            case "clock":
                g.drawOval(2, 2, 16, 16);
                g.drawLine(10, 5, 10, 10);
                g.drawLine(10, 10, 14, 12);
                break;
            case "equipment":
                g.drawRoundRect(3, 3, 14, 14, 2, 2);
                g.drawRoundRect(7, 7, 6, 6, 1, 1);
                g.drawLine(10, 1, 10, 3);
                g.drawLine(10, 17, 10, 19);
                g.drawLine(1, 10, 3, 10);
                g.drawLine(17, 10, 19, 10);
                break;
            case "star":
                int[] sx = {10, 12, 18, 13, 15, 10, 5, 7, 2, 8};
                int[] sy = {2, 8, 8, 12, 18, 15, 18, 12, 8, 8};
                g.drawPolygon(sx, sy, sx.length);
                break;
            case "user":
                g.drawOval(7, 2, 6, 6);
                g.drawRoundRect(3, 10, 14, 8, 4, 4);
                break;
            case "key":
                g.drawOval(2, 4, 8, 8);
                g.drawLine(9, 10, 18, 18);
                g.drawLine(14, 14, 16, 12);
                g.drawLine(16, 16, 18, 14);
                break;
            case "settings":
                g.drawOval(6, 6, 8, 8);
                g.drawOval(8, 8, 4, 4);
                g.drawLine(10, 1, 10, 5);
                g.drawLine(10, 15, 10, 19);
                g.drawLine(1, 10, 5, 10);
                g.drawLine(15, 10, 19, 10);
                g.drawLine(4, 4, 6, 6);
                g.drawLine(14, 14, 17, 17);
                g.drawLine(4, 16, 6, 14);
                g.drawLine(14, 6, 17, 4);
                break;
            case "bell":
                g.drawArc(4, 3, 12, 14, 0, 180);
                g.drawLine(4, 10, 4, 14);
                g.drawLine(16, 10, 16, 14);
                g.drawLine(4, 14, 16, 14);
                g.drawArc(7, 14, 6, 5, 180, 180);
                break;
            case "eye":
                g.drawArc(2, 5, 16, 10, 0, 180);
                g.drawArc(2, 5, 16, 10, 180, 180);
                g.drawOval(7, 7, 6, 6);
                break;
            case "menu":
                g.drawLine(3, 5, 17, 5);
                g.drawLine(3, 10, 17, 10);
                g.drawLine(3, 15, 17, 15);
                break;
            case "report":
                g.drawLine(3, 18, 3, 2);
                g.drawLine(3, 18, 18, 18);
                g.drawLine(6, 15, 6, 11);
                g.drawLine(10, 15, 10, 7);
                g.drawLine(14, 15, 14, 4);
                g.drawLine(18, 15, 18, 9);
                break;
            case "logout":
                g.drawRoundRect(2, 3, 10, 14, 2, 2);
                g.drawLine(9, 10, 18, 10);
                g.drawLine(14, 6, 18, 10);
                g.drawLine(14, 14, 18, 10);
                break;
            default:
                g.draw(new RoundRectangle2D.Double(3, 3, 14, 14, 3, 3));
        }
    }
}
