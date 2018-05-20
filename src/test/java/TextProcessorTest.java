import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TextProcessorTest {
    private Graphics2D g2;
    private TextProcessor tP;

    @Before
    public void setup() {
        String filepath = "./random_manga_images/easy/hanebado.jpeg";
        BufferedImage img = null;
        try {
            File f = new File(filepath);
            img = ImageIO.read(f);
        } catch (IOException e) {
            System.err.printf("failed to retrieve image %s\n", filepath);
        }
        this.g2 = img.createGraphics();
        this.tP = new TextProcessor(this.g2);
    }

    @Test
    public void testSplitIntoLines() {
        // TODO: Implement test
        String[] lines = tP.splitIntoLines("Makino Tsukushi is a tough, hard-working, middle-class student at the prestigious " +
                "escalator school Eitoku Gakuen. Initially, Makino wanted to attend Eitoku because her idol, an " +
                "internationally renowned model named Todou Shizuka, was an alumna of the school. Not long after " +
                "however, Makino discovers the superficial nature of her classmates. Their arrogance and her inability" +
                " to relate to them because of her social status, limits her chances at making friends. Worse yet, the " +
                "school is ruled by the F4 or Flower Four, composed of playboys Nishikado Soujiro and Mimasaka Akira, " +
                "introverted Hanazawa Rui and violent and bratty Domyouji Tsukasa. The F4, sons of Japan's wealthiest " +
                "and most powerful tycoons, bully fellow students out of boredom or malevolence until they are " +
                "expelled or quit.", 40);

        for (String line : lines) {
            System.out.println(line);
        }

    }
}
