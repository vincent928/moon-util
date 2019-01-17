package com.moon.util.string;

import java.util.UUID;

/**
 * Author : moon
 * Date  : 2019/1/17 10:41
 * Description : Class for
 */
public class UUIDUtil {

    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }


}
