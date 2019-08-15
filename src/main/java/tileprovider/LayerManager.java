package tileprovider;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import org.geotools.util.logging.Logging;
import tileprovider.wms.GetMapRequest;
import tileprovider.wms.WmsFactory;
import tileprovider.wms.WmsRequest;
import tileprovider.wms.WmsResponse;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageException;
import mil.nga.geopackage.manager.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageCoreImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Created by joshua.johnson on 3/18/2019.
 *
 * LayerManager manages a map of Layers linked associated to LayerNames.  LayerManager also provides
 * methods for Indexing feature layers.
 */

class LayerManager implements TileProvider {

    static final Logger LOGGER = Logging.getLogger(LayerManager.class);

    /**
     * Accessor to GeoPackage Library
     */
    private GeoPackageManager mGeoPackageManager;

    /**
     * File Import History - Used to determine if file should be imported again due to file changes
     */
    private ImportHistoryDb mFileHistoryDb;

    /**
     * Map of mLayerNames to Layers
     */
    private Map<String, Layer> mLayerMap;

    private Layer mDefaultLayer;

    private static final String DEFAULT_ASSET_FILE_PATH = "tileprovider_default/NOMS-BlueMarbleA.gpkg";
    private static final String DEFAULT_DATABASE_NAME = "DefaultLayer";
    private static final String DEFAULT_LAYERNAME = "BlueMarbleA";



    /**
     * Constructor
     * @param context (Non-null)
     */
    public LayerManager() {
        mGeoPackageManager = GeoPackageProvider.getInstance().getGeoPackageManager();
        mFileHistoryDb = ImportHistoryDb.getInstance(mContext);

        mLayerMap = new HashMap<>();
        try {
            InputStream defaultIn = mContext.getAssets().open(DEFAULT_ASSET_FILE_PATH);
            mGeoPackageManager.importGeoPackage(DEFAULT_DATABASE_NAME, defaultIn, true);

            //default layer (bluemarble)
            LayerInfo info = LayerInfo.create(
                    GetLayersProvider.getInstance(mContext),
                    DEFAULT_DATABASE_NAME,
                    DEFAULT_LAYERNAME,
                    LayerInfo.LayerType.TILE
            );
            mDefaultLayer = new TileBaseLayer(mContext, info);

        } catch (IOException | GeoPackageException ex) {
            ex.printStackTrace();
        }
    }


//    public int registerLayers(File geopackageDirOrFile) {
//        return registerLayers(geopackageDirOrFile, "");
//    }
    public void unregisterAll() {
        String[] layerNames = layerSet().toArray(new String[0]);
        for (String layerName : layerNames) {
            unregister(layerName);
        }
    }

    public void unregister(String layerName) {
        mLayerMap.remove(layerName);
    }


    /**
     * Recursively searches directory/sub-directories for all geopackage files
     * And/Or imports each layer into the geopackage manager
     * @param geopackageDirOrFile
     * @return
     */
    public int registerLayers(File geopackageDirOrFile) {
        //number of layers added
        int count = 0;

        //Recursive search -- Case Directory
        if (geopackageDirOrFile.isDirectory()) {
            File[] files = geopackageDirOrFile.listFiles();
            if (files != null) {
                for (File file : geopackageDirOrFile.listFiles()) {
                    count += registerLayers(file);
                }
            } else {
                Log.e(getClass().getSimpleName(), "Files not found: " + geopackageDirOrFile.getAbsolutePath() );
            }

            //Recursive search -- Case .gpkg named file
        } else if (geopackageDirOrFile.getName().toLowerCase().endsWith(".gpkg")) {
            //.gpkg File
            String databaseName = geopackageDirOrFile.getAbsolutePath();

            //check to see if database has already been loaded
            if (!databaseNamesSet().contains(databaseName)) {

                //If new or changed file (since last import) -- use lastModified metadata for signature
                //TODO: Use HASH
                if (mFileHistoryDb.getLastModified(geopackageDirOrFile.getAbsolutePath()) == null || mFileHistoryDb.getLastModified(geopackageDirOrFile.getAbsolutePath()) != geopackageDirOrFile.lastModified()) {


                    //New or Modified File -- so import first
                    //REMOVED -- This code imported code as Internal
//                try (InputStream in = new FileInputStream(geopackageDirOrFile)) {
//
//
//                    //Import geopackage with override option -- we base override decision with if statement above (if new/modified file)
//                    mGeoPackageManager.importGeoPackage(databaseName, in, true);
//                    mFileHistoryDb.insert(geopackageDirOrFile.getAbsolutePath(), geopackageDirOrFile.lastModified());
//                } catch (IOException | GeoPackageException ex) {
//                    ex.printStackTrace();
//                }
                    //Import code as external (mOverwrite enabled)
                    try {
                        mGeoPackageManager.importGeoPackageAsExternalLink(geopackageDirOrFile, databaseName, true);
                        mFileHistoryDb.insert(geopackageDirOrFile.getAbsolutePath(), geopackageDirOrFile.lastModified());
                    } catch (GeoPackageException ex) {
                        ex.printStackTrace();
                    }
                }

                //register the layers contained within the geopackage
                count = registerLayersFromGeoPackage(databaseName);

            }
        }

        return count;
    }


    /**
     * Loops through GeoPackage associated to databaseName in GeoPackageManager and registers a new
     * layer for each feature or tile table within that GeoPackage
     * @param databaseName - Associated database name to GeoPackage as registered in GeoPackageManager
     * @return number of layers created
     */
    private int registerLayersFromGeoPackage(String databaseName) {
        int count = 0;

        GetLayersProvider getLayersProvider = GetLayersProvider.getInstance(mContext);


        //open geopackage to read contents
        try (GeoPackage geoPackage = mGeoPackageManager.open(databaseName, false)) {
                  //do not close this geopackage.  Open geopackage stored in mLayerMap until needed

            if (geoPackage != null) {
                //Assign a layer for each Feature Table
                for (String tableName : geoPackage.getFeatureTables()) {
                    try {
                        LayerInfo info = LayerInfo.create(getLayersProvider, databaseName, tableName, LayerInfo.LayerType.FEATURE);

                        Layer layer = new FeatureLayer(mContext, info);
                        mLayerMap.put(info.getName(), layer);
                        count++;
                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    }
                }

                //Assign a layer for each Tile Table
                for (String tableName : geoPackage.getTileTables()) {
                    try {

                        LayerInfo info = LayerInfo.create(getLayersProvider, databaseName, tableName, LayerInfo.LayerType.TILE);

                        Layer layer = new TileLayer(mContext, info);
                        mLayerMap.put(info.getName(), layer);
                        count++;
                    } catch (IllegalArgumentException ex) {
                        ex.printStackTrace();
                    }
                }


            } else {
                Log.w(getClass().getSimpleName(), "GeoPackage " + databaseName + " returned null.");
            }
        } catch (GeoPackageException ex) {
            ex.printStackTrace();
        }

        return count;
    }


    /**
     * A Runnable that wraps indexing tasks for separate-threaded functionality.
     */
    class IndexLayersTask implements Runnable, IndexTask {

        private boolean mOverwrite;
        private Set<String> mLayerNames;
        private IndexProgressListener mIndexProgressListener;

        private AtomicBoolean mStopTask;
        private volatile Status mStatus;

        private Thread mWorkerThread;

        //Cross threaded variables
        private int mLayersCompleted;
        private int mLayersOutOf;
        private String mLayerName;

        /**
         * Constructor - Index all previously unindexed feature-layers registered to LayerManager
         */
        public IndexLayersTask(@NonNull IndexProgressListener indexProgressListener) {
            this(false, indexProgressListener);
        }

        /**
         * Constructor - Index all feature-layers registered to LayerManager with optional mOverwrite and progress Handler
         * @param overwrite - Should the indexer replace previously indexed layers?
         */
        IndexLayersTask(boolean overwrite, @NonNull IndexProgressListener indexProgressListener) {

            this(overwrite, featureLayerSet(), indexProgressListener);
        }

        /**
         * Constructor - Index specific feature-layers registered to LayerManager with optional mOverwrite and progress Handler
         * @param overwrite - Should the indexer replace previously indexed layers?
         * @param layerNames - List of Specific registered layers by LayerName to index
         * @param indexProgressListener - Callback to report progress
         */
        IndexLayersTask(boolean overwrite, Set<String> layerNames, @NonNull IndexProgressListener indexProgressListener) {
            this.mOverwrite = overwrite;
            this.mLayerNames = layerNames;
            this.mIndexProgressListener = indexProgressListener;
            this.mStopTask = new AtomicBoolean(false);
            mStatus = Status.WAITING;
        }

        /**
         * Main method which indexes each layer found in mLayerNames
         */
        @Override
        public void run() {

            mLayersCompleted = 0;
            mLayersOutOf = mLayerNames.size();

            for (String layerName : mLayerNames) {
                mLayerName = layerName;

                if (Thread.currentThread().isInterrupted() || mStopTask.get()) {
                    break;
                }

                indexFeatureLayer(mLayerName, mOverwrite, new Layer.LayerIndexProgressListener() {
                    @Override
                    public void onProgress(int progress, int outOf) {
                        mIndexProgressListener.onProgressUpdate(mLayerName, progress, outOf, mLayersCompleted, mLayersOutOf);
                    }

                    @Override
                    public void onTaskComplete(boolean isSuccessful) {

                    }
                });
                mLayersCompleted++;
            }

            if (Thread.currentThread().isInterrupted()) {
                mStatus = Status.INTERRUPTED;
            } else {
                mStatus = Status.FINISHED;
            }


        }

        public void start() {
            if (mStatus == Status.WAITING) {
                mStopTask.set(false);
                mStatus = Status.RUNNING;
                mWorkerThread = new Thread(this);
                mWorkerThread.start();
            } else{
                Log.d(getClass().getSimpleName(), "Start called on Task, but is already running or finished");
            }
        }

        public void stop(){
            mStopTask.set(true);
        }

        public Status getStatus() {
            return mStatus;
        }
    }

    /**
     * Indexes a specific feature layer.  Can take a significant amount of time. Consider running on a worker thread or use IndexLayersTask
     * @see IndexLayersTask
     * @param layerName layer to index
     * @param overwriteIndex - set true to overwrite past indexes, false to skip if index already exists
     * @param layerIndexProgressListener Callback for progress updates (updates per row processed)
     */
    private void indexFeatureLayer(String layerName, boolean overwriteIndex, Layer.LayerIndexProgressListener layerIndexProgressListener) {
        Layer layer = mLayerMap.get(layerName);
        if (layer != null) {
            layer.index(overwriteIndex, layerIndexProgressListener);
        }
    }

    /**
     * @return LayerNames currently registered to LayerManager
     */
    public Set<String> layerSet() {
        return mLayerMap.keySet();
    }

    public Set<String> tileLayerSet() {
        Set<String> layers = new LinkedHashSet<>(mLayerMap.size());

        for (String key : mLayerMap.keySet()) {
            if (mLayerMap.get(key).getType() == LayerInfo.LayerType.TILE) {
                layers.add(key);
            }
        }

        return layers;
    }

    /**
     * @return Feature LayerNames currently registered to LayerManager
     */
    public Set<String> featureLayerSet() {
        Set<String> layers = new LinkedHashSet<>(mLayerMap.size());

        for (String key : mLayerMap.keySet()) {
            if (mLayerMap.get(key).getType() == LayerInfo.LayerType.FEATURE) {
                layers.add(key);
            }
        }

        return layers;
    }

    /**
     * @return Non-indexed Feature LayerNames currently registered to LayerManager
     */
    public Set<String> nonIndexedFeatureLayerSet() {
        Set<String> layerNames = featureLayerSet();
        Set<String> results = new LinkedHashSet<>();

        for (String layerName : layerNames) {
            Layer layer = mLayerMap.get(layerName);
            if (layer != null && layer.getType() == LayerInfo.LayerType.FEATURE && layer instanceof FeatureLayer) {
                FeatureLayer featureLayer = (FeatureLayer) layer;

                if (!featureLayer.isIndexed()) {
                    results.add(layerName);
                }

            }
        }

        return results;
    }

    /**
     * Retrieves a layer
     * @param layerName
     * @return
     */
    public Layer getLayer(String layerName) {
        return mLayerMap.get(layerName);
    }


    public Layer getDefaultLayer() {
        return mDefaultLayer;
    }

    /**
     * Closes all GeoPackages associated to This LayerManager (but not all open GeoPackages)
     */
    public void closeLayers() {
        GeoPackageProvider geoPackageProvider = GeoPackageProvider.getInstance(mContext);
        geoPackageProvider.closeGeoPackages(layerSet());
    }

    /**
     * @return a set of databaseNames registered to LayerManager
     */
    private Set<String> databaseNamesSet() {
        Set<String> openDb = new LinkedHashSet<>();

        for(String key : mLayerMap.keySet()) {
            Layer layer = mLayerMap.get(key);
            openDb.add(layer.getDatabaseName());
        }
        return openDb;
    }

    @Override
    public String getLayersJSON() {
        GetLayersProvider getLayersProvider = GetLayersProvider.getInstance(mContext);
        return getLayersProvider.getJsonString(mLayerMap.values().toArray(new Layer[0]));
    }

    @Override
    public IndexTask index(IndexProgressListener indexProgressListener) {
        return index(false, indexProgressListener);
    }

    @Override
    public IndexTask index(boolean overwrite, IndexProgressListener indexProgressListener) {
        return index(
                featureLayerSet(),
                overwrite,
                indexProgressListener
        );
    }

    @Override
    public IndexTask index(Set<String> layerNames, boolean overwrite, IndexProgressListener indexProgressListener) {
        return new IndexLayersTask(overwrite, layerNames, indexProgressListener);
    }

    @Override
    public WmsResponse makeWmsRequest(Map<String, String> parameters) {
        WmsRequest request = WmsFactory.generateWmsRequest(this, parameters);
        WmsResponse response = null;

        if (request != null) {
            response = request.getResponse();
        }

        return response;
    }
}
