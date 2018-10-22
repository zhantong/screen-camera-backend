import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static void main(String[] args) {

        JsonParser parser = new JsonParser();
        JsonObject root = null;
        try {
            root = parser.parse(new FileReader(new File("configs/BlackWhiteCodeML.json"))).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BlackWhiteCodeMLStream blackWhiteCodeMLStream = new BlackWhiteCodeMLStream();
        blackWhiteCodeMLStream.configureBarcode(root);
        LinkedBlockingQueue<RawImage> queue = blackWhiteCodeMLStream.getQueue();
        Thread thread = new Thread() {
            public void run() {
                File directory = new File("/Users/zhantong/Desktop/Screen-Camera-Backend-Preprocess/VID_20180913_155407");
                File[] images = directory.listFiles();
                Arrays.sort(images);
                for (int i = 0; i < images.length; i++) {
                    File image = images[i];
                    System.out.println(image.getName());
                    try {
                        BufferedImage frame = ImageIO.read(image);
                        try {
                            JsonObject settings = new JsonObject();
                            settings.add("thresholds", new Gson().toJsonTree(new int[]{220, 0, 0}));
                            RawImage rawImage = new RawImage(frame, i, image.getName());
                            rawImage.customSettings(settings);
                            queue.put(rawImage);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
        blackWhiteCodeMLStream.start();
    }
}
