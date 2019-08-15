package tileprovider.get_layers_object;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;

/**
 * Created by joshua.johnson on 3/13/2019.\
 * Class is used to write JSON from GetLayersObject class using jackson.fasterxml
 */

public class GetLayersObjectSerializer extends StdSerializer<GetLayersObject> {
    public GetLayersObjectSerializer() {
        this(null);
    }

    public GetLayersObjectSerializer(Class<GetLayersObject> t) {
        super(t);
    }

    @Override
    public void serialize(GetLayersObject value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("errorMessage", value.getErrorMessage());
        gen.writeArrayFieldStart("layerGroups");
        Map<String, Group> groups;
        if ((groups = value.getLayerGroups()) != null) {
            for (String groupname : groups.keySet()) {
                Group group = groups.get(groupname);
                if (group != null) {
                    gen.writeObject(group);
                }
            }
        }
        gen.writeEndArray();
        gen.writeEndObject();
    }
}
