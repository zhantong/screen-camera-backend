import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.parameters.SerializableParameters;

import java.util.BitSet;
import java.util.List;


/**
 * Created by zhantong on 2016/11/28.
 */

public class ShiftCodeMLStream extends BlackWhiteCodeMLStream {
    private static final String TAG = "ShiftCodeMLStream";
    private static final boolean DUMP = true;

    private List<BitSet> truthBitSetList;

    private List<BitSet> loadBitSetListFromFile(String fileName) {
        String filePath = Utils.combinePaths(Config.getProperty("storage_path"), fileName);
        return (List<BitSet>) Utils.loadObjectFromFile(filePath);
    }

    public ShiftCodeMLStream() {
        //truthBitSetList = loadBitSetListFromFile("truth.txt");
    }

    ShiftCodeML getBarcodeInstance(MediateBarcode mediateBarcode) {
        return new ShiftCodeML(mediateBarcode);
    }

    void sampleContent(BlackWhiteCodeML blackWhiteCodeML) {
        blackWhiteCodeML.mediateBarcode.getContent(blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN), RawImage.CHANNLE_Y);
    }

    @Override
    public void processFrame(RawImage frame) {
        int raptorQSymbolSize = -1;
        ArrayDataDecoder dataDecoder = null;
        JsonObject barcodeJson = new JsonObject();
        LOG.info(frame.toString());

        MediateBarcode mediateBarcode;
        try {
            mediateBarcode = new MediateBarcode(frame, barcodeConfig, null, RawImage.CHANNEL_GRAY);
        } catch (NotFoundException e) {
            LOG.info("barcode not found");
            return;
        }
        ShiftCodeML shiftCodeML = getBarcodeInstance(mediateBarcode);
        sampleContent(shiftCodeML);
        int overlapSituation = shiftCodeML.getOverlapSituation();

        if (DUMP) {
            JsonObject mainJson = shiftCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).toJson();
            barcodeJson.add("barcode", mainJson);
            barcodeJson.addProperty("index", shiftCodeML.mediateBarcode.rawImage.getIndex());
            JsonElement varyBarJson = shiftCodeML.getVaryBarToJson();
            barcodeJson.add("varyBar", varyBarJson);
            barcodeJson.addProperty("overlapSituation", overlapSituation);
            barcodeJson.addProperty("isRandom", shiftCodeML.getIsRandom());
            //String jsonString=new Gson().toJson(root);
            //LOG.info(CustomMarker.source,jsonString);
        }
        if (shiftCodeML.getIsRandom()) {
            try {
                int index = shiftCodeML.getTransmitFileLengthInBytes();
                System.out.println("random index: " + index);
                barcodeJson.addProperty("randomIndex", index);
                if (index >= numRandomBarcode) {
                    return;
                }
                int[] value = randomIntArrayList.get(index);
                if (DUMP) {
                    barcodeJson.add("value", new Gson().toJsonTree(value));
                    //root.addProperty("index",shiftCodeML.mediateBarcode.rawImage.getIndex());
                    //LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (CRCCheckException e) {
                e.printStackTrace();
            }
        } else {
            if (raptorQSymbolSize == -1) {
                raptorQSymbolSize = shiftCodeML.calcRaptorQSymbolSize(shiftCodeML.calcRaptorQPacketSize());
            }
            if (dataDecoder == null) {
                try {
                    int head = shiftCodeML.getTransmitFileLengthInBytes();
                    int numSourceBlock = Integer.parseInt(barcodeConfig.hints.get(BlackWhiteCodeML.KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS).toString());
                    FECParameters parameters;
                    try {
                        parameters = FECParameters.newParameters(head, raptorQSymbolSize, numSourceBlock);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        return;
                    }
                    if (DUMP) {
                        JsonObject paramsJson = new JsonObject();
                        SerializableParameters serializableParameters = parameters.asSerializable();
                        paramsJson.addProperty("commonOTI", serializableParameters.commonOTI());
                        paramsJson.addProperty("schemeSpecificOTI", serializableParameters.schemeSpecificOTI());
                        LOG.info(CustomMarker.raptorQMeta, new Gson().toJson(paramsJson));
                    }
                    System.out.println("FECParameters: " + parameters.toString());
                    LOG.info("data length: " + parameters.dataLengthAsInt() + " symbol length: " + parameters.symbolSize());
                    dataDecoder = OpenRQ.newDecoder(parameters, 0);
                } catch (CRCCheckException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
        if (true) {
            int[][] rawContents = shiftCodeML.getRawContents();
            int leastDiffCount = Integer.MAX_VALUE;
            BitSet leastDiffBitSet = null;
            for (int[] rawContent : rawContents) {
                BitSet inBitSet = Utils.intArrayToBitSet(rawContent, 2);
                Pair pair = Utils.getMostCommon(inBitSet, truthBitSetList);
                int diffCount = (int) pair.first;
                BitSet diffBitSet = (BitSet) pair.second;
                if (leastDiffCount > diffCount) {
                    leastDiffCount = diffCount;
                    leastDiffBitSet = diffBitSet;
                }
            }
            if (DUMP) {
                barcodeJson.add("truth", new Gson().toJsonTree(Utils.bitSetToIntArray(leastDiffBitSet, rawContents[0].length * 2, 2)));
            }
        }
        if (DUMP) {
            LOG.info(CustomMarker.processed, new Gson().toJson(barcodeJson));
        }
    }
}
