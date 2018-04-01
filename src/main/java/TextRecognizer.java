import java.io.File;

public interface TextRecognizer {

    Language getLanguage();

    String getTextFromImageFile(File f);

    /**
     * Change our enum language to the String language we desire.
     * Of course, this is dependent on the implementation we are using.
     * @param language enum representing language
     * @return String, in the implementation chosen
     */
    String implSpecificLanguage(Language language);

}
