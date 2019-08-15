package tileprovider;

import android.content.Context;
import java.awt.image.VolatileImage;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.tiles.user.TileRow;

/**
 * Created by joshua.johnson on 4/26/2019.
 * This specific tile layer refers to the base layer that should always be drawn, no matter what
 */

class TileBaseLayer extends TileLayer {
    TileBaseLayer(@NonNull Context context, LayerInfo layerInfo) {
        super(context, layerInfo);
    }

    @Override
    protected VolatileImage getBestTile(TileDao tileDao, int x, int y, int z) {
        //x_i, y_i, and z_i change when iterating through the zoom levels
        int x_i = x;
        int y_i = y;
        int z_i = z;

        //initialize row with the requested zoom level
        TileRow row = tileDao.queryForTile(x_i, y_i, z_i);

        //if a row wasn't found keep searched to lower zoom levels until zoom level 0
        while (row == null && z_i > 0) {
            x_i = x_i/2;
            y_i = y_i/2;
            z_i = z_i -1;

            row = tileDao.queryForTile(x_i, y_i, z_i);

        }


        /**
         * Saved for potential future capability
         */
        //if below the best zoom, just get the best zoom
//        if (row == null && z > tileDao.getMaxZoom()){
//            z_i = (int)tileDao.getMaxZoom();
//            x_i = (int)(x/Math.pow(2,Math.abs(z - z_i)));
//            y_i = (int)(y/Math.pow(2,Math.abs(z - z_i)));
//
//            row = tileDao.queryForTile(x_i, y_i, z_i);
//        }



        Bitmap tileBitmap = null;
        //if a tile was found
        if (row != null) {
            tileBitmap = row.getTileDataBitmap();

            if (z_i != z) {
                //a matrix transformation must be applied to the tile if zoom level doesn't match the requested
                Matrix matrix = new Matrix();
                float nTiles = (float) Math.pow(2, z - z_i);    //number of divisions in this tile
                //TODO: This is assuming 256x256 height width -- we shouldn't assume these values
                matrix.setTranslate(-256 / nTiles * (x % nTiles), -256 / nTiles * (y % nTiles));
                matrix.postScale(nTiles, nTiles);   //zoom in on specified area

                Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(tileBitmap, matrix, null);    //apply transformation
                tileBitmap = bitmap;

            }
        }

        return tileBitmap;
    }
}
