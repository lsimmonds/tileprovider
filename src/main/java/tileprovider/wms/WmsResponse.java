package tileprovider.wms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by joshua.johnson on 4/23/2019.
 */

public class WmsResponse {
    private ByteArrayOutputStream byteArrayOutputStream;
    private String mimeType;

    WmsResponse(byte[] data, String mimeType) {
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byteArrayOutputStream.write(data);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.mimeType = mimeType;
    }

    WmsResponse(ByteArrayOutputStream byteArrayOutputStream, String mimeType) {
        this.byteArrayOutputStream = byteArrayOutputStream;
        this.mimeType = mimeType;
    }

    public ByteArrayOutputStream getByteArrayOutputStream() {
        return byteArrayOutputStream;
    }

    public String getMimeType() {
        return mimeType;
    }

}
