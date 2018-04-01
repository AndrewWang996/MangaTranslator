import java.io.*;
import java.util.*;
import java.util.function.Function;

import com.google.cloud.vision.v1.*;    // not sure if importing all is a great idea
import com.google.protobuf.ByteString;

/**
 * Instantiate this class without any arguments
 */
public class TextRecognizerGoogle implements TextRecognizer {
    private static final int INF = (1 << 30);
    private final Language language;

    public TextRecognizerGoogle(Language language) {
        this.language = language;
    }



    /**
     * Pass in a filepath that is a single image file.
     * [Not sure if this works with file containing multiple images]
     *
     * @param filePath single image file.
     * @throws Exception
     */
    public List<Paragraph> detectDocumentText(String filePath) throws Exception {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        List<Paragraph> paragraphs = new ArrayList<>();

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            client.close();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.err.printf("Error: %s\n", res.getError().getMessage());
                    return new ArrayList<>();
                }

                // For full list of available annotations, see http://g.co/cloud/vision/docs
                TextAnnotation annotation = res.getFullTextAnnotation();
                for (Page page: annotation.getPagesList()) {
                    for (Block block : page.getBlocksList()) {
                        paragraphs.addAll( block.getParagraphsList() );
                    }
                }
            }
        }

        return joinAllParagraphs( paragraphs );
    }

    /**
     * Find the minimum 4-sided axis aligned bounding box for any polygon
     *
     *
     *
     *
     * @param poly bounding polygon, any number of sides
     * @return axis aligned bounding rectangle, 4 sides
     */
    public BoundingPoly alignedBoundingBox(BoundingPoly poly) {
        if (poly == null) {
            return null;
        }
        int xmin, xmax, ymin, ymax;
        xmin = ymin = INF;
        xmax = ymax = - INF;

        for(Vertex v : poly.getVerticesList()) {
            if (v.getX() < xmin) xmin = v.getX();
            if (v.getY() < ymin) ymin = v.getY();
            if (v.getX() > xmax) xmax = v.getX();
            if (v.getY() > ymax) ymax = v.getY();
        }

        return boundingBoxFromAxes(xmin, ymin, xmax, ymax);
    }

    private BoundingPoly boundingBoxFromAxes(int xmin, int ymin, int xmax, int ymax) {
        BoundingPoly.Builder boxBuilder = BoundingPoly.newBuilder();
        int[] x = new int[]{xmin, xmax, xmax, xmin};
        int[] y = new int[]{ymax, ymax, ymin, ymin};
        for(int i=0; i<4; i++) {
            Vertex.Builder vertBuilder = Vertex.newBuilder();
            vertBuilder.setX(x[i]);
            vertBuilder.setY(y[i]);
            boxBuilder.addVertices(vertBuilder.build());
        }
        return boxBuilder.build();
    }

    private Vertex upperRightVertex(BoundingPoly poly) {
        int xmin, xmax, ymin, ymax;
        xmin = ymin = INF;
        xmax = ymax = - INF;

        for(Vertex v : poly.getVerticesList()) {
            if (v.getX() < xmin) xmin = v.getX();
            if (v.getY() < ymin) ymin = v.getY();
            if (v.getX() > xmax) xmax = v.getX();
            if (v.getY() > ymax) ymax = v.getY();
        }

        return Vertex.newBuilder().setX(xmax).setY(ymin).build();
    }

    /**
     * Join any paragraphs whose axis aligned bounding boxes intersect
     *  eg. they are <= 1 pixels away from each other.
     * Takes care to join text in correct order:
     *  example: For Japanese: up -> down, left -> right
     *
     * Note: This does NOT modify the original argument.
     * @param paragraphs list of paragraphs
     * @return new list of paragraphs as described
     */
    public List<Paragraph> joinAllParagraphs(List<Paragraph> paragraphs) {
        if (this.getLanguage() == Language.JPN) {
            // create copy so that original list is not modified
            paragraphs = new ArrayList<>(paragraphs);
            Collections.sort(paragraphs, new Comparator<Paragraph>() {
                @Override
                public int compare(Paragraph o1, Paragraph o2) {
                    Vertex v1 = upperRightVertex(o1.getBoundingBox());
                    Vertex v2 = upperRightVertex(o2.getBoundingBox());
                    return Integer.compare(v1.getX(), v2.getX());
                }
            });
        }
        else {
            // TODO: handle other languages (what order to join paragraphs)
        }


        ArrayList<Paragraph> paras = new ArrayList<>();
        for(Paragraph para : paragraphs) {
            BoundingPoly newBox = alignedBoundingBox( para.getBoundingBox() );
            Paragraph newPara = para.toBuilder().setBoundingBox( newBox ).build();
            paras.add( newPara );
        }

        boolean hasIntersection;
        do {
            hasIntersection = false;
            for (int i=0; i<paras.size() && ! hasIntersection; i++) {
                for (int j=i+1; j<paras.size(); j++) {
                    Paragraph p1 = paras.get(i);
                    Paragraph p2 = paras.get(j);
                    Optional<Paragraph> joinedParagraph = joinParagraphs(p1, p2);
                    if (joinedParagraph.isPresent()) {
                        hasIntersection = true;
                        paras.remove(j);
                        paras.remove(i);
                        paras.add(i, joinedParagraph.get());
                        break;
                    }
                }
            }
        } while(hasIntersection);

        return paras;
    }

    private Optional<Paragraph> joinParagraphs(Paragraph p1, Paragraph p2) {
        Optional<BoundingPoly> joinedBoundingBox = joinBoundingBoxes(
                p1.getBoundingBox(),
                p2.getBoundingBox()
        );
        if ( ! joinedBoundingBox.isPresent() ) {
            return Optional.empty();
        }
        Paragraph.Builder ParaBuilder = Paragraph.newBuilder();
        ParaBuilder.addAllWords( p1.getWordsList() );
        ParaBuilder.addAllWords( p2.getWordsList() );
        ParaBuilder.setBoundingBox( joinedBoundingBox.get() );
        return Optional.of( ParaBuilder.build() );
    }

    private Optional<BoundingPoly> joinBoundingBoxes(BoundingPoly p1, BoundingPoly p2) {
        if ( ! boxesIntersect(p1, p2) ) {
            return Optional.empty();
        }

        int xmin, xmax, ymin, ymax;
        xmin = ymin = INF;
        xmax = ymax = - INF;

        List<Vertex> vertices = new ArrayList<>();
        vertices.addAll(p1.getVerticesList());
        vertices.addAll(p2.getVerticesList());

        for(Vertex v : vertices) {
            if (v.getX() < xmin) xmin = v.getX();
            if (v.getY() < ymin) ymin = v.getY();
            if (v.getX() > xmax) xmax = v.getX();
            if (v.getY() > ymax) ymax = v.getY();
        }

        return Optional.of(boundingBoxFromAxes(xmin, ymin, xmax, ymax));
    }

    /**
     * Return whether 2 axis aligned bounding boxes intersect
     *
     * @param p1 first axis aligned bounding box
     * @param p2 second one
     * @return whether they intersect
     */
    private boolean boxesIntersect(BoundingPoly p1, BoundingPoly p2) {
        int xmin, xmax, ymin, ymax;
        xmin = ymin = INF;
        xmax = ymax = - INF;

        for(Vertex v : p1.getVerticesList()) {
            if (v.getX() < xmin) xmin = v.getX();
            if (v.getY() < ymin) ymin = v.getY();
            if (v.getX() > xmax) xmax = v.getX();
            if (v.getY() > ymax) ymax = v.getY();
        }

        for(Vertex v2 : p2.getVerticesList()) {
            if ( v2.getX() >= xmin && v2.getX() <= xmax
                    && v2.getY() >= ymin && v2.getY() <= ymax ) {
                return true;
            }
        }

        return false;
    }


    @Override
    public Language getLanguage() {
        return language;
    }

    public String getTextFromImageFile(File f) {
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
        TextRecognizer tess = new TextRecognizerGoogle(Language.JPN);
        String text = tess.getTextFromImageFile(imageFile);
        System.out.println(text);
    }
}
