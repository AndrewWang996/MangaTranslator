import java.io.*;
import java.util.*;
import com.google.cloud.vision.v1.*;    // not sure if importing all is a great idea

public class TextRecognizerGoogle implements TextRecognizer {

    public static void detectTextGcs(String gcsPath, PrintStream out) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(gcsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    out.printf("Error: %s\n", res.getError().getMessage());
                    return;
                }

                // For full list of available annotations, see http://g.co/cloud/vision/docs
                for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                    out.printf("Text: %s\n", annotation.getDescription());
                    out.printf("Position : %s\n", annotation.getBoundingPoly());
                }
            }
        }
    }

    public String getTextFromImageFile(File f, Language language) {
        return "";
    }

    /**
     * Change our enum language to the String version for our
     * language for the Google OCR implementation.
     * @param language enum representing language
     * @return String version for Google OCR implementation
     */
    public String implSpecificLanguage(Language language) {
        switch (language) {
            case JPN:
                return "ja";
            case JPN_VERT:
                return "ja";
                // google treats vertical japanese the same ?
            case EN:
                return "en";
            default:
                return "en";
        }
    }

    public static void main(String[] args) {
        File imageFile = new File("random_manga_images/easy/watari_4.jpeg");
        TextRecognizer tess = new TextRecognizerGoogle();
        String text = tess.getTextFromImageFile(imageFile, Language.JPN);
        System.out.println(text);
    }
}
