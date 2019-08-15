package tileprovider;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.tiles.TileBoundingBoxUtils;

/**
 * Created by joshua.johnson on 4/11/2019.
 * Utility functions related to translation
 */
class LayerUtility {

    /**
     * Private Constructor.  This class is purely utility and not instantiated.
     */
    private LayerUtility() {
        //Intentionally Blank
    }

    /**
     * Data holder for x (row), y (column), z (zoom)
     */
    static class XYZ {
        long x;
        long y;
        long z;

        XYZ(long x, long y, long z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }


    /***
     * Converts Bounding Box to Row Column Zoom coordinates in snapped tile grid assuming EPSG 4326 projection
     * @param bbox bounding box to convert (in EPSG:4326 projection)
     * @return snapped Row,Column,Zoom represeting this bounding box
     */
    static XYZ bboxToXyz(BoundingBox bbox) {
        int z = (int) Math.round(Math.log(180d/(bbox.getMaxLatitude() - bbox.getMinLatitude()))/ Math.log(2d));
        int x = (int) Math.round( ((bbox.getMinLongitude() + 180d)* Math.pow(2d,(double)z))/180d);
        int y = (int) Math.round( (( 90d - bbox.getMaxLatitude())* Math.pow(2d,(double)z))/180d);

        return new XYZ(x, y, z);
    }

    /***
     * Converts Row/Column/Zoom coordinates to Bounding Box assuming EPSG 4326 projection
     * @param x row
     * @param y column
     * @param z zoom
     * @return EPSG:4326 projection bounding box
     */
    static BoundingBox XyzToBbox(long x, long y, long z) {
        double dLat = 180d/ Math.pow(2d,(double)z);  //difference between latMax and latMin
        double lonMin = ((double)x)*180d/ Math.pow(2d,(double)z)-180d;
        double latMax = 90d - ((double)y)*180d/ Math.pow(2d,(double)z);

        BoundingBox bbox = new BoundingBox();
        bbox.setMinLatitude(latMax - dLat);
        bbox.setMaxLatitude(latMax);
        bbox.setMinLongitude(lonMin);
        bbox.setMaxLongitude(lonMin + dLat);

        return bbox;
    }

    /**
     * Generates a bounding box for a specific range of pixels on a tile
     * @param bbox - Bounding box of the Tile (EPSG:4326 projection)
     * @param width - width of tile
     * @param height - height of tile
     * @param i - x pixels from left edge of tile
     * @param j - y pixels from top edge of tile
     * @param pixelRadius - radius of pixels to extend bounding box from selected pixel
     * @return
     */
    static BoundingBox selectionArea(BoundingBox bbox, int width, int height, int i, int j, float pixelRadius) {
        double latitudeMin = TileBoundingBoxUtils.getLatitudeFromPixel(height, bbox, ((float)j)+ pixelRadius);
        double latitudeMax = TileBoundingBoxUtils.getLatitudeFromPixel(height, bbox, ((float)j)- pixelRadius);
        double longitudeMin = TileBoundingBoxUtils.getLongitudeFromPixel(width, bbox, ((float)i) - pixelRadius);
        double longitudeMax = TileBoundingBoxUtils.getLongitudeFromPixel(width, bbox, (float)i + pixelRadius);

        return new BoundingBox(longitudeMin, latitudeMin, longitudeMax, latitudeMax);
    }

    /**
     * Math to determine if boundingBox1 overlaps boundingBox2
     * Must be same projection
     * TODO: This won't work across PRIME MERIDIAN.  Fix...
     * @param boundingBox1
     * @param boundingBox2
     * @return true when overlapping, false when not
     */
    static boolean isOverlapping(BoundingBox boundingBox1, BoundingBox boundingBox2) {
        if (boundingBox1.getMinLongitude() > boundingBox2.getMaxLongitude() || boundingBox2.getMinLongitude() > boundingBox1.getMaxLongitude()) {
            return false;
        }

        if (boundingBox1.getMaxLatitude() < boundingBox2.getMinLatitude() || boundingBox2.getMaxLatitude() < boundingBox1.getMinLatitude()) {
            return false;
        }

        return true;
    }
}
