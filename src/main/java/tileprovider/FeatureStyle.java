package tileprovider;

import mil.nga.geopackage.tiles.features.FeatureTilePointIcon;

import java.awt.*;

/**
 * Created by joshua.johnson on 4/16/2019.
 * Style Holder which contains instructions on how a feature layer ought to be drawn.
 */

public class FeatureStyle {


    protected float pointRadius;
    protected Color pointColor = Color.BLACK;
    protected FeatureTilePointIcon pointIcon = null;
    protected Color lineColor = Color.BLACK;
    protected float lineStrokeWidth;
    protected Color polygonColor = Color.BLACK;
    protected boolean fillPolygon = false;
    protected Color polygonFillColor = Color.BLACK;
    protected float polygonStrokeWidth;

    public FeatureStyle() {
        pointRadius = 2f;

        lineStrokeWidth = 2f;

        polygonStrokeWidth = 2f;

        fillPolygon = false;
    }

    public float getPointRadius() {
        return pointRadius;
    }

    public void setPointRadius(float pointRadius) {
        this.pointRadius = pointRadius;
    }

    public Color getPointColor() {
        return pointColor;
    }

    public FeatureTilePointIcon getPointIcon() {
        return pointIcon;
    }

    public void setPointIcon(FeatureTilePointIcon pointIcon) {
        this.pointIcon = pointIcon;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }

    public float getLineStrokeWidth() {
        return lineStrokeWidth;
    }

    public void setLineStrokeWidth(float lineStrokeWidth) {
        this.lineStrokeWidth = lineStrokeWidth;
    }

    public Color getPolygonColor() {
        return polygonColor;
    }

    public void setPolygonColor(Color polygonColor) {
        this.polygonColor = polygonColor;
    }

    public boolean isFillPolygon() {
        return fillPolygon;
    }

    public void setFillPolygon(boolean fillPolygon) {
        this.fillPolygon = fillPolygon;
    }

    public Color getPolygonFillColor() {
        return polygonFillColor;
    }

    public void setPolygonFillColor(Color polygonFillColor) {
        this.polygonFillColor = polygonFillColor;
    }

    public float getPolygonFillStrokeWidth() {
        return polygonStrokeWidth;
    }

    public void setPolygonFillStrokeWidth(float polygonStrokeWidth) {
        this.polygonStrokeWidth = polygonStrokeWidth;
    }
}
