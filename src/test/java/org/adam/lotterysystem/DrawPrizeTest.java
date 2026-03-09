package org.adam.lotterysystem;

import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.controller.param.CreateActivityParam;
import org.adam.lotterysystem.controller.param.CreatePrizeByActivityParam;
import org.adam.lotterysystem.controller.param.CreateUserByActivityParam;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.dao.dataobject.ActivityDO;
import org.adam.lotterysystem.dao.dataobject.ActivityPrizeDO;
import org.adam.lotterysystem.dao.dataobject.ActivityUserDO;
import org.adam.lotterysystem.dao.dataobject.WinningRecordDO;
import org.adam.lotterysystem.dao.mapper.ActivityMapper;
import org.adam.lotterysystem.dao.mapper.ActivityPrizeMapper;
import org.adam.lotterysystem.dao.mapper.ActivityUserMapper;
import org.adam.lotterysystem.dao.mapper.WinningRecordMapper;
import org.adam.lotterysystem.service.ActivityService;
import org.adam.lotterysystem.service.DrawPrizeService;
import org.adam.lotterysystem.service.activitystatus.ActivityStatusManager;
import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.adam.lotterysystem.service.dto.CreateActivityDTO;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityPrizeTiersEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityUSerStatusEnum;
import org.adam.lotterysystem.service.mq.MqReceiver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@Transactional
public class DrawPrizeTest {

    private static final Long PRIZE_ID = 18L;
    private static final Long USER_ID = 43L;
    private static final String USER_NAME = "lisi";

    @Autowired
    private DrawPrizeService drawPrizeService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ActivityStatusManager activityStatusManager;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;

    @Autowired
    private ActivityUserMapper activityUserMapper;

    @Autowired
    private WinningRecordMapper winningRecordMapper;

    @Autowired
    private MqReceiver mqReceiver;

    @Test
    void drawPrizeTest() {
        DrawPrizeParam param = new DrawPrizeParam();
        param.setActivityId(1L);
        param.setPrizeId(1L);
        param.setWinningTime(new Date());

        DrawPrizeParam.Winner winner = new DrawPrizeParam.Winner();
        winner.setUserId(1L);
        winner.setUserName("zhangsan");
        param.setWinnerList(List.of(winner));

        drawPrizeService.drawPrize(param);
    }

    @Test
    void testConvertActivityStatus() {
        Long userId = 44L;

        CreateActivityParam createActivityParam = new CreateActivityParam();
        createActivityParam.setActivityName("status-test-" + System.currentTimeMillis());
        createActivityParam.setDescription("status test activity");

        CreatePrizeByActivityParam prizeParam = new CreatePrizeByActivityParam();
        prizeParam.setPrizeId(PRIZE_ID);
        prizeParam.setPrizeAmount(1L);
        prizeParam.setPrizeTiers(ActivityPrizeTiersEnum.FIRST_PRIZE.name());
        createActivityParam.setActivityPrizeList(List.of(prizeParam));

        CreateUserByActivityParam userParam = new CreateUserByActivityParam();
        userParam.setUserId(userId);
        userParam.setUserName("wangwu");
        createActivityParam.setActivityUserList(List.of(userParam));

        CreateActivityDTO createActivityDTO = activityService.createActivity(createActivityParam);
        Long activityId = createActivityDTO.getActivityId();

        ActivityDO beforeActivity = activityMapper.selectById(activityId);
        ActivityPrizeDO beforePrize = activityPrizeMapper.selectByActivityPrizeId(activityId, PRIZE_ID);
        List<ActivityUserDO> beforeUserList = activityUserMapper.batchSelectByActivityUserIds(activityId, List.of(userId));

        assertNotNull(beforeActivity);
        assertNotNull(beforePrize);
        assertFalse(beforeUserList.isEmpty());
        assertEquals(ActivityStatusEnum.RUNNING.name(), beforeActivity.getStatus());
        assertEquals(ActivityPrizeStatusEnum.INIT.name(), beforePrize.getStatus());
        assertEquals(ActivityUSerStatusEnum.INIT.name(), beforeUserList.get(0).getStatus());

        ConvertActivityStatusDTO convertActivityStatusDTO = new ConvertActivityStatusDTO();
        convertActivityStatusDTO.setActivityId(activityId);
        convertActivityStatusDTO.setTargetActivityStatus(ActivityStatusEnum.END);
        convertActivityStatusDTO.setPrizeId(PRIZE_ID);
        convertActivityStatusDTO.setTargetPrizeStatus(ActivityPrizeStatusEnum.END);
        convertActivityStatusDTO.setUserIds(List.of(userId));
        convertActivityStatusDTO.setTargetUserStatus(ActivityUSerStatusEnum.END);
        activityStatusManager.handlerEvent(convertActivityStatusDTO);

        ActivityDO afterActivity = activityMapper.selectById(activityId);
        ActivityPrizeDO afterPrize = activityPrizeMapper.selectByActivityPrizeId(activityId, PRIZE_ID);
        List<ActivityUserDO> afterUserList = activityUserMapper.batchSelectByActivityUserIds(activityId, List.of(userId));

        assertNotNull(afterActivity);
        assertNotNull(afterPrize);
        assertFalse(afterUserList.isEmpty());
        assertEquals(ActivityStatusEnum.END.name(), afterActivity.getStatus());
        assertEquals(ActivityPrizeStatusEnum.END.name(), afterPrize.getStatus());
        assertEquals(ActivityUSerStatusEnum.END.name(), afterUserList.get(0).getStatus());
    }

    @Test
    void testSaveWinnerRecords() {
        DrawPrizeParam param = new DrawPrizeParam();
        param.setActivityId(24L);
        param.setPrizeId(19L);
        param.setWinningTime(new Date());
        List<DrawPrizeParam.Winner> winnerList = new ArrayList<>();
        DrawPrizeParam.Winner winner = new DrawPrizeParam.Winner();
        winner.setUserId(43L);
        winner.setUserName("lisi");
        winnerList.add(winner);
        param.setWinnerList(winnerList);
        drawPrizeService.saveWinnerRecords(param);
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试活动并返回活动ID
     */
    private Long createTestActivity(String activityNamePrefix) {
        CreateActivityParam createActivityParam = new CreateActivityParam();
        createActivityParam.setActivityName(activityNamePrefix + System.currentTimeMillis());
        createActivityParam.setDescription(activityNamePrefix + " activity");

        CreatePrizeByActivityParam prizeParam = new CreatePrizeByActivityParam();
        prizeParam.setPrizeId(PRIZE_ID);
        prizeParam.setPrizeAmount(1L);
        prizeParam.setPrizeTiers(ActivityPrizeTiersEnum.FIRST_PRIZE.name());
        createActivityParam.setActivityPrizeList(List.of(prizeParam));

        CreateUserByActivityParam userParam = new CreateUserByActivityParam();
        userParam.setUserId(USER_ID);
        userParam.setUserName(USER_NAME);
        createActivityParam.setActivityUserList(List.of(userParam));

        CreateActivityDTO dto = activityService.createActivity(createActivityParam);
        return dto.getActivityId();
    }

    /**
     * 构造抽奖参数
     */
    private DrawPrizeParam buildDrawPrizeParam(Long activityId) {
        DrawPrizeParam param = new DrawPrizeParam();
        param.setActivityId(activityId);
        param.setPrizeId(PRIZE_ID);
        param.setWinningTime(new Date());

        DrawPrizeParam.Winner winner = new DrawPrizeParam.Winner();
        winner.setUserId(USER_ID);
        winner.setUserName(USER_NAME);
        param.setWinnerList(List.of(winner));
        return param;
    }

    /**
     * 构造MQ消息
     */
    private Map<String, String> buildMqMessage(DrawPrizeParam param) {
        return Map.of(
                "messageId", UUID.randomUUID().toString(),
                "messageData", JacksonUtil.writeValueAsString(param)
        );
    }

    /**
     * 验证状态已回滚至初始状态
     */
    private void assertStatusRolledBack(Long activityId) {
        ActivityDO afterActivity = activityMapper.selectById(activityId);
        ActivityPrizeDO afterPrize = activityPrizeMapper.selectByActivityPrizeId(activityId, PRIZE_ID);
        List<ActivityUserDO> afterUserList = activityUserMapper.batchSelectByActivityUserIds(activityId, List.of(USER_ID));
        List<WinningRecordDO> winningRecords = winningRecordMapper.selectByActivityId(activityId);

        assertAll(
                () -> assertEquals(ActivityStatusEnum.RUNNING.name(), afterActivity.getStatus(),
                        "活动状态应回滚为RUNNING"),
                () -> assertEquals(ActivityPrizeStatusEnum.INIT.name(), afterPrize.getStatus(),
                        "奖品状态应回滚为INIT"),
                () -> assertEquals(ActivityUSerStatusEnum.INIT.name(), afterUserList.get(0).getStatus(),
                        "人员状态应回滚为INIT"),
                () -> assertEquals(0, winningRecords.size(),
                        "中奖记录应被清除")
        );
    }

    // ==================== 集成测试：正向流程 ====================

    /**
     * 测试抽奖正向流程：绕过MQ，直接同步调用消费者处理方法
     */
    @Test
    void testDrawPrizeHappyPath() {
        Long activityId = createTestActivity("draw-test-");
        DrawPrizeParam param = buildDrawPrizeParam(activityId);

        mqReceiver.process(buildMqMessage(param));

        // 验证：中奖记录已持久化
        List<WinningRecordDO> winningRecords = winningRecordMapper.selectByActivityId(activityId);
        assertNotNull(winningRecords);
        assertFalse(winningRecords.isEmpty(), "中奖记录不应为空");

        // 验证：状态已扭转
        ActivityPrizeDO afterPrize = activityPrizeMapper.selectByActivityPrizeId(activityId, PRIZE_ID);
        assertEquals(ActivityPrizeStatusEnum.END.name(), afterPrize.getStatus(), "奖品状态应为END");

        List<ActivityUserDO> afterUserList = activityUserMapper.batchSelectByActivityUserIds(activityId, List.of(USER_ID));
        assertFalse(afterUserList.isEmpty());
        assertEquals(ActivityUSerStatusEnum.END.name(), afterUserList.get(0).getStatus(), "人员状态应为END");
    }

    // ==================== 集成测试：异常回滚 ====================

    /**
     * 测试saveWinnerRecords抛出未知异常时，状态和中奖记录回滚
     */
    @Test
    void testDrawPrizeRollbackWhenSaveWinnerRecordsThrows() {
        Long activityId = createTestActivity("draw-rollback-test-");
        DrawPrizeParam param = buildDrawPrizeParam(activityId);
        Map<String, String> message = buildMqMessage(param);

        // 用Mockito spy替换，仅让saveWinnerRecords抛出异常
        DrawPrizeService spyService = Mockito.spy(drawPrizeService);
        Mockito.doThrow(new IllegalStateException("mock save winner records failure"))
                .when(spyService).saveWinnerRecords(any());

        DrawPrizeService originalService = (DrawPrizeService) ReflectionTestUtils.getField(mqReceiver, "drawPrizeService");
        try {
            ReflectionTestUtils.setField(mqReceiver, "drawPrizeService", spyService);

            assertThrows(IllegalStateException.class, () -> mqReceiver.process(message));

            assertStatusRolledBack(activityId);
        } finally {
            ReflectionTestUtils.setField(mqReceiver, "drawPrizeService", originalService);
        }
    }

    /**
     * 测试saveWinnerRecords抛出ServiceException时，状态和中奖记录回滚
     */
    @Test
    void testDrawPrizeRollbackWhenSaveWinnerRecordsThrowsServiceException() {
        Long activityId = createTestActivity("draw-rollback-svc-test-");
        DrawPrizeParam param = buildDrawPrizeParam(activityId);
        Map<String, String> message = buildMqMessage(param);

        DrawPrizeService spyService = Mockito.spy(drawPrizeService);
        Mockito.doThrow(new ServiceException(500, "mock service exception"))
                .when(spyService).saveWinnerRecords(any());

        DrawPrizeService originalService = (DrawPrizeService) ReflectionTestUtils.getField(mqReceiver, "drawPrizeService");
        try {
            ReflectionTestUtils.setField(mqReceiver, "drawPrizeService", spyService);

            assertThrows(ServiceException.class, () -> mqReceiver.process(message));

            assertStatusRolledBack(activityId);
        } finally {
            ReflectionTestUtils.setField(mqReceiver, "drawPrizeService", originalService);
        }
    }

}
