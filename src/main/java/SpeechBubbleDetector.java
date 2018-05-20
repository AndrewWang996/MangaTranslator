import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class SpeechBubbleDetector {
    private final BufferedImage img;
    private static final float COLOR_STDEV_LIM = 25.0f;
    private static final float BUBBLE_COLOR_ALLOWED_ERROR = 0.3f; // a percentage
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
     * Given a text box as a rectangle, we wish to find the rectangle that
     * 1. contains the text box
     * 2. does not extend past the speech bubble
     * 3. has the maximum possible area (width * height)
     *
     * NOTE: sometimes, we don't actually have a speech bubble...
     * eg. when we have a large panel with just a small piece of text in the center.
     * TODO: Address this case
     *
     * NOTE: Right now, we implement this by extending horizontally in both directions and similar for vertical
     * expansion. We should fix this so that it is possible to extend different amounts in all directions.
     *
     * @param box text box
     * @return largest containing box not extending past the speech bubble
     */
    public Rectangle getLargestExpansion(Rectangle box) {
        int xmin, ymin, xmax, ymax;
        int x = 0;
        Color bubbleBackgroundColor = this.calculateSpeechBubbleColor(box);
        Rectangle containingBox;
        Rectangle bestBox = box;
        do {
            xmin = box.UL.x - x;
            xmax = box.BR.x + x;
            int y = 0;
            do {
                ymin = box.UL.y - y;
                ymax = box.BR.y + y;
                containingBox = new Rectangle(xmin, ymin, xmax, ymax);
                if (containingBox.area() > bestBox.area()) {
                    bestBox = containingBox;
                }
                y ++;
            } while (inSpeechBubble(
                    containingBox,
                    bubbleBackgroundColor)
                    );
            x ++;
        } while (inSpeechBubble(
                new Rectangle(xmin, box.ymin(), xmax, box.ymax()),
                bubbleBackgroundColor
        ));
        return bestBox;
    }

    public boolean inSpeechBubble(Rectangle box, Color bubbleBackground) {
        if (box.xmin() < 0 || box.ymin() < 0 || box.xmax() >= img.getWidth() || box.ymax() >= img.getHeight()) {
            return false;
        }
        for (int x = box.xmin(); x <= box.xmax(); x++) {
            Color top = new Color(img.getRGB(x, box.ymin()));
            if ( ! validColor(top, bubbleBackground) ) {
                return false;
            }
            Color bot = new Color(img.getRGB(x, box.ymax()));
            if ( ! validColor(bot, bubbleBackground) ) {
                return false;
            }
        }

        for (int y = box.ymin(); y <= box.ymax(); y++) {
            Color left = new Color(img.getRGB(box.xmin(), y));
            if ( ! validColor(left, bubbleBackground) ) {
                return false;
            }
            Color right = new Color(img.getRGB(box.xmax(), y));
            if ( ! validColor(right, bubbleBackground) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if the color is within a certain margin of error of the speech bubble's background color
     * @param c color
     * @param bubbleBackground speech bubble background color
     * @return as described
     */
    private boolean validColor(Color c, Color bubbleBackground) {
        return this.colorDiff(c, bubbleBackground) < BUBBLE_COLOR_ALLOWED_ERROR;
    }

    public Color calculateSpeechBubbleColor(Rectangle box) {
        box = this.clampRect(box);
        int xmin = box.UL.x;
        int ymin = box.UL.y;
        int xmax = box.BR.x;
        int ymax = box.BR.y;

        Color top = calculateAverageColor(new Rectangle(xmin, ymin, xmax, ymin));
        Color bot = calculateAverageColor(new Rectangle(xmin, ymax, xmax, ymax));
        Color left = calculateAverageColor(new Rectangle(xmin, ymin, xmin, ymax));
        Color right = calculateAverageColor(new Rectangle(xmax, ymin, xmax, ymax));


        int R = 0, G = 0, B = 0;
        for (Color c : new Color[]{top, bot, left, right}) {
            if (c.getRed() > R) R = c.getRed();
            if (c.getGreen() > G) G = c.getGreen();
            if (c.getBlue() > B) B = c.getBlue();
        }

        return new Color(R, G, B);
    }

    public Color calculateAverageColor(Rectangle box) {
        int R=0;
        int G=0;
        int B=0;
        int n=0;
        for (int x = box.xmin(); x <= box.xmax(); x++) {
            for (int y = box.ymin(); y <= box.ymax(); y++) {
                Color pixel = new Color(img.getRGB(x,y));
                R += pixel.getRed();
                G += pixel.getGreen();
                B += pixel.getBlue();
                n ++;
            }
        }
        return new Color(R/n, G/n, B/n);
    }

    private float calculateColorStdev(Rectangle box) {
        Color avgColor = this.calculateAverageColor(box);
        float sum = 0;
        float n = 0;
        for (int x = box.xmin(); x <= box.xmax(); x++) {
            for (int y = box.ymin(); y <= box.ymax(); y++) {
                Color pixel = new Color(img.getRGB(x,y));
                sum += Math.pow(pixel.getRed() - avgColor.getRed(), 2);
                sum += Math.pow(pixel.getGreen() - avgColor.getGreen(), 2);
                sum += Math.pow(pixel.getBlue() - avgColor.getBlue(), 2);
                n += 3;
            }
        }
        float variance = sum / n;
        return (float)Math.sqrt(variance);
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
        Rectangle newBox = box.copy();

        // expand vertically upward
        while (newBox.UL.y > 0) {
            float stdev = calculateColorStdev(
                    new Rectangle(
                            newBox.xmin(),
                            newBox.ymin(),
                            newBox.xmax(),
                            newBox.ymin()
                    )
            );
            if (stdev < COLOR_STDEV_LIM) {
                break;
            }
            newBox.UL.y --;
        }
        newBox.UL.y ++;
        return newBox;
    }

    private float colorDiff(Color cA, Color cB) {
        int dR = Math.abs(cA.getRed() - cB.getRed());
        int dG = Math.abs(cA.getGreen() - cB.getGreen());
        int dB = Math.abs(cA.getBlue() - cB.getBlue());

        return (dR + dG + dB) / (3 * 256f);
    }

    private Rectangle clampRect(Rectangle box) {
        int xmin = Math.max(0, box.xmin());
        int xmax = Math.min(img.getWidth() - 1, box.xmax());
        int ymin = Math.max(0, box.ymin());
        int ymax = Math.min(img.getHeight() - 1, box.ymax());
        return new Rectangle(xmin, ymin, xmax, ymax);
    }

}