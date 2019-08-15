package tileprovider.get_layers_object;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

/**
 * Created by joshua.johnson on 3/13/2019.

 * Class is used to parse JSON to GetLayersObject class using jackson.fasterxml
 */

public class GetLayersObjectDeserializer extends StdDeserializer<GetLayersObject> {

    public GetLayersObjectDeserializer() {
        this(null);
    }

    public GetLayersObjectDeserializer(Class<GetLayersObject> t ) {
        super(t);
    }

    @Override
    public GetLayersObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        GetLayersObject getLayersObject = new GetLayersObject();
        while(!p.isClosed()) {
            JsonToken token = p.nextToken();

            if (JsonToken.FIELD_NAME.equals(token)) {
                String fieldName = p.getCurrentName();
                token = p.nextToken();

                if (fieldName.equalsIgnoreCase("errorMessage")) {
                    getLayersObject.setErrorMessage(p.getValueAsString());
                } else if (fieldName.equalsIgnoreCase("layerGroups")) {
                    ObjectMapper mapper = new ObjectMapper();
                    SimpleModule module = new SimpleModule("GroupDeserializer", new Version(1, 0, 0, null, null, null));
                    module.addDeserializer(Group.class, new GroupDeserializer());
                    mapper.registerModule(module);

                    while (token != JsonToken.END_ARRAY) {
                        if (JsonToken.START_OBJECT.equals(token)) {
                            Group group = mapper.readValue(p, Group.class);
                            getLayersObject.addGroup(group);
                        }
                        token = p.nextToken();
                    }
                }
            }



        }

        return getLayersObject;
    }
}
