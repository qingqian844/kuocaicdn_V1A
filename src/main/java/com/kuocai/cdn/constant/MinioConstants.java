package com.kuocai.cdn.constant;


/**
 * @author xiaobo
 */
public class MinioConstants {

    private MinioConstants() {

    }

    public static final String SUCCESS = "成功";
    public static final String FILE_EXIST = "文件已存在";
    public static final String FILE_NOT_EXIST = "文件不存在";
    public static final Boolean TRUE = true;
    public static final Boolean FALSE = false;
    public static final String MEAGER_FILE_FAIL = "合并文件失败";
    public static final String MD5_CHECK_FAIL = "md5值不同.合并出错";
    public static final String MINIO_UPLOAD_FAIL = "文件上传minio失败";
    public static final String MINIO_UPLOAD_SUCCESS = "文件上传minio成功";
    public static final String GET_URL_FAIL = "获取文件预览地址出错";
    public static final String DELETE_SUCCESS = "删除成功";
    public static final String DELETE_FAIL = "删除失败";
    public static final String DELETE_BUCKET_FAIL = "删除临时桶失败";
    public static final String CREATE_BUCKET_FAIL = "创建桶失败";
    public static final String CHUNK_EXIST = "块文件已存在";
    public static final String CHUNK_NOT_EXIST = "块文件不存在";
    public static final String CHUNK_UPLOAD_SUCCESS = "块文件上传成功";
    public static final String MERGE_FAIL = "合并失败";
    public static final String MERGE_SUCCESS = "合并成功";

}

