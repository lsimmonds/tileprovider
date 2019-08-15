package tileprovider.get_layers_object;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Ian.Clark on 3/12/2019.
 * Data Model for Group as a component of GetLayersObject
 * @see GetLayersObject
 */

//Jackson FasterXML serializer/deserializer classes
@JsonSerialize(using = GroupSerializer.class)
@JsonDeserialize(using = tileprovider.get_layers_object.GroupDeserializer.class)


public class Group {

    private List<String> urls = new LinkedList<>();
    private Map<String, Layer> layers = new TreeMap<>();
    private String name = null;
    private String masterName = null;

    static final String DEFAULT_MASTERNAME = "NOMS";

    //default constructor
    Group() {
        //intentionally blank
    }

    //convenience constructor
    public Group(String name) {
        this(name, DEFAULT_MASTERNAME);
    }

    //convenience constructor
    public Group(String name, String masterName) {
        this(new LinkedList(), new TreeMap<String, Layer>(), name, masterName);
    }

    //explicit constructor
    public Group(List<String> urls, Map<String, Layer> layers, String name, String masterName) {
        this.urls = urls;
        this.layers = layers;
        this.name = name;
        this.masterName = masterName;
    }

    //copy constructor
    public Group(Group group) {
        //copy url
        if (group.getUrls() != null) {
            this.urls = new LinkedList<>(group.urls);
        }

        //copy layers
        if (group.getLayers() != null) {
            this.layers = new TreeMap<>();
            for (String layerName : group.getLayers().keySet()) {
                Layer layer = group.getLayers().get(layerName);
                this.layers.put(layer.getName(), new Layer(layer));
            }
        }

        //copy name
        this.name = group.getName();

        //copy master name
        this.masterName = group.getMasterName();
    }

    //GET method
    public List<String> getUrls() {
        return urls;
    }

    /**
     * Adds the specified urlString
     * @param urlString - to be added
     * @return this instance
     */
    public Group addUrl(String urlString) {
        urls.add(urlString);
        return this;
    }

    /**
     * Removes the specified urlString if found
     * @param urlString - urlString to be removed
     * @return this instance
     */
    public Group removeUrl(String urlString) {
        urls.remove(urlString);
        return this;
    }

    //GET Method
    public Map<String, Layer> getLayers() {
        return layers;
    }

    /**
     * Adds layer to group
     * @param layer non-null layer to be added
     * @return instance of this object
     */
    public Group addLayer(Layer layer) {
        if (layer != null) {
            layers.put(layer.getName(), layer);
        }

        return this;
    }

    //GET/SET name
    public String getName() {
        return name;
    }
    public void setName(String name) {this.name = name;}

    //GET/SET masterName
    public String getMasterName() {
        return masterName;
    }
    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }
}
