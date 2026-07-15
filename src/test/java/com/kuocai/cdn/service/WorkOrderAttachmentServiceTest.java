package com.kuocai.cdn.service;

import com.kuocai.cdn.component.OssClient;
import com.kuocai.cdn.dto.WorkOrderMessageDTO;
import com.kuocai.cdn.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkOrderAttachmentServiceTest {

    private OssClient ossClient;
    private WorkOrderAttachmentService service;

    @BeforeEach
    void setUp() {
        ossClient = mock(OssClient.class);
        service = new WorkOrderAttachmentService(ossClient);
    }

    @Test
    void uploadsImageAndKeepsChineseFileName() throws Exception {
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
        };
        MockMultipartFile file = new MockMultipartFile("fileObj", "截图 测试.png", "image/png", png);
        when(ossClient.upload(file)).thenReturn("/image/abc123.png");
        when(ossClient.extractObjectName("/image/abc123.png")).thenReturn("abc123.png");

        WorkOrderMessageDTO result = service.upload(file);

        assertEquals("img", result.getType());
        assertEquals("截图 测试.png", result.getFileName());
        assertEquals("image/png", result.getContentType());
        assertEquals("abc123.png", result.getStorageKey());
        assertEquals((long) png.length, result.getFileSize());
    }

    @Test
    void uploadsOrdinaryFileAsDownloadAttachment() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "fileObj", "客户资料 2026.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        when(ossClient.upload(file)).thenReturn("/image/def456.txt");
        when(ossClient.extractObjectName("/image/def456.txt")).thenReturn("def456.txt");

        WorkOrderMessageDTO result = service.upload(file);

        assertEquals("file", result.getType());
        assertEquals("客户资料 2026.txt", result.getFileName());
        assertEquals("text/plain", result.getContentType());
        assertEquals("def456.txt", result.getStorageKey());
    }

    @Test
    void rejectsExecutableFile() {
        MockMultipartFile file = new MockMultipartFile(
                "fileObj", "setup.exe", "application/octet-stream", new byte[]{1, 2, 3});

        BusinessException exception = assertThrows(BusinessException.class, () -> service.upload(file));

        assertEquals("不支持该文件格式", exception.getMessage());
        verifyNoInteractions(ossClient);
    }

    @Test
    void rejectsImageWhoseContentDoesNotMatchExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "fileObj", "fake.png", "image/png", "not-an-image".getBytes(StandardCharsets.UTF_8));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.upload(file));

        assertEquals("图片内容与扩展名不匹配", exception.getMessage());
        verifyNoInteractions(ossClient);
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "fileObj", "large.zip", "application/zip",
                new byte[(int) WorkOrderAttachmentService.MAX_ATTACHMENT_SIZE + 1]);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.upload(file));

        assertEquals("附件不能超过 10 MB", exception.getMessage());
        verifyNoInteractions(ossClient);
    }
}
