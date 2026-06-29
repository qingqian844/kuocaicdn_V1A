package com.kuocai.cdn.api.huawei.cdn.constant;

/**
 * 缓存规则类型
 */
public class RuleType {

    /**
     * 全部类型，表示匹配所有文件，默认值。
     */
    public static final int ALL = 0;

    /**
     * 文件类型，表示按文件后缀匹配。
     */
    public static final int FILE = 1;

    /**
     * 文件夹类型，表示按目录匹配。
     */
    public static final int FOLDER = 2;

    /**
     * 文件全路径类型，表示按文件全路径匹配。
     */
    public static final int FILE_ALL_PATH = 3;
}
