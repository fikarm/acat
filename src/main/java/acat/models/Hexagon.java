package acat.models;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeType;

/**
 * A Symmetrical Hexagon Implementation
 */
public class Hexagon extends Polygon {

    private double radius;
    public double centerx, centery;

    public Hexagon(double radius, double centerx, double centery ) {

        this.radius = radius;
        this.centerx = centerx;
        this.centery = centery;
        this.setFill(Color.LIGHTGRAY);
        this.setStroke(Color.BLACK);
        this.setStrokeWidth(1);
        this.setStrokeType(StrokeType.INSIDE);

        drawHex();

    }

    private void drawHex() {

        int corners = 6;

        for( int corner = 0; corner < corners; corner++ ) {

            Point2D cornerPoint = pointyToppedHexCorner(corner);
            getPoints().addAll( cornerPoint.getX(), cornerPoint.getY() );

        }

    }

    /**
     * Calculate corner location from the center
     */
    private Point2D pointyToppedHexCorner(int corner ) {

        double  angle_deg = 60 * corner + 30,
                angle_rad = Math.toRadians(angle_deg),
                cos = centerx + radius * Math.cos( angle_rad ),
                sin = centery + radius * Math.sin( angle_rad );

        return new Point2D( cos, sin );

    }

    public void update( double radius ) {

        this.radius = radius;
        getPoints().clear();
        drawHex();

    }

}
