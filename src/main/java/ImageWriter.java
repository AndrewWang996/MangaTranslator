import com.google.cloud.vision.v1.BoundingPoly;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Vertex;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.color.ColorSpace;
import java.util.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Font;
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

    private static BufferedImage overlayRect(
            BufferedImage img,
            Color rectColor,
            int x, int y,
            int w, int h
    ) {
        Graphics g = img.getGraphics();
        g.setColor(rectColor);
        g.fillRect(x, y, w, h);
        return img;
    }

    private static BufferedImage overlayText(
            BufferedImage img,
            String text,
            int textSize,
            int x, int y
            ) {
        Graphics g = img.getGraphics();
        g.setFont( new Font("Arial Black", Font.BOLD, textSize) );
        g.drawString(text, x, y);
        return img;
    }

    /**
     *
     * @param img
     * @return
     */
    public static BufferedImage writeParagraphs(
            BufferedImage img,
            List<Paragraph> paragraphs
    ) {
        for (Paragraph p : paragraphs) {
            BoundingPoly box = p.getBoundingBox();
            Vertex topLeft = getTopLeft(box);
            int x = topLeft.getX();
            int y = topLeft.getY();
            Vertex botRight = getBottomRight(box);
            int w = Math.abs(x - botRight.getX());
            int h = Math.abs(y - botRight.getY());



            Color gray = Color.LIGHT_GRAY;
            ColorSpace space = gray.getColorSpace();
            float[] components = new float[3];
            gray.getColorComponents(space, components);
            Color rectColor = new Color(space, components, 0.5f);
            overlayRect(img, rectColor, x, y, w, h);
            // TODO: Replace color with method that determines color of background

            // TODO: Write text
        }
        return img;
    }

    private static void displayImage(BufferedImage img) {
        JFrame window = new JFrame();
        window.setSize(1000, 1000);
        window.setTitle("Manga Translator");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);


        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                System.out.println("painting the image");
                g.drawImage(img, 0, 0, null);
                System.out.println("painted the image");
            }
        };

        window.add(panel);
        panel.setVisible(true);
        window.setVisible(true);
    }

    private static Vertex getTopLeft(BoundingPoly box) {
        int x,y;
        x = y = (1 << 30);
        for (Vertex v : box.getVerticesList()) {
            if (v.getX() < x) { x = v.getX(); }
            if (v.getY() < y) { y = v.getY(); }
        }
        return Vertex.newBuilder().setX(x).setY(y).build();
    }

    private static Vertex getBottomRight(BoundingPoly box) {
        int x,y;
        x = y = - (1 << 30);
        for (Vertex v : box.getVerticesList()) {
            if (v.getX() > x) { x = v.getX(); }
            if (v.getY() > y) { y = v.getY(); }
        }
        return Vertex.newBuilder().setX(x).setY(y).build();
    }

    public static void main(String[] args) {

        String filepath = "/Users/andywang/IdeaProjects/mangatranslator" +
                "/random_manga_images/easy/hanebado.jpeg";
        BufferedImage img = loadImage(filepath);
        TextRecognizerGoogle txtRec = new TextRecognizerGoogle(Language.JPN);
        java.util.List<Paragraph> paragraphs = new ArrayList<>();

        try {
            paragraphs = txtRec.detectDocumentText(filepath);
        } catch(Exception e) {
            System.err.printf("Exception %s caught!", e.toString());
            System.exit(0);
        }

        Paragraph one = paragraphs.get(0);
        System.out.println(one.getBoundingBox());
        System.out.println(getTopLeft(one.getBoundingBox()));
        System.out.println(getBottomRight(one.getBoundingBox()));
        System.out.printf("Paragraphs length: %d\n", paragraphs.size());

        writeParagraphs(img, paragraphs);
        displayImage(img);
    }
}
