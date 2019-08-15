package tileprovider.wms;

import mil.nga.geopackage.BoundingBox;
import org.geotools.util.logging.Logging;
import tileprovider.Layer;
import tileprovider.TileProvider;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by joshua.johnson on 4/23/2019.
 */

class GetMapRequest implements WmsRequest {

    static final Logger LOGGER = Logging.getLogger(GetMapRequest.class);
    private TileProvider mtileProvider;
    private Map<String, String> mParameters;

    GetMapRequest(TileProvider tileProvider, Map<String, String> parameters) {
        mtileProvider = tileProvider;
        mParameters = parameters;
    }

    @Override
    public WmsResponse getResponse() {
        String layersString = mParameters.get("LAYERS");

        //extract each layer name
        String[] layerNames;
        if (layersString != null) {
            layerNames = layersString.split(",");
        } else {
            //no layers provided?
            layerNames = new String[0];
            LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": No layers parameter found");
        }

        //extract width and height
        int width;
        int height;
        try {
            if (mParameters.containsKey("HEIGHT") && mParameters.containsKey("WIDTH")) {
                width = Integer.parseInt(mParameters.get("WIDTH"));
                height = Integer.parseInt(mParameters.get("HEIGHT"));
            } else {
                throw new IllegalArgumentException("Missing parameter width or height");
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            width = 256;
            height = 256;
        }

        //extract bounding box
        //TODO VERSION DEPENDANT -- ASSUME 1.3.0
        BoundingBox bbox = new BoundingBox();
        if (!mParameters.containsKey("BBOX")) {
            LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Bounding box not present");
            return new WmsResponse("Parameter bbox not found".getBytes(), "text/plain");
        }
        String bboxString = mParameters.get("BBOX");
        String[] bboxParams = bboxString.split(",");
        if (bboxParams.length != 4) {
            LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Bounding box malformed?");
            return new WmsResponse("Bounding box malformed".getBytes(), "text/plain");
        }
        try {
            bbox.setMinLongitude(Double.parseDouble(bboxParams[1]));
            bbox.setMinLatitude(Double.parseDouble(bboxParams[0]));
            bbox.setMaxLongitude(Double.parseDouble(bboxParams[3]));
            bbox.setMaxLatitude(Double.parseDouble(bboxParams[2]));
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not parse Bounding Box");
            return new WmsResponse("Bounding box malformed".getBytes(), "text/plain");
        }

//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bufferedImage.createGraphics();
//        Canvas canvas = new Canvas(bitmap);

        //default layer
        {
            Layer layer = mtileProvider.getDefaultLayer();
//            Bitmap layerBitmap = layer.draw(bbox, width, height);
            BufferedImage layerBufferedImage = layer.draw(bbox, width, height);
//            if (layerBitmap != null) {
//                canvas.drawBitmap(layerBitmap, 0, 0, null);
//            }
            if (layerBufferedImage != null) {
                graphics.drawImage(layerBufferedImage, null, 0, 0);
            }
        }

        for (String layerName : layerNames) {

            int index = layerName.indexOf(':');
            if (index != -1 && index != layerName.length()) {
                layerName = layerName.substring(index + 1);
            }

            Layer layer = mtileProvider.getLayer(layerName);
            if (layer != null) {
//                Bitmap layerBitmap = layer.draw(bbox, width, height);
                BufferedImage layerBufferedImage = layer.draw(bbox, width, height);
//                if (layerBitmap != null) {
//                    canvas.drawBitmap(layerBitmap, 0, 0, null);
//                }
                if (layerBufferedImage != null) {
                    graphics.drawImage(layerBufferedImage, null, 0, 0);
                }

            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        try {
            ImageIO.write(bufferedImage, "jpeg", out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new WmsResponse(out, "image/jpeg");
    }
}
