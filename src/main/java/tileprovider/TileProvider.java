package tileprovider;

import tileprovider.wms.WmsResponse;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Created by Joshua.Johnson on 5/6/2019.
 */

public interface TileProvider {
    /**
     * Registers layers within a geopackage or directory of GeoPackages. Recursively searches
     * any directory given for all contained GeoPackage files.
     * @param fileOrDirectory - geopackage file or directory of GeoPackages
     * @return number of layers imported - This may be different than number of GeoPackage files found
     */
    int registerLayers(File fileOrDirectory);

    /**
     * Closes all associated GeoPackages.  GeoPackages are reopened when WMS calls are made to this interface
     * or draw methods are called on the underlying Layer objects.  This should be called whenever the application
     * is done using this tile_provider object.
     */
    void closeLayers();

    /**
     * Indexes the associated unindexed feature layers on a separate worker thread.  Progress is posted to given listener.
     * Task begins automatically
     * @return - IndexTask which can be stopped
     */
    IndexTask index(IndexProgressListener indexProgressListener);


    /**
     * Indexes the associated feature layers on a separate worker thread.  Progress is posted to given listener.
     * Task begins automatically
     * @return - IndexTask which can be stopped
     */
    IndexTask index(boolean overwrite, IndexProgressListener indexProgressListener);

    /**
     * Indexes the associated feature layers on a separate worker thread.  Progress is posted to given listener.
     * Task begins automatically
     * @return - IndexTask which can be stopped
     */
    IndexTask index(Set<String> layerNames, boolean overwrite, IndexProgressListener indexProgressListener);

    /**
     * @return - a GeoJSON representation of all registered layers
     */
    String getLayersJSON();

    /**
     * functions based on request parameters using WMS standards
     * @param parameters - WMS related parameters
     * @return null if request failed, Response object if request successful
     */
    WmsResponse makeWmsRequest(Map<String, String> parameters);

    /**
     * Returns the layer with the given name
     * @param layerName
     * @return
     */
    Layer getLayer(String layerName);

    /**
     * Returns the default (base) layer
     * @return
     */
    Layer getDefaultLayer();

    /**
        Returns a list of layer names registered to this Tile Provider instance
     **/
    Set<String> layerSet();

    /**
     * @return Returns a list of tile layer names registered to this Tile Provider instance
     */
    Set<String> tileLayerSet();

    /**
     * @return Returns a list of feature layer names registered to this Tile Provider instance
     */
    Set<String> featureLayerSet();

    /**
     * @return Returns a list of non-indexed feature layer names registered to this Tile Provider Instance
     */
    Set<String> nonIndexedFeatureLayerSet();

    /**
     * Unregisters all layers currently registered to this Tile Provider Instance
     * TODO: Layer should be closed as well when unregistered
     */
    void unregisterAll();

    /**
     * Unregisters specific layer currently registered to this Tile Provider Instance
     * TODO: LAyer should be closed when unregistered
     * @param layerName
     */
    void unregister(String layerName);
}
