package tileprovider;

import mil.nga.geopackage.BoundingBox;
import mil.nga.sf.geojson.Feature;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;

/**
 * Created by joshua.johnson on 4/10/2019.
 * Parent class for Layer.  Layer represents a specific table within Geopackage with geo data
 * Subclasses include FeatureLayer and TileBaseLayer
 */

public abstract class Layer {

    private LayerInfo layerInfo;    //Data holder for layer information

    protected Layer(LayerInfo layerInfo) {
        this.layerInfo = layerInfo;
    }

    /**
     * Draw a bitmap containing layer information with the given bounding box, width, and height
     * @param bbox - Bounding box of the extent which to draw
     * @param width - pixel width of bitmap to draw
     * @param height - pixel height of bitmap to draw
     */
    public abstract BufferedImage draw(BoundingBox bbox, int width, int height);

    /**
     * Index given layer data
     * @param overwrite - overwrite previous index
     * @param layerIndexProgressListener - Optional listener to report progress updates
     */
    public abstract void index(boolean overwrite, LayerIndexProgressListener layerIndexProgressListener);

    /**
     * @return the self-appointed type of layer this layer is
     * //TODO: This should probably be defined elsewhere since we don't always want to be modifying the enum
     */
    LayerInfo.LayerType getType() {
        return layerInfo.getType();
    }

    /**
     * @return Layer name from Layer Info data holder
     */
    String getLayerName() {
        return layerInfo.getName();
    }

    /**
     * @return Table name from LayerInfo data holder
     */
    String getTableName() {
        return layerInfo.getTableName();
    }

    /**
     * @return Database name from LayerInfo data holder
     */
    String getDatabaseName() {
        return layerInfo.getDatabaseName();
    }

    /**
     * @return The layer info data holder
     */
    LayerInfo getLayerInfo() {
        return layerInfo;
    }

    /**
     * abstract method for retrieving feature data from specific pixel
     * used for featureInfo requests
     * @param bbox - Bounding box to search
     * @param width - pixel width of tile
     * @param height - pixel height of tile
     * @param i - x pixel coordinate from left edge
     * @param j - y pixel coordinate from top edge
     * @param pixelRadius - radius in which to search
     * @return null if no features are found
     */
    public abstract Collection<Feature> getGeoJsonFeatures(BoundingBox bbox, int width, int height, int i, int j, float pixelRadius);

    /**
     * Interface for progress updates while indexing
     * TODO: This interface already exists outside this class.  Remove this nested listener?
     */
    interface LayerIndexProgressListener {
        void onProgress(int progress, int outOf);
        void onTaskComplete(boolean isSuccessful);
    }


}
