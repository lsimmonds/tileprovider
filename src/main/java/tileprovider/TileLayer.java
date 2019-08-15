package tileprovider;

import android.content.Context;

import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.util.Log;
import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.tiles.user.TileRow;
import mil.nga.sf.geojson.Feature;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by joshua.johnson on 3/18/2019.
 * Tile layers contain pre-rasterized data which can be drawn.
 */

class TileLayer extends Layer {

    /**
     * Doa
     */
    private TileDaoHolder tileDaoHolder;

    /**
     * Constructor
     * @param layerInfo - Associated Layer Info
     */
    TileLayer(LayerInfo layerInfo) {
        super(layerInfo);



        tileDaoHolder = new TileDaoHolder();
    }

    /**
     * Draw the layer
     * @param bbox - BoundingBox to Draw -- rounds to nearest Tile
     * @param width - Width of Tile (pixels)
     * @param height - Height of Tile (pixels)
     * @return A rendered layer representing feature data for bounding box or null if not found
     */
    @Override
    public BufferedImage draw(BoundingBox bbox, int width, int height) {
        BufferedImage bufferedImage = null;

        LayerUtility.XYZ xyz = LayerUtility.bboxToXyz(bbox);

        Log.d("drawing", "bbox: " +
                "MinLon="+bbox.getMinLongitude() + ", " +
                "MinLat="+bbox.getMinLatitude() + ", " +
                "MaxLon="+bbox.getMaxLongitude() + ", " +
                "MaxLat="+bbox.getMaxLatitude() + "; " +
                "X="+xyz.x + ", " +
                "Y="+xyz.y + ", " +
                "Z="+xyz.z + ", "
        );

//        TileRow row = getTileDao().queryForTile(xyz.x, xyz.y, xyz.z);
//        if (row!= null) {
//            volatileImage = row.getTileDataBitmap();
//        }

        bufferedImage = getBestTile(getTileDao(), (int)xyz.x, (int)xyz.y, (int)xyz.z);


        return bufferedImage;
    }

    /**
     * Returns GeoJSON data representing features held in this layer at point i, j
     * @param bbox bounding box (in Latitude -90 to 90 and Longitude -180 to 180 snapped to WGS-84 tile Projection
     * @param width - Pixel Width
     * @param height - Pixel Height
     * @param i - ith pixel of tile (horizontal)
     * @param j - jth pixel of tile (vertical)
     * @param pixelRadius - radius of area around pixel (i, j) to search for data
     * @return
     */
    @Override
    public Collection<Feature> getGeoJsonFeatures(BoundingBox bbox, int width, int height, int i, int j, float pixelRadius) {
        return null;
    }

    /**
     * This method attempts to grab the best tile available, even if the zoom level is less than
     * the requested zoom level.  If the zoom level is less, a transformation is applied to the image
     * to match the given tile space
     * @param tileDao - the tile database to operate on
     * @param x - column
     * @param y - row
     * @param z - zoom
     * @return a bitmap or null if no tile could be drawn
     */


    protected BufferedImage getBestTile(TileDao tileDao, int x, int y, int z) {

        //x_i, y_i, and z_i change when iterating through the zoom levels
        int x_i = x;
        int y_i = y;
        int z_i = z;

        //initialize row with the requested zoom level
        TileRow row = tileDao.queryForTile(x_i, y_i, z_i);

        //if a row wasn't found keep searched to lower zoom levels until zoom level 0
//        while (row == null && z_i > 0) {
//            x_i = x_i/2;
//            y_i = y_i/2;
//            z_i = z_i -1;
//
//            row = tileDao.queryForTile(x_i, y_i, z_i);
//        }
        //if below the best zoom, just get the best zoom

        if (row == null && z > tileDao.getMaxZoom()){
            z_i = (int)tileDao.getMaxZoom();
            int z_d = Math.abs(z - z_i);

            x_i = (int)(x/ Math.pow(2,z_d));
            y_i = (int)(y/ Math.pow(2,z_d));

            row = tileDao.queryForTile(x_i, y_i, z_i);
        }



        BufferedImage tileBufferedImage = null;
        //if a tile was found
        if (row != null) {
            try {
                tileBufferedImage = row.getTileDataImage();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (z_i != z) {
                //a matrix transformation must be applied to the tile if zoom level doesn't match the requested
                Matrix matrix = new Matrix();
                float nTiles = (float) Math.pow(2, z - z_i);    //number of divisions in this tile
                matrix.setTranslate(-256 / nTiles * (x % nTiles), -256 / nTiles * (y % nTiles));
                matrix.postScale(nTiles, nTiles);   //zoom in on specified area

                Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
                BufferedImage resized = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB)
                Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(tileBufferedImage, matrix, null);    //apply transformation
                tileBufferedImage = bitmap;
            }
        }

        return tileBufferedImage;
    }


    /**
     * Indexes layer
     * @param overwrite true to overwrite any preexisting index associated to this layer.
     * @return - TileLayer cannot be indexed.
     */
    @Override
    public void index(boolean overwrite, LayerIndexProgressListener layerIndexProgressListener) {
        //do nothing -- cannot be indexed
    }

    /**
     * @return an open Dao
     */
    private TileDao getTileDao() {
        return tileDaoHolder.getTileDao();
    }

    /**
     * Class provides a Database Access Object (DAO) based upon an open GeoPackage.  The GeoPackage is checked
     * for opened state before providing the DAO.
     */
    private class TileDaoHolder {
        private GeoPackageProvider geoPackageProvider;
        private TileDao tileDao;

        TileDaoHolder(Context context) {
            geoPackageProvider = GeoPackageProvider.getInstance(context);
        }

        public TileDao getTileDao() {
            if (tileDao == null ||
                    !tileDao.getDatabaseConnection().getDb().isOpen()) {
                GeoPackage geoPackage = geoPackageProvider.getGeoPackage(getDatabaseName());
                tileDao = geoPackage.getTileDao(getTableName());
            }

            return tileDao;
        }
    }
}
