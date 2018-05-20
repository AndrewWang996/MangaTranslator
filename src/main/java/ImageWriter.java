import com.google.cloud.vision.v1.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImageWriter {

    private final BufferedImage img;
    private final SpeechBubbleDetector bubbleDetector;
    private final Translator translator;

    public ImageWriter(String filepath) {
        this.img = loadImage(filepath);
        this.bubbleDetector = new SpeechBubbleDetector(this.img);
        this.translator = new Translator();
    }



    protected static BufferedImage loadImage(String filepath) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(filepath));
        } catch (IOException e) {
            System.err.printf("failed to retrieve image %s\n", filepath);
        }
        return img;
    }

    private void overlayRect(Rectangle rect) {
        Graphics g = this.img.getGraphics();

        g.setColor(
                this.bubbleDetector.calculateEdgeColor(
                        new Rectangle(
                                rect.UL.x - 1,
                                rect.UL.y - 1,
                                rect.BR.x + 1,
                                rect.BR.y + 1
                        )
                )
        );

        g.fillRect(
                rect.UL.x,
                rect.UL.y,
                rect.width(),
                rect.height()
        );
    }

    private void overlayText(String text, Rectangle rect) {
        Graphics g = this.img.getGraphics();
        g.setColor(Color.BLACK);
        text = text.trim().replaceAll(" +", " ");

        int fontSize = 20;
        Font font = new Font("Stencil", Font.PLAIN, fontSize);
        try {
            URL fontURL = new URL(new URL("file:"), "Fonts/animeace.ttf");
            font = Font.createFont(Font.TRUETYPE_FONT, fontURL.openStream());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FontFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        g.setFont(font);
        TextProcessor txtProcessor = new TextProcessor((Graphics2D)g);
        txtProcessor.resizeToFit(text, rect);
        g.setFont(txtProcessor.getFont());
        String[] lines = txtProcessor.splitIntoLines(text, rect.width());
        this.drawStrings((Graphics2D)g, lines, rect);
    }


    private void drawStrings(Graphics2D g2, String[] lines, Rectangle box) {
        TextProcessor txtProcessor = new TextProcessor(g2);
        int textHeight = txtProcessor.textHeight(lines);
        int x, y = box.ymin() + (box.height() - textHeight) / 2;
        for (String line : lines) {
            line = line.trim();
            y += txtProcessor.lineHeight();
            int lineWidth = txtProcessor.lineWidth(line);
            x = (box.xmin() + box.xmax()) / 2 - lineWidth / 2;
            g2.drawString(line, x, y);
        }
    }

    public void writeParagraphs(List<Paragraph> paragraphs) {
        for (Paragraph p : paragraphs) {
            BoundingPoly box = p.getBoundingBox();
            Rectangle rect = toRectangle(box);
            rect = this.bubbleDetector.getLargestExpansion(rect);
            overlayRect(rect);
            String text = paragraphText(p, " ");
            overlayText(text, rect);
        }
    }

    private String paragraphText(Paragraph p, String wordDelimiter) {
        StringBuilder strBuilder = new StringBuilder();
        for (Word word : p.getWordsList()) {
            strBuilder.append(wordText(word));
            strBuilder.append(wordDelimiter);
        }
        return strBuilder.toString();
    }

    private String wordText(Word w) {
        StringBuilder strBuilder = new StringBuilder();
        for (Symbol s : w.getSymbolsList()) {
            strBuilder.append(s.getText());
        }
        return strBuilder.toString();
    }


    public void drawSpeechBubbles(
            List<Paragraph> paragraphs
    ) {
        for (Paragraph p : paragraphs) {
            BoundingPoly box = p.getBoundingBox();
            Rectangle rect = toRectangle(box);

            Ellipse2D ellipse = new Ellipse2D.Float(
                    rect.UL.x,
                    rect.UL.y,
                    (float)rect.width(),
                    (float)rect.height()
            );

            Graphics2D g = (Graphics2D)img.getGraphics();
            g.setColor(Color.BLACK);
            g.draw(ellipse);
        }
    }

    private void displayImage() {
        JFrame window = new JFrame();
        window.setSize(1000, 1000);
        window.setTitle("Manga Translator");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);


        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(img, 0, 0, null);
            }
        };

        window.add(panel);
        panel.setVisible(true);
        window.setVisible(true);
    }

    private static Rectangle toRectangle(BoundingPoly box) {
        int xmax, ymax, xmin,ymin;
        xmax = ymax = - (1 << 30);
        xmin = ymin = (1 << 30);
        for (Vertex v : box.getVerticesList()) {
            if (v.getX() > xmax) { xmax = v.getX(); }
            if (v.getY() > ymax) { ymax = v.getY(); }
            if (v.getX() < xmin) { xmin = v.getX(); }
            if (v.getY() < ymin) { ymin = v.getY(); }
        }
        return new Rectangle(xmin, ymin, xmax, ymax);
    }

    /**
     * Sometimes Google's OCR fails to correctly box the japanese text.
     * This method is to account for this.
     *
     * @param paragraphs list of paragraphs
     */
    private void resizeParagraphs(List<Paragraph> paragraphs) {
        for (int i=0; i<paragraphs.size(); i++) {
            Paragraph p = paragraphs.get(i);
            Rectangle rect = toRectangle(p.getBoundingBox());
            rect = new SpeechBubbleDetector(this.img).fixParagraph(rect);
            paragraphs.set(i, p.toBuilder()
                    .setBoundingBox(rect.toBoundingPoly())
                    .build()
                );
        }
    }

    private List<Paragraph> translatedParagraphs(List<Paragraph> paragraphs) {
        List<Paragraph> newParagraphs = new ArrayList<>();
        for (int i=0; i<paragraphs.size(); i++) {
            Paragraph p = paragraphs.get(i);
            String jText = paragraphText(p, "");
            String eText = this.translator.translateJapaneseToEnglish(jText);

            newParagraphs.add( p.toBuilder()
                    .clearWords()
                    .addAllWords(toWords(eText))
                    .build()
            );
        }
        return newParagraphs;
    }

    private List<Word> toWords(String text) {
        List<Word> words = new ArrayList<>();
        for (String w : text.split("\\s+")) {
            Word.Builder wordBuilder = Word.newBuilder();
            for (char c : w.toCharArray()) {
                Symbol symbol = Symbol.newBuilder().setText("" + c).build();
                wordBuilder.addSymbols(symbol);
            }
            words.add(wordBuilder.build());
        }
        return words;
    }

    public static void main(String[] args) {
        String filepath = "random_manga_images/dagashi.jpeg";

        TextRecognizerGoogle txtRec = new TextRecognizerGoogle(Language.JPN);
        java.util.List<Paragraph> paragraphs = new ArrayList<>();



        try {
            paragraphs = txtRec.detectDocumentText(filepath);
        } catch(Exception e) {
            System.err.printf("Exception %s caught!", e.toString());
            System.exit(0);
        }

        ImageWriter iw = new ImageWriter(filepath);
        iw.resizeParagraphs(paragraphs);
//        iw.drawSpeechBubbles(paragraphs);
        iw.writeParagraphs( iw.translatedParagraphs(paragraphs) );
        iw.displayImage();
    }
}
