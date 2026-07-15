package com.kuocai.cdn.controller.rest;

import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.service.WorkOrderAttachmentService;
import com.kuocai.cdn.service.WorkOrderMessageService;
import com.kuocai.cdn.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkOrderMessageControllerTest {

    private WorkOrderMessageController controller;
    private WorkOrderMessageService messageService;
    private WorkOrderAttachmentService attachmentService;
    private WorkOrderService workOrderService;

    @BeforeEach
    void setUp() {
        controller = new WorkOrderMessageController();
        messageService = mock(WorkOrderMessageService.class);
        attachmentService = mock(WorkOrderAttachmentService.class);
        workOrderService = mock(WorkOrderService.class);
        ReflectionTestUtils.setField(controller, "service", messageService);
        ReflectionTestUtils.setField(controller, "attachmentService", attachmentService);
        ReflectionTestUtils.setField(controller, "workOrderService", workOrderService);
        controller.setLoginUserId(100L);
        controller.setLoginUserRoleCode("user");
    }

    @Test
    void rejectsAttachmentFromAnotherUsersWorkOrder() throws Exception {
        WorkOrder workOrder = WorkOrder.builder().id(10L).userId(200L).build();
        when(workOrderService.queryById(10L)).thenReturn(workOrder);
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.attachment(10L, "abc123.png", false, response);

        assertEquals(403, response.getStatus());
        verifyNoInteractions(messageService, attachmentService);
    }

    @Test
    void returnsNotFoundForMissingWorkOrder() throws Exception {
        when(workOrderService.queryById(99L)).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.attachment(99L, "abc123.png", false, response);

        assertEquals(404, response.getStatus());
        verifyNoInteractions(messageService, attachmentService);
    }
}
