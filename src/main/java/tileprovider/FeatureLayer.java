package tileprovider;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.index.FeatureIndexResults;
import mil.nga.geopackage.features.index.FeatureIndexType;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.io.GeoPackageProgress;
import mil.nga.geopackage.tiles.features.DefaultFeatureTiles;
import mil.nga.geopackage.tiles.features.FeatureTiles;
import mil.nga.geopackage.user.ColumnValue;
import mil.nga.sf.GeometryEnvelope;
import mil.nga.sf.geojson.Feature;
import mil.nga.sf.geojson.FeatureConverter;
import org.geotools.util.logging.Logging;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by joshua.johnson on 3/18/2019.
 * Feature layer is a layer containing feature data.  Feature layers can be indexed, stylized, drawn, and can
 * also export data in GeoJSON form.
 *
 * Feature Layer is not yet fully supported
 */

class FeatureLayer extends Layer {

    static final Logger LOGGER = Logging.getLogger(FeatureLayer.class);
    private FeatureTilesHolder featureTilesHolder;      //Instance of the drawing tool for this layer
    private GeoPackageProgressImpl mGeoPackageProgress = null;  //instance of progress for Geopackage (for indexing)

    //Constructor
    FeatureLayer(LayerInfo layerInfo) {
        super(layerInfo);

        FeatureStyle featureStyle = new FeatureStyle();

        featureTilesHolder = new FeatureTilesHolder(featureStyle);
        FeatureTiles featureTiles = featureTilesHolder.getFeatureTiles();

        //PNG supports transparency for overlaying points on a map
        featureTiles.setCompressFormat(Bitmap.CompressFormat.PNG);

    }


    /**
     * Draws features found within a bounding box
     * @param bbox - extent
     * @param width - width of tile
     * @param height - height of tile
     * @return a drawn bitmap representing the requested data
     */
    public Bitmap draw(BoundingBox bbox, int width, int height) {

        LayerUtility.XYZ xyz = LayerUtility.bboxToXyz(bbox);

        FeatureTiles featureTiles = getFeatureTiles();

        //TODO: This paint color and style should be configurable
        Paint paint = featureTiles.getPointPaint();
        paint.setColor(Color.argb(255,200,255,200));

        featureTiles.setPointPaint(paint);

        return featureTiles.drawTile((int)xyz.x, (int)xyz.y, (int)xyz.z);
    }

    /**
     * Method is for returning information about underlying data at a specific point (GetFeatureInfo request)
     * @param bbox - Bounding box where the feature is to be found
     * @param width - Width of the tile
     * @param height - Height of the tile
     * @param i - selected x pixel coordinate from tile's left edge
     * @param j - selected y pixel coordinate from tile's top edge
     * @param pixelRadius - Radius of (square) area to search from selected x,y point
     * @return
     */
    public Collection<Feature> getGeoJsonFeatures(BoundingBox bbox, int width, int height, int i, int j, float pixelRadius) {
        Collection<Feature> features = new LinkedList<>();

        //generate the search extent based on selected position and search radius
        BoundingBox selectionBox = LayerUtility.selectionArea(bbox, width, height, i, j, pixelRadius);

        //get our DAO
        FeatureTiles featureTiles = getFeatureTiles();
        FeatureDao dao = featureTiles.getFeatureDao();

        //Do either an indexed search if available or a full search if not
        if (featureTiles.isIndexQuery()) {
            //indexed
            FeatureIndexResults results = featureTiles.queryIndexedFeatures(selectionBox);
            if (results.count() > 0) {
                for (FeatureRow row : results) {
                    Feature feature = rowToFeature(row, selectionBox);

                    if (feature != null) {
                        features.add(feature);
                    }
                }
            }
            results.close();

        } else {
            //not indexed
            FeatureCursor cursor = dao.queryForAll();
            while (cursor.moveToNext()) {
                Feature feature = rowToFeature(cursor.getRow(), selectionBox);
                if (feature != null) {
                    features.add(feature);
                }
            }


            cursor.close();
        }

        return features;
    }

    /**
     * method for converting a FeatureRow row (from an Feature Table query) to a GeoJSON Feature
     * Useful for interfacing feature data in Geopackage with GeoJSON inputs (such as FeatureInfo requests)
     * @param row - Row to convert
     * @param selectionBox - search extent?
     * @return A GeoJSON feature representation of the FeatureRow within selectionBox
     */
    private Feature rowToFeature(FeatureRow row, BoundingBox selectionBox) {
        Feature feature = null;

        //Build a bounding box based on the underlying geometry
        GeometryEnvelope envelope = row.getGeometry().getOrBuildEnvelope();
        BoundingBox geomBbox = new BoundingBox(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());

        //Check that the geometry is in our search parameters
        if (LayerUtility.isOverlapping(geomBbox, selectionBox)) {
            //It is!  Now extract and set our properties to our feature object
            feature = FeatureConverter.toFeature(row.getGeometry().getGeometry());
            Set<Map.Entry<String,ColumnValue>> propertiesSet = row.getAsMap();
            Map<String, Object> propertiesMap = new HashMap<>();
            for (Map.Entry<String,ColumnValue> entry : propertiesSet) {
                //ignore geometry column
                if (!entry.getKey().equalsIgnoreCase("geom")){
                    propertiesMap.put(entry.getKey(), entry.getValue().getValue());
                }
            }

            feature.setProperties(propertiesMap);
        }

        return feature;
    }


    //SETTER
    public void setFeatureStyle(FeatureStyle featureStyle) {
        featureTilesHolder.setStyle(featureStyle);
    }

    /**
     * Limits the number of features drawn to a tile
     * @param maxFeatures - No max features implemented when set to null
     */
    public void setMaxFeatures(@Nullable Integer maxFeatures) {
        getFeatureTiles().setMaxFeaturesPerTile(maxFeatures);
    }


    /**
     * Indexes the feature data for this layer
     * @param overwrite - Overwrites any previous index if true
     * @param progressListener - A listener for reporting index progress
     */
    public void index(boolean overwrite, LayerIndexProgressListener progressListener, String dbName) {
        //Open a new writable geopackage for indexing
    //            GeoPackage geoPackage = mGeoPackageManager.open(layer.getDatabaseName(), true);
        GeoPackage geoPackage = GeoPackageProvider.getInstance().getGeoPackage(dbName);

        if (geoPackage != null) {

            //cancel any currently running indexing tasks
            cancelIndex();

            final FeatureIndexManager manager = new FeatureIndexManager(geoPackage, getTableName());
            manager.setIndexLocation(FeatureIndexType.GEOPACKAGE);

            //Set a progress callback using Geopackage progress interface.  Set global so that cancel can be called.
            mGeoPackageProgress = new GeoPackageProgressImpl(progressListener, featureTilesHolder.getFeatureTiles().getFeatureDao().count());
            manager.setProgress(mGeoPackageProgress);

            manager.index(overwrite);

            featureTilesHolder.getFeatureTiles().setFeatureIndex(manager.getFeatureTableIndex());

        }

    }

    /**
     * @return is the underlying data indexed?
     */
    public boolean isIndexed() {
        return getFeatureTiles().isIndexQuery();
    }

    /**
     * cancel the indexing task (if it exists)
     */
    public void cancelIndex() {
        if (mGeoPackageProgress != null) {
            mGeoPackageProgress.cancelTask();
        }
    }

    /**
     * @return the feature drawing tool object
     */
    private FeatureTiles getFeatureTiles() {
        return featureTilesHolder.getFeatureTiles();
    }


    /**
     * This class holds an instance of FeatureTiles. If the underlying geopackage is closed or FeatureTiles
     * no longer works, then a new connection is established
     */
    private class FeatureTilesHolder {
        private FeatureTiles featureTiles = null;
        private GeoPackageProvider geoPackageProvider;
        private FeatureStyle featureStyle;

        //Constructor
        public FeatureTilesHolder(FeatureStyle featureStyle) {
            geoPackageProvider = GeoPackageProvider.getInstance();
            _init();
            setStyle(featureStyle);
        }

        //GETTER -- If the connection is closed, then reopen it.
        public FeatureTiles getFeatureTiles() {

            //Create a new featureTiles if the geopackage connection is closed
            try {
                if (featureTiles == null ||
                        featureTiles.getFeatureDao().getConnection().isClosed())
                {
                    _init();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return featureTiles;
        }

        //GETTER/SETTER
        public FeatureStyle getStyle() {
            return featureStyle;
        }

        public void setStyle() {
            setStyle(featureStyle);
        }

        public void setStyle(FeatureStyle featureStyle) {
            this.featureStyle = featureStyle;

            featureTiles.setPointRadius(featureStyle.getPointRadius());
            featureTiles.setPointColor(featureStyle.getPointColor());
            featureTiles.setPointIcon(featureStyle.getPointIcon());
            featureTiles.setLineColor(featureStyle.getLineColor());
            featureTiles.setLineStrokeWidth(featureStyle.getLineStrokeWidth());
            featureTiles.setPolygonColor(featureStyle.getPolygonColor());
            featureTiles.setPolygonFillColor(featureStyle.getPolygonFillColor());
            featureTiles.setPolygonStrokeWidth(featureStyle.getPolygonFillStrokeWidth());
            featureTiles.setFillPolygon(featureStyle.isFillPolygon());

            featureTiles.calculateDrawOverlap();
        }

        //Initializer
        private void _init() {

            //Set up DAO
            GeoPackage geoPackage = geoPackageProvider.getGeoPackage(getDatabaseName());
            FeatureDao featureDao = geoPackage.getFeatureDao(getTableName());

            //Establish which FeatureTile implementation to use (Factory)
            switch (featureDao.getProjection().getCode()) {
                case "4326":
                    featureTiles = new Epsg4326FeatureTiles(getContext(), featureDao);
                    break;
                default:
                    featureTiles = new DefaultFeatureTiles(getContext(), featureDao);
                    break;
            }

            //Look for an index to attach (if one exists)
            FeatureIndexManager featureIndexManager = new FeatureIndexManager(geoPackage, getTableName());
            featureTiles.setFeatureIndex(featureIndexManager.getFeatureTableIndex());
        }
    }

    private class GeoPackageProgressImpl implements GeoPackageProgress {
        private int mMax;
        private boolean mActive;
        private LayerIndexProgressListener mProgressListener;

        private GeoPackageProgressImpl(@NotNull LayerIndexProgressListener progressListener) {
            this(progressListener, -1); //-1 to indicate not yet set
        }

        private GeoPackageProgressImpl(@NotNull LayerIndexProgressListener progressListener, int max) {
            if (progressListener == null) {
                throw new IllegalArgumentException("Progress Listener cannot be null");
            }

            mProgressListener = progressListener;
            mMax = max;
            mActive = true;
        }

        @Override
        public void setMax(int max) {
            Log.d(getClass().getSimpleName(), "SetMax() was called");
            //Max should be greater than 0
            if (max > 0) {
                mMax = max;
            }
        }

        @Override
        public void addProgress(int progress) {
            mProgressListener.onProgress(progress, mMax);
        }

        //False will cancel the linked task
        @Override
        public boolean isActive() {
            return mActive;
        }

        @Override
        public boolean cleanupOnCancel() {
            Log.d(getClass().getSimpleName(), "CleanupOnCancel() was called");
            return true;
        }

        public void cancelTask() {
            mActive = false;
        }
    }




}
