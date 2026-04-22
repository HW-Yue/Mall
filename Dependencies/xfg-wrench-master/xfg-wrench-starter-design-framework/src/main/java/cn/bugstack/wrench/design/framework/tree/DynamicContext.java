package cn.bugstack.wrench.design.framework.tree;

import java.util.HashMap;
import java.util.Map;

/**
 * 上下文对象
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/12/7 09:07
 */
public class DynamicContext {

    private final Map<String, Object> dataObjects = new HashMap<>();

    public <T> void setValue(String key, T value) {
        dataObjects.put(key, value);
    }

    public <T> T getValue(String key) {
        return (T) dataObjects.get(key);
    }

}
