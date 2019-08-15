package tileprovider.get_layers_object;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Ian.Clark on 3/12/2019.
 * Data model for GetLayers JSON object.  Used to tell NomsMapApp client what layers are available to draw.
 */

//Classes Used for Serializing/Deserializing from/to JSON using jackson
@JsonSerialize(using=GetLayersObjectSerializer.class)
@JsonDeserialize(using=GetLayersObjectDeserializer.class)


public class GetLayersObject {
    private String errorMessage = null;
    private Map<String, Group> layerGroups = new TreeMap<>();

    //default constructor
    public GetLayersObject() {

    }


    //copy constructor
    public GetLayersObject(GetLayersObject getLayersObject) {

        //copy errorMessage
        this.errorMessage = getLayersObject.getErrorMessage();

        //copy groups
        if (getLayersObject.layerGroups != null) {
            this.layerGroups = new TreeMap<>();
            for (String groupName : getLayersObject.layerGroups.keySet()) {
                Group group = layerGroups.get(groupName);
                addGroup(new Group(group));
            }
        }
    }

    //GETS and SETS
    public Map<String, Group> getLayerGroups() {
        return layerGroups;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Adds a group to the current group list.  If the group with the same name already exists, it should
     * throw some sort of exception indicating conflict
     * @param group - Group to be added
     * @return this instance
     */
    public GetLayersObject addGroup(Group group) {
        if (!layerGroups.containsKey(group.getName())) {
            layerGroups.put(group.getName(), group);
        } else {
            //TODO: Exception should get thrown indicating duplicate group
        }

        return this;
    }


    /**
     * Method for adding a specific layer under a specific group name to this object
     * If the group does not exist, a group is created with the given name
     * @param groupName - Groupname in which to store the layer
     * @param layer - Layer to be added
     * @return this instance
     */
    public GetLayersObject addLayer(String groupName, Layer layer) {

        //search for group
        Group group = layerGroups.get(groupName);

        //If the group is not found
        if (group == null) {
            //create it
            group = new Group(groupName);
            addGroup(group);
        }

        //add this layer to the group
        group.addLayer(layer);

        return this;
    }

    /**
     * Searches the object under each group for this specific layer name. Note that if more than one
     * layer shares the same name, this method only finds the first one found (not in any specific order).
     * @param layerName - Name of the layer to search
     * @return first layer if found, null if not
     */

    public Layer getLayer(String layerName) {
        for (Group group : layerGroups.values()) {
            if (group != null) {
                Layer layer = group.getLayers().get(layerName);
                if (layer != null) {
                    //layer found, return layer
                    return layer;
                }
            }
        }

        //not found
        return null;
    }

    /**
     * Searches the object under each group for the specific layer name.  Note that if more than one
     * layers shares the same name, this method only finds the first one found.  Returns the group under
     * which the layer is nested.
     * @param layerName - name of the layer to search
     * @return - The group containing the layer or null if not found
     */
    public Group getGroupContaining(String layerName) {
        //search each group for layer
        for (Group group : layerGroups.values()) {
            if (group != null) {
                if (group.getLayers().containsKey(layerName)) {
                    //layer found, return group
                    return group;
                }
            }
        }

        //layer not found
        return null;
    }
}
