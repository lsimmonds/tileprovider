package tileprovider.get_layers_object;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Ian.Clark on 3/12/2019.
 * Data Model for Layer... An object which nests under
 */

public class Layer {

    //Variable names are based upon JSON data model
    public String name;
    public String title;
    public boolean snappable;
    public String thumbnail;

    @JsonProperty("zindex") //camelcase intentionally not used (JSON variable name)
    public Integer zIndex;

    //Default Constructor
    public Layer() {

    }

    //Convenience constructor
    public Layer(String name) {
        this(name, 5);
    }

    //Convenience Constructor
    public Layer(String name, int zIndex) {
        this(name, name, false, null, zIndex);
    }

    //Full Constructor
    public Layer(String name, String title, boolean snappable, String thumbnail, Integer zIndex) {
        this.name = name;
        this.title = title;
        this.snappable = snappable;
        this.thumbnail = thumbnail;
        this.zIndex = zIndex;
    }

    //Copy Constructor
    public Layer(Layer layer) {
        this.name = layer.getName();
        this.title = layer.getTitle();
        this.snappable = layer.isSnappable();
        this.thumbnail = layer.getThumbnail();
        this.zIndex = layer.getzIndex();
    }

    //GET/SET
    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isSnappable() {
        return snappable;
    }

    public void setSnappable(boolean snappable) {
        this.snappable = snappable;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Integer getzIndex() {
        return zIndex;
    }

    public void setzIndex(Integer zIndex) {
        this.zIndex = zIndex;
    }
}
