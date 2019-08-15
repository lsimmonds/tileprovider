package tileprovider.wms;

import tileprovider.TileProvider;

import java.util.Map;

/**
 * Created by joshua.johnson on 4/23/2019.
 */

class GetFeatureRequest implements WmsRequest {

    GetFeatureRequest(TileProvider tile_provider, Map<String, String> parameters) {

    }

    @Override
    public WmsResponse getResponse() {
        return new WmsResponse("Feature Info is currently unsupported".getBytes(), "text/plain");
    }
}
