import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.fec.openrq.parameters.FECParameters;

import java.util.List;

/**
 * Created by zhantong on 2017/5/11.
 */

public class BlackWhiteCodeMLStream extends StreamDecode {
    private static final String TAG = "BlackWhiteCodeMLStream";
    private static final boolean DUMP = true;
    int transmitFileLengthInBytes = -1;
    int numRandomBarcode;

    List<int[]> randomIntArrayList;


    @Override
    void configureBarcode(JsonObject jsonRoot) {
        super.configureBarcode(jsonRoot);
        numRandomBarcode = Integer.parseInt(barcodeConfig.hints.get(BlackWhiteCodeML.KEY_NUMBER_RANDOM_BARCODES).toString());
        randomIntArrayList = getBarcodeInstance(new MediateBarcode(barcodeConfig)).randomBarcodeValue(barcodeConfig, numRandomBarcode);
    }

    BlackWhiteCodeML getBarcodeInstance(MediateBarcode mediateBarcode) {
        return new BlackWhiteCodeML(mediateBarcode);
    }


    void sampleContent(BlackWhiteCodeML blackWhiteCodeML) {
        blackWhiteCodeML.mediateBarcode.getContent(blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN), RawImage.CHANNLE_Y);
    }

    MediateBarcode getMediateBarcode(RawImage rawImage) {
        try {
            return new MediateBarcode(rawImage, barcodeConfig, null, RawImage.CHANNEL_GRAY);
        } catch (NotFoundException e) {
            return null;
        }
    }

    @Override
    public void beforeStream() {
        FILE_LOG.info(CustomMarker.barcodeConfig, new Gson().toJson(barcodeConfig.toJson()));
    }

    @Override
    public void processFrame(RawImage frame) {
        Gson gson = new Gson();
        JsonObject jsonRoot = new JsonObject();
        LOG.info(frame.toString());
        LOG.info(frame.fileName);
        MediateBarcode mediateBarcode = getMediateBarcode(frame);
        if (DUMP) {
            jsonRoot.add("image", frame.toJson());
        }
        if (mediateBarcode != null) {
            BlackWhiteCodeML blackWhiteCodeML = getBarcodeInstance(mediateBarcode);
            sampleContent(blackWhiteCodeML);
            if (DUMP) {
                jsonRoot.add("barcode", blackWhiteCodeML.toJson());
            }
            if (blackWhiteCodeML.getIsRandom()) {
                int index = Integer.MAX_VALUE;
                try {
                    index = blackWhiteCodeML.getTransmitFileLengthInBytes();
                } catch (CRCCheckException e) {
                    e.printStackTrace();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (index < numRandomBarcode) {
                    JsonObject randomJsonRoot = new JsonObject();
                    randomJsonRoot.addProperty("index", index);
                    randomJsonRoot.add("truthValue", gson.toJsonTree(randomIntArrayList.get(index)));
                    jsonRoot.add("random", randomJsonRoot);
                }
            } else {
                if (transmitFileLengthInBytes == -1) {
                    try {
                        transmitFileLengthInBytes = blackWhiteCodeML.getTransmitFileLengthInBytes();
                    } catch (CRCCheckException e) {
                        e.printStackTrace();
                    }
                    if (transmitFileLengthInBytes != -1) {
                        int raptorQSymbolSize = blackWhiteCodeML.calcRaptorQSymbolSize(blackWhiteCodeML.calcRaptorQPacketSize());
                        int numSourceBlock = Integer.parseInt(barcodeConfig.hints.get(BlackWhiteCodeML.KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS).toString());
                        FECParameters parameters = FECParameters.newParameters(transmitFileLengthInBytes, raptorQSymbolSize, numSourceBlock);
                        if (DUMP) {
                            FILE_LOG.info(CustomMarker.fecParameters, new Gson().toJson(Utils.fecParametersToJson(parameters)));
                        }
                    }
                }
            }
        }
        if (DUMP) {
            FILE_LOG.info(CustomMarker.processed, new Gson().toJson(jsonRoot));
        }
    }
}
