package org.adam.lotterysystem.service.impl;

import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.common.utils.RedisUtil;
import org.adam.lotterysystem.service.dto.ActivityDetailDTO;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ActivityServiceImplCacheTest {

    @Test
    void shouldReturnActivityDetailWhenCacheHasText() {
        ActivityServiceImpl activityService = new ActivityServiceImpl();
        RedisUtil redisUtil = Mockito.mock(RedisUtil.class);
        ReflectionTestUtils.setField(activityService, "redisUtil", redisUtil);

        ActivityDetailDTO detailDTO = new ActivityDetailDTO();
        detailDTO.setActivityId(196L);
        detailDTO.setActivityName("测试33");
        detailDTO.setDescription("cache");
        detailDTO.setStatus(ActivityStatusEnum.RUNNING);
        Mockito.when(redisUtil.get("ACTIVITY_196"))
                .thenReturn(JacksonUtil.writeValueAsString(detailDTO));

        ActivityDetailDTO cached = (ActivityDetailDTO) ReflectionTestUtils
                .invokeMethod(activityService, "getActivityFromCache", 196L);

        assertNotNull(cached);
        assertEquals(196L, cached.getActivityId());
        assertEquals("测试33", cached.getActivityName());
        assertEquals(ActivityStatusEnum.RUNNING, cached.getStatus());
    }
}
