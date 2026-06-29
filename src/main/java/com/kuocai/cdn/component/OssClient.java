package com.kuocai.cdn.component;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson.JSON;
import com.kuocai.cdn.constant.MinioConstants;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import io.minio.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * OSS文件上传服务
 *
 * @author XUEW
 * @date 下午8:59 2023/2/12
 */
@Component
@Slf4j
public class OssClient {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String host;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 上传文件并返回文件网络路径
     *
     * @param file 文件
     * @return URL
     * @author chenwei
     * @date 2023/2/14 10:49 AM
     */
    public String upload(MultipartFile file) throws Exception {
        if (Assert.isEmpty(file)) {
            return null;
        }
        BigDecimal fileSize = KuocaiBaseUtil.flowUnitConversion(file.getSize(), "MB");
        if (fileSize.longValue() > 10) {
            throw new BusinessException("文件过大");
        }
        String filename = file.getOriginalFilename();
        if (Assert.isEmpty(filename)) {
            return null;
        }
        // 判断桶是否存在
        if (!bucketExists()) createBucket();
        // 判断是否上传过此文件
        String fileMd5 = getFileMd5(file);
        String type = filename.substring(filename.lastIndexOf("."));
        String fileName = fileMd5 + type;
        if (getFilesName().contains(fileName)) {
            return buildPublicUrl(fileName);
        }
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .build());
            return buildPublicUrl(fileName);
        }
    }

    public String normalizePublicUrl(String url) {
        if (Assert.isEmpty(url)) {
            return url;
        }
        String trimmedUrl = url.trim().replace("\\", "/");
        String duplicateBucketPath = "/" + bucketName + "/" + bucketName + "/";
        int duplicateBucketIndex = trimmedUrl.indexOf(duplicateBucketPath);
        if (duplicateBucketIndex >= 0) {
            return "/" + bucketName + "/" + trimmedUrl.substring(duplicateBucketIndex + duplicateBucketPath.length());
        }
        String bucketPath = "/" + bucketName + "/";
        int bucketIndex = trimmedUrl.indexOf(bucketPath);
        if (bucketIndex >= 0) {
            return trimmedUrl.substring(bucketIndex);
        }
        return trimmedUrl;
    }

    private String buildPublicUrl(String fileName) {
        return "/" + bucketName + "/" + fileName;
    }

    /**
     * 获取Minio指定文件对象信息
     *
     * @param filename 文件名称
     * @return io.minio.StatObjectResponse
     * @throws Exception e
     * @author bo
     * @date 2023/2/14 10:27 AM
     */
    public StatObjectResponse getStatObject(String filename) throws Exception {
        return minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucketName)
                .object(filename)
                .build());
    }

    /**
     * 获取所有文件
     *
     * @return 文件夹
     * @throws Exception 异常
     */
    public List<Object> getFilesList() throws Exception {
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build());
        Iterator<Result<Item>> iterator = results.iterator();
        List<Object> items = new ArrayList<>();
        String format = "{'fileName':'%s','fileSize':'%s'}";
        while (iterator.hasNext()) {
            Item item = iterator.next().get();
            items.add(JSON.parse((String.format(format, item.objectName(),
                    formatFileSize(item.size())))));
        }
        return items;
    }

    /**
     * 获取所有文件名
     *
     * @return 文件夹
     * @throws Exception 异常
     */
    public List<String> getFilesName() throws Exception {
        List<Object> folderList = this.getFilesList();
        List<String> fileNames = new ArrayList<>();
        if (folderList != null && !folderList.isEmpty()) {
            for (Object value : folderList) {
                Map o = (Map) value;
                String name = (String) o.get("fileName");
                fileNames.add(name);
            }
        }
        return fileNames;
    }

    /**
     * 桶是否存在
     *
     * @return boolean b
     * @author bo
     * @date 2023/2/14 10:53 AM
     */
    public boolean bucketExists() throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    }

    /**
     * 创建桶
     *
     * @return boolean b
     * @author bo
     * @date 2023/2/14 10:53 AM
     */
    public boolean createBucket() throws Exception {
        String READ_WRITE = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetBucketLocation\",\"s3:ListBucket\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}]}";
        boolean b = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!b) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(READ_WRITE).build());
        }
        return MinioConstants.TRUE;
    }

    /**
     * 获取随机文件名
     *
     * @param name 原始文件名
     * @return 随机文件名
     */
    public String getUUIDFileName(String name) {
        String suffix = name.substring(name.lastIndexOf("."));
        String prefix = UUID.randomUUID().toString();
        return prefix + suffix;
    }

    /**
     * 计算文件大小
     *
     * @param bytes 字节数
     * @return java.lang.String
     * @author bo
     * @date 2023/2/14 11:04 AM
     */
    private static String formatFileSize(long bytes) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize = "0B";
        if (bytes == 0) {
            return wrongSize;
        }
        if (bytes < 1024) {
            fileSizeString = df.format((double) bytes) + " B";
        } else if (bytes < 1048576) {
            fileSizeString = df.format((double) bytes / 1024) + " KB";
        } else if (bytes < 1073741824) {
            fileSizeString = df.format((double) bytes / 1048576) + " MB";
        } else {
            fileSizeString = df.format((double) bytes / 1073741824) + " GB";
        }
        return fileSizeString;
    }

    /**
     * 获取文件 md5
     *
     * @param stream 文件流
     * @return MD5
     */
    public String getFileMd5(InputStream stream) {
        return MD5.create().digestHex(stream);
    }

    /**
     * 获取文件 md5
     *
     * @param multipartFile 文件
     * @return MD5
     */
    public String getFileMd5(MultipartFile multipartFile) throws IOException {
        return this.getFileMd5(multipartFile.getInputStream());
    }
}
