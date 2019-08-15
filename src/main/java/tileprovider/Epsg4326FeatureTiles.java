package tileprovider;

import android.graphics.Bitmap;
import com.j256.ormlite.dao.CloseableIterator;
import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackageException;
import mil.nga.geopackage.extension.index.GeometryIndex;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureResultSet;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.tiles.TileBoundingBoxUtils;
import mil.nga.geopackage.tiles.features.DefaultFeatureTiles;
import mil.nga.geopackage.tiles.features.FeatureTiles;
import mil.nga.sf.Point;
import mil.nga.sf.Polygon;
import mil.nga.sf.*;
import org.geotools.util.logging.Logging;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by joshua.johnson on 3/18/2019.
 * implementation using Android Graphics to draw tiles from Well Known Binary Geometries
 * based largely upon DefaultFeatureTiles, but with geometry corrections for EPSG 4326 projections
 * <p>
 * Note that FeatureData is not yet fully supported
 *
 * @see DefaultFeatureTiles
 */

class Epsg4326FeatureTiles extends FeatureTiles {

    static final Logger LOGGER = Logging.getLogger(Epsg4326FeatureTiles.class);

    //Constructor
    Epsg4326FeatureTiles(FeatureDao featureDao) {
        super(featureDao);

        //Largely for debugging -- If wrong projection is used, anomalous behavior will probably occur
        //TODO: either reject this incorrect projection or perform a conversion
        if (!featureDao.getProjection().getCode().equals("4326")) {
            LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Projection Mismatch. Expected 4326.  Found: " + featureDao.getProjection().getCode());
        }
    }

    /**
     * Draws tile based on index state
     *
     * @param x    - column
     * @param y    - row
     * @param zoom - zoom
     * @return Representative Bitmap with feature data
     */
    @Override
    public BufferedImage drawTile(int x, int y, int zoom) {
        //When this object is indexed, use the more efficient index drawing method
        if (isIndexQuery()) {
            return drawTileQueryIndex(x, y, zoom);
        } else {
            return drawTileQueryAll(x, y, zoom);
        }
    }

    /**
     * Draws based on query of all results.  Less efficient than drawTileQueryIndex().
     *
     * @param x    - column
     * @param y    - row
     * @param zoom - zoom level
     * @return Representative Bitmap with feature data
     */
    @Override
    public BufferedImage drawTileQueryAll(int x, int y, int zoom) {

        BufferedImage bufferedImage = null;

        // Query for all features
        FeatureResultSet resultSet = featureDao.queryForAll();

        try {
            int totalCount = resultSet.getCount();

            // Draw if at least one geometry exists
            if (totalCount > 0) {
                BoundingBox bbox = LayerUtility.XyzToBbox(x, y, zoom);
                if (maxFeaturesPerTile == null || totalCount <= maxFeaturesPerTile) {
                    // Draw the tile bitmap
                    bufferedImage = drawTile(zoom, bbox, resultSet);
                } else if (maxFeaturesTileDraw != null) {
                    // Draw the unindexed max features tile
                    bufferedImage = maxFeaturesTileDraw.drawUnindexedTile(tileWidth, tileHeight, totalCount, resultSet);
                } else {
                    //TODO: Based on the logic above, I don't think this code can ever run  (Remove??)
                    BoundingBox expandedBoundingBox = expandBoundingBox(bbox);
                    bufferedImage = new BufferedImage(x, y, zoom);

                    int count = 0;
                    while (resultSet.moveToNext()) {
                        FeatureRow row = resultSet.getRow();
                        if (drawFeature(zoom, bbox, expandedBoundingBox, bufferedImage, row)) {
                            count++;
                        }
                        if (count >= maxFeaturesPerTile) {
                            break;
                        }
                    }
                }
            }
        } finally {
            resultSet.close();
        }

        return bufferedImage;
    }

    /**
     * Query for feature results in the x, y, and zoom level by querying features in the tile location
     *
     * @return
     */
    @Override
    public CloseableIterator<GeometryIndex> queryIndexedFeatures(BoundingBox epsg4326BoundingBox) {
        // Create an expanded bounding box to handle features outside the tile
        // that overlap
        BoundingBox expandedQueryBoundingBox = expandBoundingBox(epsg4326BoundingBox);

        // Query for geometries matching the bounds in the index
        CloseableIterator<GeometryIndex> results = featureIndex.query(
                expandedQueryBoundingBox, WGS_84_PROJECTION);

        return results;
    }

    /*
     * METHODS NOT IMPLEMENTED
     */
    @Override
    public BufferedImage drawTile(int zoom, BoundingBox boundingBox, FeatureResultSet resultSet) {

        BoundingBox expandedBoundingBox = expandBoundingBox(boundingBox);
        LayerUtility.XYZ xyz = LayerUtility.bboxToXyz(boundingBox);

        // Create bitmap and canvas
        BufferedImage bufferedImage = new BufferedImage((int) xyz.x, (int) xyz.y, (int) xyz.z);
        Graphics2D graphics2D = (Graphics2D) bufferedImage.getGraphics();

        boolean drawn = false;
        for (FeatureRow featureRow : resultSet.getRow()) {
            if (drawFeature(zoom, boundingBox, expandedBoundingBox, bufferedImage, featureRow)) {
                drawn = true;
            }
        }

        graphics2D.dispose();

        return bufferedImage;

    }

    @Override
    public BufferedImage drawTile(int zoom, BoundingBox boundingBox, CloseableIterator<GeometryIndex> closeableIterator) {
        //When this object is indexed, use the more efficient index drawing method
        LayerUtility.XYZ xyz = LayerUtility.bboxToXyz(boundingBox);
        if (isIndexQuery()) {
            return drawTileQueryIndex((int) xyz.x, (int) xyz.y, (int) xyz.z);
        } else {
            return drawTileQueryAll((int) xyz.x, (int) xyz.y, (int) xyz.z);
        }
    }

    /**
     * Draw the feature on the canvas
     *
     * @param zoom                zoom level
     * @param boundingBox         bounding box
     * @param expandedBoundingBox expanded bounding box
     * @param bufferedImage       image to draw on
     * @param row                 feature row
     * @return true if at least one feature was drawn
     */
    private boolean drawFeature(int zoom, BoundingBox boundingBox, BoundingBox expandedBoundingBox, BufferedImage bufferedImage, FeatureRow row) {

        boolean drawn = false;

        try {
            GeoPackageGeometryData geomData = row.getGeometry();
            if (geomData != null) {
                Geometry geometry = geomData.getGeometry();
                if (geometry != null) {

                    GeometryEnvelope envelope = geomData.getOrBuildEnvelope();
                    BoundingBox geometryBoundingBox = new BoundingBox(envelope);
//                    BoundingBox transformedBoundingBox = geometryBoundingBox.transform(transform);

                    if (expandedBoundingBox.intersects(geometryBoundingBox, true)) {

                        double simplifyTolerance = TileBoundingBoxUtils.toleranceDistance(zoom, tileWidth, tileHeight);
                        drawShape(simplifyTolerance, boundingBox, bufferedImage, geometry);

                        drawn = true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, DefaultFeatureTiles.class.getSimpleName() + ": Failed to draw feature in tile. Table: "
                    + featureDao.getTableName(), e);
        }

        return drawn;
    }

    /**
     * Draw the geometry on the canvas
     *
     * @param simplifyTolerance simplify tolerance in meters
     * @param boundingBox
     * @param bufferedImage
     * @param geometry
     */
    private void drawShape(double simplifyTolerance, BoundingBox boundingBox, BufferedImage bufferedImage, Geometry geometry) {

        switch (geometry.getGeometryType()) {

            case POINT:
                Point point = (Point) geometry;
                drawPoint(boundingBox, bufferedImage, pointPaint.getColor(), point);
                break;
            case LINESTRING:
            case CIRCULARSTRING:
                LineString lineString = (LineString) geometry;
                Path2D linePath = new Path2D.Float();
                addLineString(simplifyTolerance, boundingBox, linePath, lineString);
                drawLinePath(bufferedImage, linePath);
                break;
            case POLYGON:
            case TRIANGLE:
                Polygon polygon = (Polygon) geometry;
                Path2D polygonPath = new Path2D.Float();
                addPolygon(simplifyTolerance, boundingBox, polygonPath, polygon);
                drawPolygonPath(bufferedImage, polygonPath);
                break;
            case MULTIPOINT:
                MultiPoint multiPoint = (MultiPoint) geometry;
                for (Point pointFromMulti : multiPoint.getPoints()) {
                    drawPoint(boundingBox, bufferedImage, pointPaint.getColor(), pointFromMulti);
                }
                break;
            case MULTILINESTRING:
                MultiLineString multiLineString = (MultiLineString) geometry;
                Path2D multiLinePath = new Path2D.Float();
                for (LineString lineStringFromMulti : multiLineString.getLineStrings()) {
                    addLineString(simplifyTolerance, boundingBox, multiLinePath, lineStringFromMulti);
                }
                drawLinePath(bufferedImage, multiLinePath);
                break;
            case MULTIPOLYGON:
                MultiPolygon multiPolygon = (MultiPolygon) geometry;
                Path2D multiPolygonPath = new Path2D.Float();
                for (Polygon polygonFromMulti : multiPolygon.getPolygons()) {
                    addPolygon(simplifyTolerance, boundingBox, multiPolygonPath, polygonFromMulti);
                }
                drawPolygonPath(bufferedImage, multiPolygonPath);
                break;
            case COMPOUNDCURVE:
                CompoundCurve compoundCurve = (CompoundCurve) geometry;
                Path2D compoundCurvePath = new Path2D.Float();
                for (LineString lineStringFromCompoundCurve : compoundCurve.getLineStrings()) {
                    addLineString(simplifyTolerance, boundingBox, compoundCurvePath, lineStringFromCompoundCurve);
                }
                drawLinePath(bufferedImage, compoundCurvePath);
                break;
            case POLYHEDRALSURFACE:
            case TIN:
                PolyhedralSurface polyhedralSurface = (PolyhedralSurface) geometry;
                Path2D polyhedralSurfacePath = new Path2D.Float();
                for (Polygon polygonFromPolyhedralSurface : polyhedralSurface.getPolygons()) {
                    addPolygon(simplifyTolerance, boundingBox, polyhedralSurfacePath, polygonFromPolyhedralSurface);
                }
                drawPolygonPath(bufferedImage, polyhedralSurfacePath);
                break;
            case GEOMETRYCOLLECTION:
                GeometryCollection<Geometry> geometryCollection = (GeometryCollection) geometry;
                List<Geometry> geometries = geometryCollection.getGeometries();
                for (Geometry geometryFromCollection : geometries) {
                    drawShape(simplifyTolerance, boundingBox, bufferedImage, geometryFromCollection);
                }
                break;
            default:
                throw new GeoPackageException("Unsupported Geometry Type: "
                        + geometry.getGeometryType().getName());
        }

    }

    /**
     * Draw the line path on the canvas
     *
     * @param bufferedImage
     * @param path
     */
    private void drawLinePath(BufferedImage bufferedImage, Path2D path) {
        Graphics2D graphics2D = (Graphics2D) bufferedImage.getGraphics();
        graphics2D.draw(path);
        graphics2D.dispose();
    }

    /**
     * Draw the path on the canvas
     *
     * @param bufferedImage
     * @param path
     */
    private void drawPolygonPath(BufferedImage bufferedImage, Path2D path) {
        Graphics2D graphics2D = (Graphics2D) bufferedImage.getGraphics();
        path.closePath();
        java.awt.Polygon polygon = (java.awt.Polygon) path.createTransformedShape(null);
        graphics2D.drawPolygon(polygon);
        if (fillPolygon) {
            java.awt.Paint paint = graphics2D.getPaint();
            Color color = polygonFillPaint.getColor();
            graphics2D.setPaint(color);
            graphics2D.fillPolygon(polygon);
            graphics2D.setPaint(paint);
        }
        graphics2D.dispose();
    }

    /**
     * Add the linestring to the path
     *
     * @param simplifyTolerance simplify tolerance in meters
     * @param boundingBox
     * @param path
     * @param lineString
     */
    private void addLineString(double simplifyTolerance, BoundingBox boundingBox, Path2D path, LineString lineString) {

        List<Point> points = lineString.getPoints();

        if (points.size() >= 2) {

            // Try to simplify the number of points in the LineString
            points = simplifyPoints(simplifyTolerance, points);

            for (int i = 0; i < points.size(); i++) {
                Point point = points.get(i);
                float x = TileBoundingBoxUtils.getXPixel(tileWidth, boundingBox,
                        point.getX());
                float y = TileBoundingBoxUtils.getYPixel(tileHeight, boundingBox,
                        point.getY());
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
        }
    }

    /**
     * Add the polygon on the canvas
     *
     * @param simplifyTolerance simplify tolerance in meters
     * @param boundingBox
     * @param path
     * @param polygon
     */
    private void addPolygon(double simplifyTolerance, BoundingBox boundingBox, Path2D path, Polygon polygon) {
        List<LineString> rings = polygon.getRings();

        if (!rings.isEmpty()) {

            // Add the polygon points
            LineString polygonLineString = rings.get(0);
            List<Point> polygonPoints = polygonLineString.getPoints();
            if (polygonPoints.size() >= 2) {
                addRing(simplifyTolerance, boundingBox, path, polygonPoints);

                // Add the holes
                for (int i = 1; i < rings.size(); i++) {
                    LineString holeLineString = rings.get(i);
                    List<Point> holePoints = holeLineString.getPoints();
                    if (holePoints.size() >= 2) {
                        addRing(simplifyTolerance, boundingBox, path, holePoints);
                    }
                }
            }
        }
    }

    /**
     * Add a ring
     *
     * @param simplifyTolerance simplify tolerance in meters
     * @param boundingBox
     * @param path
     * @param points
     */
    private void addRing(double simplifyTolerance, BoundingBox boundingBox, Path2D path, List<Point> points) {

        // Try to simplify the number of points in the LineString
        points = simplifyPoints(simplifyTolerance, points);

        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            float x = TileBoundingBoxUtils.getXPixel(tileWidth, boundingBox,
                    point.getX());
            float y = TileBoundingBoxUtils.getYPixel(tileHeight, boundingBox,
                    point.getY());
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.closePath();
    }

    /**
     * Draw the point on the canvas
     *
     * @param boundingBox
     * @param bufferedImage
     * @param color
     * @param point
     */
    private void drawPoint(BoundingBox boundingBox, BufferedImage bufferedImage, Color color, Point point) {

        float x = TileBoundingBoxUtils.getXPixel(tileWidth, boundingBox,
                point.getX());
        float y = TileBoundingBoxUtils.getYPixel(tileHeight, boundingBox,
                point.getY());
        Graphics2D graphics2D = (Graphics2D) bufferedImage.getGraphics();

        if (pointIcon != null) {
            if (x >= 0 - pointIcon.getWidth() && x <= tileWidth + pointIcon.getWidth() && y >= 0 - pointIcon.getHeight() && y <= tileHeight + pointIcon.getHeight()) {
                graphics2D.drawImage(pointIcon.getIcon(), Math.round(x - pointIcon.getXOffset()), Math.round(y - pointIcon.getYOffset()), color, null);
            }
        } else {
            if (x >= 0 - pointRadius && x <= tileWidth + pointRadius && y >= 0 - pointRadius && y <= tileHeight + pointRadius) {
                graphics2D.drawOval(Math.round(x), Math.round(y), Math.round(pointRadius), Math.round(pointRadius));
            }
        }
        graphics2D.dispose();
    }

    @Override
    public Bitmap drawTile(int zoom, BoundingBox boundingBox, FeatureCursor cursor) {
        Bitmap bitmap = createNewBitmap();
        Canvas canvas = new Canvas(bitmap);

        BoundingBox expandedBoundingBox = expandBoundingBox(boundingBox);

        boolean drawn = false;
        while (cursor.moveToNext()) {
            FeatureRow row = cursor.getRow();
            if (drawFeature(zoom, boundingBox, expandedBoundingBox, canvas, row)) {
                drawn = true;
            }
        }

        cursor.close();
        if (!drawn) {
            bitmap.recycle();
            bitmap = null;
        }

        return bitmap;
    }

    @Override
    public Bitmap drawTile(int zoom, BoundingBox boundingBox, List<FeatureRow> featureRow) {

        Bitmap bitmap = createNewBitmap();
        Canvas canvas = new Canvas(bitmap);

        BoundingBox expandedBoundingBox = expandBoundingBox(boundingBox);

        boolean drawn = false;
        for (FeatureRow row : featureRow) {
            if (drawFeature(zoom, boundingBox, expandedBoundingBox, canvas, row)) {
                drawn = true;
            }
        }

        if (!drawn) {
            bitmap.recycle();
            bitmap = null;
        }

        return bitmap;
    }
}
