package cn.bugstack.wrench.design.framework.link.model2;

import java.util.HashMap;
import java.util.Map;

/**
 * 链路动态上下文
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/12 16:34
 */
public class DynamicContext {

    /**
     * 节点放行标识
     */
    private boolean proceed;

    /**
     * 跳过当前节点
     */
    private boolean jump;

    public DynamicContext() {
        this.proceed = true;
        this.jump = false;
    }

    private final Map<String, Object> dataObjects = new HashMap<>();

    public <T> void setValue(String key, T value) {
        dataObjects.put(key, value);
    }

    public <T> T getValue(String key) {
        return (T) dataObjects.get(key);
    }

    public boolean isProceed() {
        return proceed;
    }

    public void setProceed(boolean proceed) {
        this.proceed = proceed;
    }

    public boolean isJump() {
        return jump;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }

}
