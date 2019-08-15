package tileprovider.get_layers_object;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Created by joshua.johnson on 3/13/2019.
 * Class is used to parse JSON to Group class using jackson.fasterxml
 */

public class GroupDeserializer extends StdDeserializer {

    GroupDeserializer() {
        this(null);
    }

    GroupDeserializer(Class<Group> t) {
        super(t);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {

        Group group = new Group();

        JsonToken token = p.currentToken();

        while(!JsonToken.END_OBJECT.equals(token)) {

            token = p.nextToken();

            if (JsonToken.FIELD_NAME.equals(token)) {
                String fieldName = p.getCurrentName();
                token = p.nextToken();

                if (fieldName.equalsIgnoreCase("urls")) {
                    while (!JsonToken.END_ARRAY.equals(token)) {
                        if (JsonToken.VALUE_STRING.equals(token)) {
                            group.addUrl(p.getValueAsString());
                        }
                        token = p.nextToken();
                    }
                }

                else if (fieldName.equalsIgnoreCase("layers")) {
                    while (!JsonToken.END_ARRAY.equals(token)) {
                        if (JsonToken.START_OBJECT.equals(token)) {
                            ObjectMapper mapper = new ObjectMapper();
                            Layer layer = mapper.readValue(p, Layer.class);
                            group.addLayer(layer);
                        }
                        token = p.nextToken();
                    }
                }

                else if (fieldName.equalsIgnoreCase("name")) {
                    group.setName(p.getValueAsString());
                }

                else if (fieldName.equalsIgnoreCase("masterName")) {
                    group.setMasterName(p.getValueAsString());
                }
            }
        }

        return group;

    }
}
