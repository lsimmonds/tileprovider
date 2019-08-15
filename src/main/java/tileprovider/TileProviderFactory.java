package tileprovider;

/**
 * Created by Joshua.Johnson on 5/6/2019.
 */

public class TileProviderFactory {

    //Private Constructor to prevent instantiation
    private TileProviderFactory() {

    }

    public static TileProvider create() {
        return new LayerManager();
    }
}
