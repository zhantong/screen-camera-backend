import java.util.HashMap;
import java.util.Map;

public class SparseIntArray {
    Map<Integer, Integer> map;

    public SparseIntArray() {
        map = new HashMap<>();
    }

    public int get(int key) {
        return map.get(key);
    }

    public void put(int key, int value) {
        map.put(key, value);
    }
}
