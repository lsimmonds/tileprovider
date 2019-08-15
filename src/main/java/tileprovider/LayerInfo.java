package tileprovider;

import mil.navy.tacmo.tileprovider.get_layers_object.*;

/**
 * Created by joshua.johnson on 4/8/2019.
 * Holder of layer information including referencing data (database and table name) as well as
 * layer info.
 */

class LayerInfo {

    enum LayerType {FEATURE, TILE}

    //Index info
    private String mDatabaseName;   //Geopackage database holding this layer
    private String mTableName;      //Table Name holding layer info
    private LayerType mType;        //Type of layer (feature or tile)

    //Group Name
    private String mGroupName;

    //Layer Info
    private String mName;        //NOMS lookup name
    private String mTitle;       //NOMS assigned title (set = to name in most cases)
    private boolean mSnappable = false;
    private String mThumbnail = null; //not required
    private int mZIndex = 5;        //Position of render

    //Constructor
    LayerInfo(String databaseName, String tableName, LayerType type, String groupName) {
        this(tableName, tableName, databaseName, tableName, type, 5, groupName);
    }

    LayerInfo(String name, String title, String databaseName, String tableName, LayerType type, int zIndex, String groupName) {
        mName = name;
        mTitle = title;
        mDatabaseName = databaseName;
        mTableName = tableName;
        mType = type;
        mZIndex = zIndex;
        mGroupName = groupName;
    }

    static LayerInfo create(GetLayersProvider getLayersProvider, String databaseName, String tableName, LayerType layerType) {
        Group getLayersObjectGroup = null;

        mil.navy.tacmo.tileprovider.get_layers_object.Layer getLayersObjectLayer =
                getLayersProvider.getGetLayersReference().getLayer(tableName);

        if (getLayersObjectLayer != null) {
            getLayersObjectGroup = getLayersProvider.getGetLayersReference().getGroupContaining(tableName);
        } else {
            getLayersObjectLayer = new mil.navy.tacmo.tileprovider.get_layers_object.Layer(tableName);
        }

        if (getLayersObjectGroup == null) {
            getLayersObjectGroup = new Group("Uncategorized");
        }

        return new LayerInfo(
                getLayersObjectLayer.getName(),
                getLayersObjectLayer.getTitle(),
                databaseName,
                tableName,
                layerType,
                getLayersObjectLayer.getzIndex(),
                getLayersObjectGroup.getName()
                );
    }

    //Getters/Setters
    public String getDatabaseName() {
        return mDatabaseName;
    }

    public void setDatabaseName(String mDatabaseName) {
        this.mDatabaseName = mDatabaseName;
    }

    public String getTableName() {
        return mTableName;
    }

    public void setTableName(String mTableName) {
        this.mTableName = mTableName;
    }

    public LayerType getType() {
        return mType;
    }

    public void setType(LayerType mType) {
        this.mType = mType;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public boolean isSnappable() {
        return mSnappable;
    }

    public void setSnappable(boolean mSnappable) {
        this.mSnappable = mSnappable;
    }

    public String getThumbnail() {
        return mThumbnail;
    }

    public void setThumbnail(String mThumbnail) {
        this.mThumbnail = mThumbnail;
    }

    public int getZIndex() {
        return mZIndex;
    }

    public void setZIndex(int mZIndex) {
        this.mZIndex = mZIndex;
    }

    public String getGroupName() {
        return mGroupName;
    }

    public void setGroup(String groupName) {
        mGroupName = groupName;
    }
}
