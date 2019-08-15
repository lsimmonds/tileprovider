package tileprovider;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tileprovider.wms.WmsFactory;
import tileprovider.wms.WmsRequest;
import tileprovider.wms.WmsResponse;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class SimpleController {

    @RequestMapping(value = {"/tileprovider/{applicationName}", "/tileprovider/ows", "/tileprovider/wms"})
    public WmsResponse simpleResponse(@RequestParam(value = "layers", defaultValue = "NOMS-BlueMarbleA") String content) {
        WmsRequest wmsRequest = WmsFactory.generateWmsRequest(null, new HashMap<>()); //TODO - fill in with real params
        return wmsRequest.getResponse();
    }
}
