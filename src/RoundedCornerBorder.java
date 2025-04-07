import javax.swing.border.Border;
import java.awt.*;

// Custom border for rounded (curved) text fields.
public class RoundedCornerBorder implements Border {
    private int radius;

    public RoundedCornerBorder(int radius) {
        this.radius = radius;
    }
    
    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
    }
    
    @Override
    public boolean isBorderOpaque() {
        return false;
    }
    
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        // Smooth the border.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        g2.dispose();
    }
} 