package com.kuocai.cdn.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.kuocai.cdn.constant.UserConstants;
import com.kuocai.cdn.dao.SysUserDao;
import com.kuocai.cdn.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserServiceAvatarTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void replacesOnlyEmptyAndPreviousDefaultAvatarReferences() {
        SysUserDao dao = mock(SysUserDao.class);
        when(dao.update(isNull(), (Wrapper<SysUser>) org.mockito.ArgumentMatchers.any()))
                .thenReturn(4);

        SysUserService service = new SysUserService();
        ReflectionTestUtils.setField(service, "dao", dao);

        assertEquals(4, service.replaceDefaultAvatarReferences(
                "/images/old-default.png", "/images/new-default.png"));

        ArgumentCaptor<UpdateWrapper> captor = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(dao).update(isNull(), captor.capture());
        UpdateWrapper<SysUser> wrapper = captor.getValue();
        String sql = wrapper.getSqlSegment();
        Collection<Object> values = wrapper.getParamNameValuePairs().values();

        assertTrue(sql.contains("img IS NULL"));
        assertTrue(values.contains(""));
        assertTrue(values.contains(UserConstants.IMG));
        assertTrue(values.contains("/images/old-default.png"));
        assertTrue(values.contains("/images/new-default.png"));
        assertFalse(values.contains("/images/custom-avatar.png"));
    }
}
