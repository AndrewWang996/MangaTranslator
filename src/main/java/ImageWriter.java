import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageWriter {

    private static BufferedImage loadImage(String filepath) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(filepath));
        } catch (IOException e) {
            System.err.printf("failed to retrieve image %s\n", filepath);
        }
        return img;
    }

    private static BufferedImage overlayText(BufferedImage img, String text) {

    }

    public static void main(String[] args) {
//        BufferedImage bImg = loadImage("random_manga_images/dagashi_3.jpeg");
//        Graphics G = Graphics.create(0, 0, 1000, 1000);
//        G.drawImage(bImg, 0, 0, Color.GREEN, null);

        JFrame window = new JFrame();
        window.setSize(1000, 1000);
        window.setTitle("Manga Translator");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);


        BufferedImage img = loadImage("random_manga_images/dagashi_3.jpeg");

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(img, 0, 0, null);
            }
        };

        window.add(panel);

    }
}
