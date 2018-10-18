import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static void main(String[] args) {

        JsonParser parser = new JsonParser();
        JsonObject root = null;
        try {
            root = parser.parse(new FileReader(new File("configs/ShiftCodeML.json"))).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BarcodeFormat barcodeFormat = BarcodeFormat.fromString(root.get("barcodeFormat").getAsString());
        ShiftCodeMLStream shiftCodeMLStream = new ShiftCodeMLStream();
        shiftCodeMLStream.configureBarcode(root);
        LinkedBlockingQueue<RawImage> queue = shiftCodeMLStream.getQueue();
        Thread thread = new Thread() {
            public void run() {
                File file = new File("1684.mov");
                FrameGrab grab = null;
                try {
                    grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JCodecException e) {
                    e.printStackTrace();
                }
                Picture picture = null;
                int count = 0;
                while (true) {
                    try {
                        picture = grab.getNativeFrame();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (picture == null) {
                        break;
                    }
                    byte[][] pictureData = picture.getData();
                    byte[] data = new byte[pictureData[0].length + pictureData[1].length + pictureData[2].length];
                    System.arraycopy(pictureData[0], 0, data, 0, pictureData[0].length);
                    System.arraycopy(pictureData[1], 0, data, pictureData[0].length, pictureData[1].length);
                    System.arraycopy(pictureData[2], 0, data, pictureData[0].length + pictureData[1].length, pictureData[2].length);
                    try {
                        queue.put(new RawImage(data, picture.getWidth(), picture.getHeight(), RawImage.COLOR_TYPE_YUV));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
        shiftCodeMLStream.start();
    }
}
