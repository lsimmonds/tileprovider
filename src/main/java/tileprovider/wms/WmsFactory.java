package tileprovider.wms;

import org.geotools.util.logging.Logging;
import tileprovider.TileProvider;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by joshua.johnson on 4/23/2019.
 */

public class WmsFactory {

    static final String PARAM_REQUEST_KEY = "REQUEST";
    static final String PARAM_REQUEST_VAL_GETMAP = "getmap";
    static final String PARAM_REQUEST_VAL_GET_FEATURE_INFO = "getfeatureinfo";
    static final Logger LOGGER = Logging.getLogger(GetMapRequest.class);

    //Private constructor to prevent instantiation
    private WmsFactory() {
        //intentionally blank
    }


    public static WmsRequest generateWmsRequest(TileProvider tile_provider, Map<String, String> decodedParameters) {
        //Must contain a request
        if (!decodedParameters.containsKey(PARAM_REQUEST_KEY)) {
            LOGGER.log(Level.WARNING, WmsFactory.class.getSimpleName() + ": Request Key not present");
            return null;
        }

        WmsRequest wmsRequest = null;

        final String requestValue = decodedParameters.get(PARAM_REQUEST_KEY).toLowerCase();
        switch (requestValue) {
            case PARAM_REQUEST_VAL_GETMAP:
                wmsRequest = new GetMapRequest(tile_provider, decodedParameters);
                break;
            case PARAM_REQUEST_VAL_GET_FEATURE_INFO:
                wmsRequest = new GetFeatureRequest(tile_provider, decodedParameters);
                break;
            default:
                //unknown request type
                LOGGER.log(Level.WARNING, WmsFactory.class.getSimpleName() + ": Unknown request type: " + requestValue);
        }

        return wmsRequest;

    }


//    private static final String PARAMETER_REGEX = "(\\?|\\&)([^=]+)\\=([^&#]+)";
//    private static final Pattern PARAMETER_PATTERN = Pattern.compile(PARAMETER_REGEX);
//
//    private static Map<String, String> decodeParameters(String url) {
//
//        String decodedUrl = "";
//
//        //Decode the URL
//        try {
//            decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.displayName());
//        } catch (UnsupportedEncodingException ex) {
//            ex.printStackTrace();   //This should never happen
//        }
//
//        Map <String, String> parameters = new HashMap<>();
//
//        //Perform regular expression search
//        Matcher matcher = PARAMETER_PATTERN.matcher(decodedUrl);
//
//        //For each parameter found
//        while (matcher.find()) {
//            parameters.put(matcher.group(2), matcher.group(3));
//        }
//
//        return parameters;
//    }
}
