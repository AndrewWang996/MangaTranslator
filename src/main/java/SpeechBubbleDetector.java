import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class SpeechBubbleDetector {
    private final BufferedImage img;
    private static final float COLOR_DIFF_MAX = 0.02f;
    private static final Color TRANSLUCENT = new Color(
            Color.LIGHT_GRAY.getRed(),
            Color.LIGHT_GRAY.getGreen(),
            Color.LIGHT_GRAY.getBlue(),
            155
    );

    public SpeechBubbleDetector(BufferedImage img) {
        this.img = img;
    }

    /**
     * TODO: Implement. For now, we are using WHITE
     * @param box
     * @return
     */
    public Color calculateSurroundingColor(Rectangle box) {
        return TRANSLUCENT;
    }

    public Color calculateAverageColor(Rectangle box) {
        int R=0;
        int G=0;
        int B=0;
        int n=0;
        for (int x = box.UL.x; x <= box.BR.x; x++) {
            for (int y = box.UL.y; y <= box.BR.y; y++) {
                Color pixel = new Color(img.getRGB(x,y));
                R += pixel.getRed();
                G += pixel.getGreen();
                B += pixel.getBlue();
                n ++;
            }
        }
        return new Color(R/n, G/n, B/n);
    }

    private Color calculateAverageColor(Ellipse2D bubble) {
        int R=0;
        int G=0;
        int B=0;
        int n=0;
        for (int x=0; x<this.img.getWidth(); x++) {
            for (int y=0; y<this.img.getHeight(); y++) {
                if ( ! bubble.contains(x, y)) {
                    continue;
                }

                Color pixel = new Color(img.getRGB(x,y));
                R += pixel.getRed();
                G += pixel.getGreen();
                B += pixel.getBlue();
                n ++;
            }
        }
        return new Color(R/n, G/n, B/n);
    }

    public Rectangle fixParagraph(Rectangle box) {
        Color c = this.calculateAverageColor(box);
        Rectangle newBox = box.copy();

        // expand vertically upward
        while (newBox.UL.y > 0) {
            newBox.UL.y --;
            Color nC = this.calculateAverageColor(newBox);
            if (colorDiff(c, nC) > COLOR_DIFF_MAX) {
                break;
            }
        }

        return newBox;
    }

    private float colorDiff(Color cA, Color cB) {
        int dR = Math.abs(cA.getRed() - cB.getRed());
        int dG = Math.abs(cA.getGreen() - cB.getGreen());
        int dB = Math.abs(cA.getBlue() - cB.getBlue());

        return (dR + dG + dB) / 256f;
    }

}