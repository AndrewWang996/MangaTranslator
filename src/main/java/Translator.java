// Imports the Google Cloud client library
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

public class Translator {
    Translate translationEngine;

    public String translate(String text, String sourceLanguage, String targetLanguage) {
        Translation translation = this.translationEngine.translate(
                text,
                TranslateOption.sourceLanguage(sourceLanguage),
                TranslateOption.targetLanguage(targetLanguage)
        );
        return translation.getTranslatedText();
    }

    public String translateJapaneseToEnglish(String japaneseText) {
        return translate(japaneseText, "ja", "en");
    }

    public Translator() {
        this.translationEngine = TranslateOptions.getDefaultInstance().getService();
    }

    public static void main(String... args) throws Exception {
        Translator translator = new Translator();
        String japaneseText = "私は美味しいですよ";
        String text = translator.translateJapaneseToEnglish(japaneseText);
        System.out.printf("Text: %s%n", japaneseText);
        System.out.printf("Translation: %s%n", text);
    }
}