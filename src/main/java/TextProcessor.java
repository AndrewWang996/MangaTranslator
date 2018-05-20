import java.awt.*;
import java.util.ArrayList;

public class TextProcessor {
    final Graphics2D g2;

    public TextProcessor(Graphics2D g2) {
        this.g2 = g2;
    }

    public Font getFont() {
        return this.g2.getFont();
    }

    /**
     * @return height of single line of text to be displayed in a single line, in pixels
     */
    public int lineHeight() {
        return g2.getFontMetrics().getHeight();
    }


    public int lineWidth(String str) {
        FontMetrics fontMetrics = this.g2.getFontMetrics();
        return fontMetrics.stringWidth(str.trim());
    }


    /**
     * NOTE: a line of text may expand beyond lineWidth if an individual word has longer length
     *
     * @param txt text to be displayed
     * @param lineWidth width in pixels of a line of text
     * @return the height in pixels of the text
     */
    public int textHeight(String txt, int lineWidth) {
        String[] lines = this.splitIntoLines(txt, lineWidth);
        return this.textHeight(lines);
    }

    public int textHeight(String[] lines) {
        return lines.length * this.lineHeight();
    }

    public void resizeToFit(String text, Rectangle box) {
        int vSize = verticalFontSizeLimit(text, box);
        int hSize = horizontalFontSizeLimit(text, box);

        int fontSize = Math.min(vSize, hSize);
        this.setFontSize(fontSize);
    }


    public String[] splitIntoLines(String str, int lineWidth) {
        str = str.trim().replaceAll(" +", " ");

        FontMetrics fontMetric = g2.getFontMetrics();
        ArrayList<String> lines = new ArrayList<>();

        StringBuffer lineBuffer = new StringBuffer();
        for (String word : str.split("\\s+")) {
            String x = lineBuffer.toString().trim() + " " + word;
            int currLen = fontMetric.stringWidth(x);
            if (currLen > lineWidth) {
                if (lineBuffer.length() == 0) {
                    lines.add(x);
                }
                else {
                    lines.add(lineBuffer.toString().trim());
                    lineBuffer = new StringBuffer(word);
                }
            }
            else {
                lineBuffer.append(" " + word);
            }
        }
        if (lineBuffer.length() > 0) {
            lines.add(lineBuffer.toString().trim());
        }

        // convert arraylist into array
        String[] linesArray = new String[lines.size()];
        for (int i=0; i<lines.size(); i++) {
            linesArray[i] = lines.get(i);
        }
        return linesArray;
    }

    /**
     * @param text text to be displayed
     * @param box text box that text is to be displayed in
     * @return upper bound on font size given this limit
     */
    public int horizontalFontSizeLimit(String text, Rectangle box) {
        int lineWidth = box.width();
        String longestLine = "";
        int maxLength = -1;
        for (String line : text.split("\\s+")) {
            if (this.lineWidth(line) > maxLength) {
                maxLength = this.lineWidth(line);
                longestLine = line;
            }
        }

        // set upper bound on font size
        Font font = g2.getFont();
        int fontSize = font.getSize();
        while ( this.lineWidth(longestLine) <= lineWidth ) {
            fontSize *= 2;
            this.setFontSize(fontSize);
        }

        // binary search for best font size
        int sLo = 0, sHi = fontSize;
        while (sLo < sHi) {
            fontSize = (sLo + sHi + 1) / 2;
            this.setFontSize(fontSize);
            if (this.lineWidth(longestLine) > lineWidth) {
                sHi = fontSize - 1;
            }
            else {
                sLo = fontSize;
            }
        }

        return sLo;
    }

    public int verticalFontSizeLimit(String txt, Rectangle box) {
        int lineWidth = box.width();
        int height = box.height();

        // set upper bound on font size
        Font font = g2.getFont();
        int fontSize = font.getSize();
        while ( this.textHeight(txt, lineWidth) <= height ) {
            fontSize *= 2;
            this.setFontSize(fontSize);
        }

        // binary search for best font size
        int sLo = 0, sHi = fontSize;
        while (sLo < sHi) {
            fontSize = (sLo + sHi + 1) / 2;
            this.setFontSize(fontSize);
            if (this.textHeight(txt, lineWidth) > height) {
                sHi = fontSize - 1;
            }
            else {
                sLo = fontSize;
            }
        }

        return sLo;
    }

    private void setFontSize(int fontSize) {
        Font font = g2.getFont();
        Font resizedFont = font.deriveFont(
                font.getStyle(),
                fontSize
        );
        g2.setFont(resizedFont);
    }
}
