import java.awt.*;
import javax.swing.*;

// Custom panel that paints a vertical gradient background with fade support.
public class GradientPanel extends JPanel {
    private Color color1;
    private Color color2;
    private float alpha = 1f; // opacity level

    public GradientPanel(Color color1, Color color2) {
        this.color1 = color1;
        this.color2 = color2;
        setOpaque(false);
    }

    // Getters for original colors.
    public Color getColor1() {
        return color1;
    }
    public Color getColor2() {
        return color2;
    }
    // Allows changing the gradient colors.
    public void setColors(Color c1, Color c2) {
        this.color1 = c1;
        this.color2 = c2;
        repaint();
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
        repaint();
    }
    public float getAlpha() {
        return alpha;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        // Apply current opacity.
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        int w = getWidth();
        int h = getHeight();
        GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, w, h);
        g2d.dispose();
        super.paintComponent(g);
    }
} 