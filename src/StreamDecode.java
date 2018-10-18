import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * Created by zhantong on 2016/12/4.
 */

public class StreamDecode {
    private static final int QUEUE_TIME_OUT = 4;
    private boolean stopQueue = false;
    private boolean isVideo = false;
    private boolean isCamera = false;
    private boolean isImage = false;
    private boolean isJsonFile = false;
    private LinkedBlockingQueue<RawImage> queue;
    private String videoFilePath;
    protected JsonObject inputJsonRoot;
    Logger FILE_LOG;
    Logger LOG;
    BarcodeConfig barcodeConfig;

    public StreamDecode() {
        queue = new LinkedBlockingQueue<>(4);
    }


    public void setJsonFile(String jsonFilePath) {
        try {
            JsonParser parser = new JsonParser();
            inputJsonRoot = (JsonObject) parser.parse(new FileReader(jsonFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        isJsonFile = true;
    }

    public void setStopQueue() {
        stopQueue = true;
    }


    public void setBarcodeConfig(BarcodeConfig barcodeConfig) {
        this.barcodeConfig = barcodeConfig;
    }


    public LinkedBlockingQueue<RawImage> getQueue() {
        return queue;
    }

    public boolean getIsVideo() {
        return isVideo;
    }

    public boolean getIsCamera() {
        return isCamera;
    }

    void beforeStream() {
    }

    void processFrame(RawImage frame) {
    }

    void processFrame(JsonElement frameData) {
    }

    File restoreFile() {
        return null;
    }

    void afterStream() {
    }

    void configureBarcode(JsonObject jsonRoot) {
        barcodeConfig = new BarcodeConfig();
        barcodeConfig.barcodeFormat = BarcodeFormat.fromString(jsonRoot.get("barcodeFormat").getAsString());
        barcodeConfig.mainWidth = jsonRoot.get("mainWidth").getAsInt();
        barcodeConfig.mainHeight = jsonRoot.get("mainHeight").getAsInt();
        if (jsonRoot.has("borderLength")) {
            JsonElement e = jsonRoot.get("borderLength");
            if (e.isJsonPrimitive()) {
                barcodeConfig.borderLength = new DistrictConfig<>(e.getAsInt());
            } else if (e.isJsonArray()) {
                barcodeConfig.borderLength = new DistrictConfig<>(new Gson().fromJson(e, Integer[].class));
            }
        }
        if (jsonRoot.has("paddingLength")) {
            JsonElement e = jsonRoot.get("paddingLength");
            if (e.isJsonPrimitive()) {
                barcodeConfig.paddingLength = new DistrictConfig<>(e.getAsInt());
            } else if (e.isJsonArray()) {
                barcodeConfig.paddingLength = new DistrictConfig<>(new Gson().fromJson(e, Integer[].class));
            }
        }
        if (jsonRoot.has("metaLength")) {
            JsonElement e = jsonRoot.get("metaLength");
            if (e.isJsonPrimitive()) {
                barcodeConfig.metaLength = new DistrictConfig<>(e.getAsInt());
            } else if (e.isJsonArray()) {
                barcodeConfig.metaLength = new DistrictConfig<>(new Gson().fromJson(e, Integer[].class));
            }
        }
        String PACKAGE_NAME = this.getClass().getPackageName();
        if (jsonRoot.has("borderBlock")) {
            try {
                Method method = Class.forName(jsonRoot.get("borderBlock").getAsJsonObject().get("type").getAsString()).getMethod("fromJson", JsonObject.class);
                barcodeConfig.borderBlock = new DistrictConfig<>((Block) method.invoke(null, jsonRoot.get("borderBlock").getAsJsonObject()));
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
        if (jsonRoot.has("paddingBlock")) {
            try {
                Method method = Class.forName(jsonRoot.get("paddingBlock").getAsJsonObject().get("type").getAsString()).getMethod("fromJson", JsonObject.class);
                barcodeConfig.paddingBlock = new DistrictConfig<>((Block) method.invoke(null, jsonRoot.get("paddingBlock").getAsJsonObject()));
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
        if (jsonRoot.has("metaBlock")) {
            try {
                Method method = Class.forName(jsonRoot.get("metaBlock").getAsJsonObject().get("type").getAsString()).getMethod("fromJson", JsonObject.class);
                barcodeConfig.metaBlock = new DistrictConfig<>((Block) method.invoke(null, jsonRoot.get("metaBlock").getAsJsonObject()));
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
        if (jsonRoot.has("mainBlock")) {
            try {
                Method method = Class.forName(jsonRoot.get("mainBlock").getAsJsonObject().get("type").getAsString()).getMethod("fromJson", JsonObject.class);
                barcodeConfig.mainBlock = new DistrictConfig<>((Block) method.invoke(null, jsonRoot.get("mainBlock").getAsJsonObject()));
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
        barcodeConfig.hints = new Gson().fromJson(jsonRoot.get("hints"), new TypeToken<Map<String, String>>() {
        }.getType());
    }

    public void stream(LinkedBlockingQueue<RawImage> frames) throws InterruptedException {
        beforeStream();
        for (RawImage frame; ((frame = frames.poll(QUEUE_TIME_OUT, TimeUnit.SECONDS)) != null) && (frame.getPixels() != null); ) {
            processFrame(frame);
            if (stopQueue) {
                if (isVideo) {
                    // stopVideoDecoding();
                }
                if (isCamera) {
                    // stopCamera();
                }
                queue.clear();
                RawImage rawImage = new RawImage();
                queue.add(rawImage);
            }
        }
        final File file = restoreFile();
        if (file != null) {
            LOG.info("文件传输完成");
        }
        afterStream();
    }

    public void stream(JsonArray framesData) {
        beforeStream();
        for (JsonElement frameData : framesData) {
            processFrame(frameData);
            if (stopQueue) {
                break;
            }
        }
        afterStream();
    }

    public void start() {
        initLogging();
        if (isJsonFile) {
            JsonArray data = inputJsonRoot.getAsJsonArray("values");
            stream(data);
        } else {
            try {
                stream(queue);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void initLogging() {
        boolean enableLogging = true;
        if (enableLogging) {
            ConfigureLogback.configureLogbackDirectly(Utils.combinePaths(Config.getProperty("storage_path"), (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())) + ".log"));
            FILE_LOG = LoggerFactory.getLogger(FILE_LOG.class);
            LOG = LoggerFactory.getLogger(LOG.class);
        } else {
            FILE_LOG = null;
            LOG = null;
        }
    }
}
