package tileprovider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import tileprovider.get_layers_object.GetLayersObject;
import tileprovider.get_layers_object.Group;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;


/**
 * Created by joshua.johnson on 4/23/2019.
 * Used to generate a representative JSON of the currently loaded layers to a GetLayers request
 */

class GetLayersProvider {

    static final Logger LOGGER = Logging.getLogger(GetLayersProvider.class);

    //Singleton Accessor
    private static GetLayersProvider mInstance = null;


    public static GetLayersProvider getInstance() {
        if (mInstance == null) {
            mInstance = new GetLayersProvider();
        }

        return mInstance;
    }

    //Member Variables
    /**
     * Constants
     */
    private static final String GET_LAYERS_PATH = "noms/getLayers.json";
    private static final String GET_LAYERS_ASSET_PATH = "layers.json";
    private static final String DEFAULT_URL = "URL";

    /**
     * Application Constant
     */
    private Context mContext;

    /**
     * Loaded parsed layers reference for getLayers
     */
    private GetLayersObject mGetLayersReference;



//    private GetLayersObject mGetLayersObject;

    /**
     * Constructor
     * @param context - non-null context
     */
    private GetLayersProvider(Context context) {
        mContext = context;
        mGetLayersReference = _initLayersReference(mContext);
//        mGetLayersObject = new GetLayersObject();
    }

    /**
     * Constructs a JSON string representing layers registered to given LayerManager object
     * @param layerManager
     * @return
     */
    public String getJsonString(LayerManager layerManager) {
        Set<String> layerNames = layerManager.layerSet();
        List<Layer> layers = new LinkedList<>();
        for (String layerName : layerNames) {
            layers.add(layerManager.getLayer(layerName));
        }

        return getJsonString(layers.toArray(new Layer[0]));
    }



    /**
     * Constructs a JSON string representing the given list of layers
     * @param layers
     * @return
     */
    public String getJsonString(Layer[] layers) {
        GetLayersObject getLayersObject = new GetLayersObject();

        for (Layer layer : layers) {
            //For each layer, copy info to json data model
            LayerInfo layerInfo = layer.getLayerInfo();

            //construct layer data model based upon the passed in layer
            tileprovider.get_layers_object.Layer getLayersObjectLayer =
                    new tileprovider.get_layers_object.Layer(
                            layerInfo.getName(),
                            layerInfo.getTitle(),
                            layerInfo.isSnappable(),
                            layerInfo.getThumbnail(),
                            layerInfo.getZIndex()
                    );

            //See if the group already exists
            Group group = getLayersObject.getLayerGroups().get(layerInfo.getGroupName());

            if (group == null) {
                //Group was not yet added.  We need to add one.
                Group referenceGroup = mGetLayersReference.getLayerGroups().get(layerInfo.getGroupName());

                if (referenceGroup != null) {
                    //Copy the reference group (from layers.json)
                    group = new Group(referenceGroup.getName(), referenceGroup.getMasterName());
                } else {
                    //No reference group... simply make a new group with default values
                    group = new Group(layerInfo.getGroupName());
                }

                //There must be a URL
                group.addUrl(DEFAULT_URL);

                //Add the new group
                getLayersObject.addGroup(group);
            }


            //add our layer
            getLayersObject.addLayer(layerInfo.getGroupName(), getLayersObjectLayer);
        }

        ObjectMapper mapper = new ObjectMapper();
        String result = null;
        try {
            result = mapper.writeValueAsString(getLayersObject);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
        }

        return result;
    }

    public GetLayersObject getGetLayersReference() {
        return mGetLayersReference;
    }

    /**
     * Provides parsed reference object using the file path first, or the asset reference second
     * @param context - Application context
     * @return A parsed or empty GetLayersObject
     */
    private GetLayersObject _initLayersReference(Context context) {
        //GetLayers file path
        File file = new File(GET_LAYERS_PATH);

        //Mapper to map json to java object
        ObjectMapper mapper = new ObjectMapper();

        //Resulting GetLayersObject after mapping
        GetLayersObject result = null;

        //First - Attempt to read file from path
        try (InputStream in = new FileInputStream(file)){
            result = mapper.readValue(in, GetLayersObject.class);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //Second -- if an error occurred -(file not found, permission-issue, etc)- try reading from assets
        if (result == null) {
            LOGGER.log(Level.INFO,getClass().getSimpleName()+": GetLayers JSON file error.  Using backup file from assets.");
            try (InputStream in = context.getAssets().open(GET_LAYERS_ASSET_PATH)) {
                result = mapper.readValue(in, GetLayersObject.class);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        //Third -- Asset file error.  Use an Empty Object
        if (result == null) {
            LOGGER.log(Level.WARNING,getClass().getSimpleName()+": Could not load GetLayers JSON file from assets");
            result = new GetLayersObject();
        }

        return result;

    }



}
