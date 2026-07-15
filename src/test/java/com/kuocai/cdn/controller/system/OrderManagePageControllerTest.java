package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderManagePageControllerTest {

    private OrderManagePageController controller;
    private WorkOrderService workOrderService;

    @BeforeEach
    void setUp() {
        controller = new OrderManagePageController();
        workOrderService = mock(WorkOrderService.class);
        ReflectionTestUtils.setField(controller, "workOrderService", workOrderService);
        controller.setLoginUserId(100L);
        controller.setLoginUserRoleCode("user");
    }

    @Test
    void redirectsWhenUserDoesNotOwnWorkOrder() {
        when(workOrderService.queryById(10L))
                .thenReturn(WorkOrder.builder().id(10L).userId(200L).build());

        String view = controller.orderInfo(10L, new HashMap<>());

        assertEquals("redirect:/order-list", view);
    }

    @Test
    void redirectsWhenWorkOrderDoesNotExist() {
        when(workOrderService.queryById(99L)).thenReturn(null);

        String view = controller.orderInfo(99L, new HashMap<>());

        assertEquals("redirect:/order-list", view);
    }
}
