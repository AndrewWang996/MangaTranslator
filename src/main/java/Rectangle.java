import com.google.cloud.vision.v1.BoundingPoly;
import com.google.cloud.vision.v1.BoundingPolyOrBuilder;
import com.google.cloud.vision.v1.Vertex;

import java.util.ArrayList;
import java.util.List;

public class Rectangle {
    public vertex UL, BR;
    public Rectangle(int xmin, int ymin, int xmax, int ymax) {
        UL = new vertex(xmin, ymin);
        BR = new vertex(xmax, ymax);
    }

    public Rectangle(vertex UL, vertex BR) {
        this.UL = UL;
        this.BR = BR;
    }

    public int height() {
        return BR.y - UL.y;
    }

    public int width() {
        return BR.x - UL.x;
    }

    public Rectangle copy() {
        return new Rectangle(UL.copy(), BR.copy());
    }

    public BoundingPoly toBoundingPoly() {
        int xmin, ymin, xmax, ymax;
        xmin = this.UL.x;
        ymin = this.UL.y;
        xmax = this.BR.x;
        ymax = this.BR.y;
        List<Vertex> vertices = new ArrayList<>();
        for (int x : new int[]{xmin, xmax}) {
            for (int y : new int[]{ymin, ymax}) {
                vertices.add( new vertex(x,y).toVertex() );
            }
        }
        return BoundingPoly.newBuilder()
                .addVertices(vertices.get(0))
                .addVertices(vertices.get(1))
                .addVertices(vertices.get(2))
                .addVertices(vertices.get(3))
                .build();
    }
}

class vertex {
    public int x, y;
    public vertex(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public vertex copy() {
        return new vertex(x, y);
    }

    public Vertex toVertex() {
        return Vertex.newBuilder().setX(x).setY(y).build();
    }
}
