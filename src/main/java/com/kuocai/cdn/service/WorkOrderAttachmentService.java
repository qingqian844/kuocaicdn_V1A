package com.kuocai.cdn.service;

import com.kuocai.cdn.component.OssClient;
import com.kuocai.cdn.dto.WorkOrderMessageDTO;
import com.kuocai.cdn.exception.BusinessException;
import io.minio.GetObjectResponse;
import io.minio.StatObjectResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class WorkOrderAttachmentService {

    static final long MAX_ATTACHMENT_SIZE = 10L * 1024L * 1024L;

    private static final Set<String> IMAGE_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    )));
    private static final Set<String> FILE_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "pdf", "txt", "csv", "log", "md", "json", "xml",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "rar", "7z", "gz", "tar"
    )));
    private static final Map<String, String> CONTENT_TYPES = buildContentTypes();

    private final OssClient ossClient;

    public WorkOrderAttachmentService(OssClient ossClient) {
        this.ossClient = ossClient;
    }

    public WorkOrderMessageDTO upload(MultipartFile file) throws Exception {
        validateFile(file);
        String fileName = sanitizeFileName(file.getOriginalFilename());
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException("文件名不正确");
        }
        String extension = extension(fileName);
        boolean image = IMAGE_EXTENSIONS.contains(extension);
        if (image && !hasValidImageSignature(file, extension)) {
            throw new BusinessException("图片内容与扩展名不匹配");
        }

        String storagePath = ossClient.upload(file);
        String storageKey = ossClient.extractObjectName(storagePath);
        if (!StringUtils.hasText(storageKey)) {
            throw new BusinessException("附件存储失败");
        }
        return WorkOrderMessageDTO.builder()
                .type(image ? "img" : "file")
                .msg(storagePath)
                .fileName(fileName)
                .fileSize(file.getSize())
                .contentType(contentType(extension))
                .storageKey(storageKey)
                .build();
    }

    public String extractObjectName(WorkOrderMessageDTO message) {
        if (message == null) {
            return null;
        }
        String storageKey = ossClient.extractObjectName(message.getStorageKey());
        return StringUtils.hasText(storageKey) ? storageKey : ossClient.extractObjectName(message.getMsg());
    }

    public StatObjectResponse stat(String objectName) throws Exception {
        return ossClient.getStatObject(objectName);
    }

    public GetObjectResponse open(String objectName) throws Exception {
        return ossClient.getObject(objectName);
    }

    public String contentType(WorkOrderMessageDTO message) {
        if (message != null && StringUtils.hasText(message.getContentType())) {
            return message.getContentType();
        }
        String fileName = message == null ? null : message.getFileName();
        String objectName = extractObjectName(message);
        return contentType(extension(StringUtils.hasText(fileName) ? fileName : objectName));
    }

    public String displayFileName(WorkOrderMessageDTO message) {
        if (message != null && StringUtils.hasText(message.getFileName())) {
            return sanitizeFileName(message.getFileName());
        }
        String objectName = extractObjectName(message);
        if (message != null && "img".equals(message.getType())) {
            String extension = extension(objectName);
            return StringUtils.hasText(extension) ? "工单图片." + extension : "工单图片";
        }
        return StringUtils.hasText(objectName) ? objectName : "工单附件";
    }

    public boolean isAllowedAttachment(WorkOrderMessageDTO message) {
        if (message == null || !("img".equals(message.getType()) || "file".equals(message.getType()))) {
            return false;
        }
        String objectName = extractObjectName(message);
        if (!StringUtils.hasText(objectName)) {
            return false;
        }
        String extension = extension(StringUtils.hasText(message.getFileName()) ? message.getFileName() : objectName);
        return IMAGE_EXTENSIONS.contains(extension) || FILE_EXTENSIONS.contains(extension);
    }

    public String formatSize(Long bytes) {
        if (bytes == null || bytes < 0) {
            return "";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0d);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0d * 1024.0d));
    }

    private void validateFile(MultipartFile file) throws BusinessException {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new BusinessException("附件不能超过 10 MB");
        }
        String fileName = sanitizeFileName(file.getOriginalFilename());
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException("文件名不正确");
        }
        String extension = extension(fileName);
        if (!IMAGE_EXTENSIONS.contains(extension) && !FILE_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持该文件格式");
        }
    }

    String sanitizeFileName(String originalName) {
        String cleanName = StringUtils.cleanPath(originalName == null ? "" : originalName)
                .replace("\r", "")
                .replace("\n", "")
                .replace("\0", "");
        cleanName = StringUtils.getFilename(cleanName);
        if (!StringUtils.hasText(cleanName) || cleanName.contains("..")) {
            return "";
        }
        if (cleanName.length() > 180) {
            String suffix = extension(cleanName);
            int suffixLength = StringUtils.hasText(suffix) ? suffix.length() + 1 : 0;
            cleanName = cleanName.substring(0, 180 - suffixLength) + (suffixLength == 0 ? "" : "." + suffix);
        }
        return cleanName;
    }

    private boolean hasValidImageSignature(MultipartFile file, String extension) throws Exception {
        byte[] header = new byte[12];
        int length;
        try (InputStream inputStream = file.getInputStream()) {
            length = inputStream.read(header);
        }
        if (length < 2) {
            return false;
        }
        switch (extension) {
            case "jpg":
            case "jpeg":
                return unsigned(header[0]) == 0xFF && unsigned(header[1]) == 0xD8 && length >= 3 && unsigned(header[2]) == 0xFF;
            case "png":
                return length >= 8 && Arrays.equals(Arrays.copyOf(header, 8), new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "gif":
                return length >= 6 && (startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a"));
            case "webp":
                return length >= 12 && startsWithAscii(header, "RIFF")
                        && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            case "bmp":
                return header[0] == 'B' && header[1] == 'M';
            default:
                return false;
        }
    }

    private boolean startsWithAscii(byte[] bytes, String value) {
        if (bytes.length < value.length()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (bytes[i] != (byte) value.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private String extension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String contentType(String extension) {
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    private static Map<String, String> buildContentTypes() {
        Map<String, String> types = new HashMap<>();
        types.put("jpg", "image/jpeg");
        types.put("jpeg", "image/jpeg");
        types.put("png", "image/png");
        types.put("gif", "image/gif");
        types.put("webp", "image/webp");
        types.put("bmp", "image/bmp");
        types.put("pdf", "application/pdf");
        types.put("txt", "text/plain");
        types.put("csv", "text/csv");
        types.put("log", "text/plain");
        types.put("md", "text/markdown");
        types.put("json", "application/json");
        types.put("xml", "application/xml");
        types.put("doc", "application/msword");
        types.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        types.put("xls", "application/vnd.ms-excel");
        types.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        types.put("ppt", "application/vnd.ms-powerpoint");
        types.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        types.put("zip", "application/zip");
        types.put("rar", "application/vnd.rar");
        types.put("7z", "application/x-7z-compressed");
        types.put("gz", "application/gzip");
        types.put("tar", "application/x-tar");
        return Collections.unmodifiableMap(types);
    }
}
