package com.moon.util.valid;

import java.util.Map;

/**
 * Author : moon
 * Date  : 2018/12/4 16:14
 * Description : Class for
 */
public class ValidUtil {

    /**
     * map空值校验
     * <p>
     * 1.map = null ? true
     * 2.map = new HashMap() ? true
     * 3.map.put("xx",xxx) ? false
     *
     * @param map
     * @return true 空值 false 有值
     */
    public static boolean isMapNull(Map map) {
        return map == null || map.isEmpty() ? true : false;
    }

    /**
     * 多个map空值校验
     * <p>
     * 1.map1 = null and map2 =null ? true
     * 2.map1.put("xx",xxx) and map2=null ? true
     * 3.map1 = null and map2.put("xx",xxx) ? true
     * 4.map1.put("xx",xxx) and map2.put("ss",sss) ? false
     *
     * @param maps
     * @return
     */
    public static boolean isMapAnyNull(Map... maps) {
        for (Map map : maps) {
            if (map == null || map.isEmpty())
                return true;
        }
        return false;
    }
}
