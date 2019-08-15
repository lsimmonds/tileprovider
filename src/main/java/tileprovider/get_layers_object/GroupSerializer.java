package tileprovider.get_layers_object;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Created by joshua.johnson on 3/13/2019.
 * Class is used to write JSON from Group class using jackson.fasterxml
 */

public class GroupSerializer extends StdSerializer<Group> {

    public GroupSerializer() {
        this(null);
    }

    public GroupSerializer(Class<Group> t) {
        super(t);
    }

    @Override
    public void serialize(Group value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        gen.writeArrayFieldStart("urls");
        for(String url : value.getUrls()) {
            gen.writeString(url);
        }
        gen.writeEndArray();

        gen.writeArrayFieldStart("layers");
        for(String layerName : value.getLayers().keySet()) {
//            ObjectMapper mapper = new ObjectMapper();
            gen.writeObject(value.getLayers().get(layerName));
        }
        gen.writeEndArray();

        gen.writeStringField("name", value.getName());

        gen.writeStringField("masterName", value.getMasterName());

        gen.writeEndObject();
    }
}
