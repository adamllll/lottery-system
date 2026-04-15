package org.adam.lotterysystem;

import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.controller.result.GetActivityDetailResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityDetailSerializationTest {

    @Test
    void shouldSerializePrizeFieldsUsedByDrawPage() {
        GetActivityDetailResult.Prize prize = new GetActivityDetailResult.Prize();
        prize.setId(196L);
        prize.setName("美女");
        prize.setTiers("一等奖");
        prize.setPrizeAmount(1L);

        String json = JacksonUtil.writeValueAsString(prize);

        assertTrue(json.contains("\"id\":196"));
        assertTrue(json.contains("\"name\":\"美女\""));
        assertTrue(json.contains("\"tiers\":\"一等奖\""));
        assertTrue(json.contains("\"prizeAmount\":1"));
    }

    @Test
    void shouldSerializeUserNameForDrawPage() {
        GetActivityDetailResult.User user = new GetActivityDetailResult.User();
        user.setUserId(40L);
        user.setUserName("测试用户");

        String json = JacksonUtil.writeValueAsString(user);
        GetActivityDetailResult.User parsed =
                JacksonUtil.readValue(json, GetActivityDetailResult.User.class);

        assertTrue(json.contains("\"userId\":40"));
        assertTrue(json.contains("\"userName\":\"测试用户\""));
        assertFalse(json.contains("\"UserName\""));
        assertEquals("测试用户", parsed.getUserName());
    }
}
