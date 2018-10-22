import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Main2 {
    public static void main(String[] args) {

        JsonParser parser = new JsonParser();
        JsonObject root = null;
        try {
            root = parser.parse(new FileReader(new File("configs/BlackWhiteCodeML.json"))).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BlackWhiteCodeMLFile blackWhiteCodeMLFile = new BlackWhiteCodeMLFile();
        blackWhiteCodeMLFile.configureBarcode(root);
        blackWhiteCodeMLFile.setJsonFile("/Users/zhantong/Documents/GitHub/screen-camera-machine-learning/result.json");
        blackWhiteCodeMLFile.start();
    }
}
