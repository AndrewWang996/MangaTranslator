import com.google.cloud.vision.v1.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TextRecognizerGoogleTest {
    private static TextRecognizerGoogle recognizerJPN;

    @Before
    public void setup() {
        recognizerJPN = new TextRecognizerGoogle(Language.JPN);
    }

    /**
     * Creates Vertex using two positive integers
     */
    private Vertex toVertex(int x, int y) {
        return Vertex.newBuilder().setX(x).setY(y).build();
    }

    private BoundingPoly toQuad(int x1, int y1,
                              int x2, int y2,
                              int x3, int y3,
                              int x4, int y4) {
        Vertex v0 = toVertex(x1, y1);
        Vertex v1 = toVertex(x2, y2);
        Vertex v2 = toVertex(x3, y3);
        Vertex v3 = toVertex(x4, y4);
        return BoundingPoly.newBuilder()
            .addVertices(v0)
            .addVertices(v1)
            .addVertices(v2)
            .addVertices(v3)
            .build();
    }

    private Paragraph toParagraph(
            BoundingPoly boundingPoly,
            List<Word> words) {
        return Paragraph.newBuilder()
                .setBoundingBox(boundingPoly)
                .addAllWords(words)
                .build();
    }

    /**
     * Does not produce any boundingPoly information.
     * Splits text into words based off punctuation + whitespace
     *  (removing them). Then splits words into symbols.
     *
     * @param text any string of text
     * @return list of words
     */
    private List<Word> toWords(String text) {
        List<Word> words = new ArrayList<>();
        for (String word : text.split("[\\p{Punct}\\s]+")) {
            Word.Builder wBuilder = Word.newBuilder();
            for(char symbol : word.toCharArray()) {  // not sure if this works
                Symbol s = Symbol.newBuilder().setText("" + symbol).build();
                wBuilder.addSymbols(s);
            }
            words.add(wBuilder.build());
        }
        return words;
    }

    /**
     * Test the method alignedBoundingBox(BoundingPoly)
     * by testing on the following cases:
     *  - Null
     *  - No area
     *  - x negative / positive
     *  - y negative / positive
     *  - unordered vertices
     *  - > 4 vertices
     *      - < 4 vertices
     */
    public void testAlignedBoundingBox() {
        /* Null */
        BoundingPoly nullCase = recognizerJPN.alignedBoundingBox(null);
        assertEquals(nullCase, null);

        /* No area */
        BoundingPoly noArea = recognizerJPN.alignedBoundingBox(
                toQuad(
                        0, 0,
                        0, 0,
                        1, 1,
                        1, 1
                )
        );
        assertEquals(noArea, toQuad(0, 0,
                                    0, 1,
                                    1, 1,
                                    1, 0));

        /* x negative */
        BoundingPoly negativeX = recognizerJPN.alignedBoundingBox(
                toQuad(
                        -1, 1,
                        -5, 4,
                        3, 8,
                        3, 0
                )
        );
        assertEquals(negativeX, toQuad(-5, 0,
                                        -5, 8,
                                        3, 8,
                                        3, 0));

        /* y negative */
        BoundingPoly negativeY = recognizerJPN.alignedBoundingBox(
                toQuad(
                        1,-3,
                        3,-4,
                        0,10,
                        2, 11
                )
        );
        assertEquals(negativeY, toQuad(0, -4,
                                        0, 11,
                                        3,11,
                                        3, -4));
    }


    /**
     * Test the method joinAllParagraphs(List<Paragraph>)
     * by testing on the following cases:
     *  - <= 1 pixel away
     *  - > 1 pixel away
     *  - # paragraphs
     *      - 2 paragraphs
     *      - > 2 paragraphs
     *      - 1, no paragraphs
     *  - order of paragraphs mixed
     *  - language of recognizer (JPN vs other)
     */
    @Test
    public void testJoinAllParagraphs() {
        BoundingPoly b1 = toQuad(1, 2, 2, 2, 2, 1, 1, 1);
        List<Word> w1 = toWords("デイリーアニメランキング");
        Paragraph p1 = toParagraph(b1, w1);

        BoundingPoly b2 = toQuad(2, 2, 3, 2, 3, 1, 2, 1);
        List<Word> w2 = toWords("最も人気のアニメ");
        Paragraph p2 = toParagraph(b2, w2);



        /* order of paragraphs (left->right) (given JPN language) */
        List<Paragraph> paragraphs1 = new ArrayList<>();
        paragraphs1.add(p1);
        paragraphs1.add(p2);
        List<Paragraph> ordered1 = recognizerJPN.joinAllParagraphs(paragraphs1);
        assertEquals(ordered1.size(), 1);

        List<Word> orderedWords1 = new ArrayList<>();
        orderedWords1.addAll(w1);
        orderedWords1.addAll(w2);
        assertEquals(ordered1.get(0), toParagraph(
                toQuad(1, 2, 3, 2, 3, 1, 1, 1),
                orderedWords1
        ));

        /* mixed order of paragraphs (right->left) (given JPN language) */
        List<Paragraph> paragraphs2 = new ArrayList<>();
        paragraphs2.add(p2);
        paragraphs2.add(p1);
        List<Paragraph> ordered2 = recognizerJPN.joinAllParagraphs(paragraphs2);
        assertEquals(ordered2.size(), 1);

        List<Word> orderedWords2 = new ArrayList<>();
        orderedWords2.addAll(w1);
        orderedWords2.addAll(w2);
        assertEquals(ordered2.get(0), toParagraph(
                toQuad(1, 2, 3, 2, 3, 1, 1, 1),
                orderedWords2
        ));

    }
}