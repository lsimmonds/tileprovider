package tileprovider;

/**
 * Created by Joshua.Johnson on 5/6/2019.
 * Interface for a progress listener for FeatureLayer's Index method.
 */

public interface IndexProgressListener {

    /**
     * When a progress update is available, this method is called.
     * @param subProgress - For a given layer, this is the progress of that layer
     * @param subOutOf - This is the total progress that will be reached for given layer
     * @param superProgress - Number of layers completed (regardless of success)
     * @param superOutOf - Number of total layers to be indexed (regardless of success).  Does not change during task.
     */
    void onProgressUpdate(String layerName, int subProgress, int subOutOf, int superProgress, int superOutOf);

    void onTaskComplete(boolean isSuccessful);
}
