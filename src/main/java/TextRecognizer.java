import java.io.File;

public interface TextRecognizer {

    String getTextFromImageFile(File f, Language language);

    /**
     * Change our enum language to the String language we desire.
     * Of course, this is dependent on the implementation we are using.
     * @param language enum representing language
     * @return String, in the implementation chosen
     */
    String implSpecificLanguage(Language language);

}
