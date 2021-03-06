import net.sourceforge.tess4j.*;
import java.io.*;


/**
 * This does not accept colored images, only grayscale.
 */
public class TextRecognizerTesseract implements TextRecognizer {
    private final Language language;
    private ITesseract tess;

    public TextRecognizerTesseract(Language language) {
        tess = new Tesseract();
        this.language = language;
    }

    @Override
    public Language getLanguage() {
        return language;
    }

    /**
     * Change our enum language to the String version for our
     * language for the Tesseract implementation.
     * @param language enum representing language
     * @return String version for Tesseract implementation
     */
    public String implSpecificLanguage(Language language) {

        switch (language) {
            case JPN:
                return "jpn";
            case JPN_VERT:
                return "jpn_vert";
            case EN:
                return "en";
            default:
                return "en";
        }
    }

    public String getTextFromImageFile(File f) {
        tess = new Tesseract();
        tess.setLanguage( implSpecificLanguage(language) );

        String result;
        try {
            result = tess.doOCR(f);
        } catch (TesseractException e) {
            result = "Error: getTextFromImageFile";
            System.err.println(e.getMessage());
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        File imageFile = new File("random_manga_images/easy/watari_4.jpeg");
        TextRecognizer tess = new TextRecognizerTesseract(Language.JPN);
        String text = tess.getTextFromImageFile(imageFile);
        System.out.println(text);
    }
}
