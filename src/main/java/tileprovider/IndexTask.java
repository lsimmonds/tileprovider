package tileprovider;

/**
 * Created by Joshua.Johnson on 5/6/2019.
 * Interface for indexing task
 */

public interface IndexTask {

    enum Status {WAITING, RUNNING, FINISHED, INTERRUPTED}

    /**
     * Starts the task
     */
    void start();

    /**
     * Stops the task
     */
    void stop();

    Status getStatus();
}
