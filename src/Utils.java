import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

import net.fec.openrq.EncodingPacket;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.parameters.SerializableParameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ReedSolomon.GenericGF;
import ReedSolomon.ReedSolomonDecoder;
import ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2016/9/29.
 */

public final class Utils {
    static String combinePaths(String... paths) {
        if (paths.length == 0) {
            return "";
        }
        File combined = new File(paths[0]);
        int i = 1;
        while (i < paths.length) {
            combined = new File(combined, paths[i]);
            i++;
        }
        return combined.getPath();
    }

    static int calculateMean(int[] array, int low, int high) {
        int sum = 0;
        for (int i = low; i <= high; i++) {
            sum += array[i];
        }
        return sum / (high - low + 1);
    }

    static int[] extractResolution(String string) {
        Pattern pattern = Pattern.compile(".*?(\\d+)x(\\d+).*");
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            int width = Integer.parseInt(matcher.group(1));
            int height = Integer.parseInt(matcher.group(2));
            return new int[]{width, height};
        }
        return null;
    }

    static int bitsToInt(BitSet bitSet, int length, int offset) {
        int value = 0;
        for (int i = 0; i < length; i++) {
            value += bitSet.get(offset + i) ? (1 << i) : 0;
        }
        return value;
    }

    static void crc8Check(int data, int check) throws CRCCheckException {
        CRC8 crc8 = new CRC8();
        crc8.reset();
        crc8.update(data);
        int real = (int) crc8.getValue();
        if (check != real || data < 0) {
            throw CRCCheckException.getNotFoundInstance();
        }
    }

    static int[] changeNumBitsPerInt(int[] originData, int originNumBits, int newNumBits) {
        return changeNumBitsPerInt(originData, 0, originData.length, originNumBits, newNumBits);
    }

    static int[] changeNumBitsPerInt(int[] data, int dataOffset, int dataLength, int originBitsPerInt, int newBitsPerInt) {
        int numDataBits = dataLength * originBitsPerInt;
        int[] array = new int[(int) Math.ceil((float) numDataBits / newBitsPerInt)];
        for (int i = 0; i < numDataBits; i++) {
            if ((data[dataOffset + i / originBitsPerInt] & (1 << (i % originBitsPerInt))) > 0) {
                array[i / newBitsPerInt] |= 1 << (i % newBitsPerInt);
            }
        }
        return array;
    }

    static byte[] intArrayToByteArray(int[] data, int bitsPerInt) {
        return intArrayToByteArray(data, data.length, bitsPerInt, -1);
    }

    static byte[] intArrayToByteArray(int[] intArray, int intArrayLength, int bitsPerInt, int byteArrayLength) {
        int bitsPerByte = 8;
        int numBits = intArrayLength * bitsPerInt;
        if (byteArrayLength != -1) {
            int numByteArrayBits = byteArrayLength * bitsPerByte;
            numBits = Math.min(numBits, numByteArrayBits);
        }
        byte[] array = new byte[(int) Math.ceil((float) numBits / bitsPerByte)];
        for (int i = 0; i < numBits; i++) {
            if ((intArray[i / bitsPerInt] & (1 << (i % bitsPerInt))) > 0) {
                array[i / bitsPerByte] |= 1 << (i % bitsPerByte);
            }
        }
        return array;
    }

    static void rSDecode(int[] originData, int numEc, int ecSize) throws ReedSolomonException {
        GenericGF field;
        switch (ecSize) {
            case 12:
                field = GenericGF.AZTEC_DATA_12;
                break;
            default:
                field = GenericGF.QR_CODE_FIELD_256;
        }
        rSDecode(originData, numEc, field);
    }

    static void rSDecode(int[] originData, int numEc, GenericGF field) throws ReedSolomonException {
        ReedSolomonDecoder decoder = new ReedSolomonDecoder(field);
        decoder.decode(originData, numEc);
    }

    static int[] concatIntArray(int[] arrayA, int[] arrayB) {
        int lengthArrayA = arrayA.length;
        int lengthArrayB = arrayB.length;
        int[] concat = new int[lengthArrayA + lengthArrayB];
        System.out.println();
        System.arraycopy(arrayA, 0, concat, 0, lengthArrayA);
        System.arraycopy(arrayB, 0, concat, lengthArrayA, lengthArrayB);
        return concat;
    }

    static List<Pair> findLine(int x0, int y0, int x1, int y1) {
        List<Pair> line = new ArrayList<>();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int err = dx - dy;
        int e2;
        int currentX = x0;
        int currentY = y0;

        while (true) {
            line.add(new Pair(currentX, currentY));

            if (currentX == x1 && currentY == y1) {
                break;
            }
            e2 = 2 * err;
            if (e2 > -1 * dy) {
                err -= dy;
                currentX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currentY += sy;
            }
        }
        return line;
    }

    static List<BitSet> randomBitSetList(int bitSetLength, int listLength, int randomSeed) {
        List<BitSet> bitSets = new ArrayList<>(listLength);
        Random random = new Random(randomSeed);
        for (int i = 0; i < listLength; i++) {
            BitSet bitSet = new BitSet(bitSetLength);
            for (int pos = 0; pos < bitSetLength; pos++) {
                if (random.nextBoolean()) {
                    bitSet.set(pos);
                }
            }
            bitSets.add(bitSet);
        }
        return bitSets;
    }

    static int diff(int[] arrayA, int[] arrayB) {
        if (arrayA.length != arrayB.length) {
            throw new IllegalArgumentException();
        }
        int count = 0;
        for (int i = 0; i < arrayA.length; i++) {
            if (arrayA[i] != arrayB[i]) {
                count++;
            }
        }
        return count;
    }

    static Pair getMostCommon(int[] origin, List<int[]> arrayList) {
        int leastDiffCount = Integer.MAX_VALUE;
        int[] leastDiffArray = null;
        for (int[] array : arrayList) {
            int diffCount = diff(origin, array);
            if (leastDiffCount > diffCount) {
                leastDiffCount = diffCount;
                leastDiffArray = array;
            }
        }
        return new Pair(leastDiffCount, leastDiffArray);
    }

    static Pair getMostCommon(BitSet origin, List<BitSet> bitSetList) {
        int leastDiffCount = Integer.MAX_VALUE;
        BitSet leastDiffBitset = null;
        for (BitSet bitSet : bitSetList) {
            BitSet clone = (BitSet) bitSet.clone();
            clone.xor(origin);
            int countSame = clone.cardinality();
            if (countSame < leastDiffCount) {
                leastDiffCount = countSame;
                leastDiffBitset = bitSet;
            }
        }
        return new Pair(leastDiffCount, leastDiffBitset);
    }

    static BitSet intArrayToBitSet(int[] data, int bitsPerInt) {
        int index = 0;
        BitSet bitSet = new BitSet();
        for (int current : data) {
            for (int i = 0; i < bitsPerInt; i++) {
                if ((current & (1 << i)) > 0) {
                    bitSet.set(index);
                }
                index++;
            }
        }
        return bitSet;
    }

    static int max(int[] array) {
        int max = -1;
        for (int item : array) {
            if (item > max) {
                max = item;
            }
        }
        return max;
    }

    static int grayCodeToInt(int n) {
        String gray = Integer.toBinaryString(n);
        String binary = "";
        binary += gray.charAt(0);
        for (int i = 1; i < gray.length(); i++) {
            if (gray.charAt(i) == '0') {
                binary += binary.charAt(i - 1);
            } else {
                binary += binary.charAt(i - 1) == '0' ? '1' : '0';
            }
        }
        return Integer.parseInt(binary, 2);
    }

    static BitSet reverse(BitSet origin, int length) {
        BitSet reversed = new BitSet();
        for (int i = 0; i < length; i++) {
            if (origin.get(i)) {
                reversed.set(length - i - 1, true);
            }
        }
        return reversed;
    }

    static Object loadObjectFromFile(String filePath) {
        try {
            FileInputStream fis = new FileInputStream(filePath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object object = ois.readObject();
            return object;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    static int[] bitSetToIntArray(BitSet bitSet, int length, int bitsPerInt) {
        int[] array = new int[(int) Math.ceil((float) length / bitsPerInt)];
        for (int i = 0; i < length; i++) {
            if (bitSet.get(i)) {
                array[i / bitsPerInt] |= 1 << (i % bitsPerInt);
            }
        }
        return array;
    }

    static boolean bytesToFile(byte[] bytes, String filePath) {
        if (filePath.isEmpty()) {
            throw new IllegalArgumentException("Empty file path");
        }
        File file = new File(filePath);
        OutputStream os;
        try {
            os = new FileOutputStream(file);
            os.write(bytes);
            os.close();
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Wrong file path: " + filePath);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static JsonObject fecParametersToJson(FECParameters parameters) {
        JsonObject root = new JsonObject();
        root.addProperty("numDataBytes", parameters.dataLength());
        root.addProperty("numSymbolBytes", parameters.symbolSize());
        root.addProperty("numSourceBlocks", parameters.numberOfSourceBlocks());
        root.addProperty("numSourceSymbols", parameters.totalSymbols());
        root.addProperty("lengthInterleaver", parameters.interleaverLength());

        SerializableParameters serializableParameters = parameters.asSerializable();
        root.addProperty("commonOTI", serializableParameters.commonOTI());
        root.addProperty("schemeSpecificOTI", serializableParameters.schemeSpecificOTI());
        return root;
    }

    public static JsonObject encodingPacketToJson(EncodingPacket encodingPacket) {
        JsonObject root = new JsonObject();
        root.addProperty("encodingSymbolID", encodingPacket.encodingSymbolID());
        root.addProperty("fecPayloadID", encodingPacket.fecPayloadID());
        root.addProperty("sourceBlockNumber", encodingPacket.sourceBlockNumber());
        root.addProperty("symbolType", encodingPacket.symbolType().name());
        return root;
    }

    public static JsonObject sourceBlockDecoderToJson(SourceBlockDecoder decoder) {
        Gson gson = new Gson();
        JsonObject root = new JsonObject();
        root.add("availableRepairSymbols", gson.toJsonTree(decoder.availableRepairSymbols()));
        root.add("missingSourceSymbols", gson.toJsonTree(decoder.missingSourceSymbols()));
        return root;
    }

    public static File correctFileExtension(File file) {
        String originFileName = file.getName();
        int lastSeparatorIndex = originFileName.lastIndexOf('.');
        String fileNameWithoutExtension = originFileName;
        String originExtension = "";
        if (lastSeparatorIndex != -1) {
            fileNameWithoutExtension = originFileName.substring(0, lastSeparatorIndex);
            originExtension = originFileName.substring(lastSeparatorIndex + 1);
        }
        String correctedExtension = getFileExtension(file);
        if (!correctedExtension.equals(originExtension)) {
            String correctedFileName = fileNameWithoutExtension + "." + correctedExtension;
            File newFile = new File(file.getParent(), correctedFileName);
            file.renameTo(newFile);
            file = newFile;
        }
        return file;
    }

    public static String getFileExtension(File file) {
        if (!file.isFile()) {
            throw new RuntimeException("file not exists");
        }
        ContentInfoUtil util = new ContentInfoUtil();
        ContentInfo info;
        try {
            info = util.findMatch(file);
        } catch (IOException e) {
            throw new RuntimeException("file not exists");
        }
        if (info == null) {
            return "txt";
        }
        return info.getFileExtensions()[0];
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    public static void saveStringToFile(String string, String filePath) throws Exception {
        FileOutputStream fos = new FileOutputStream(new File(filePath));
        fos.write(string.getBytes("UTF-8"));
        fos.close();
    }
}
