package org.adam.lotterysystem;

import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.common.utils.RedisUtil;
import org.adam.lotterysystem.controller.param.CreateActivityParam;
import org.adam.lotterysystem.controller.param.CreatePrizeByActivityParam;
import org.adam.lotterysystem.controller.param.CreateUserByActivityParam;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.controller.param.ExecuteDrawPrizeParam;
import org.adam.lotterysystem.controller.param.ShowWinningRecordsParam;
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
import org.adam.lotterysystem.service.dto.DrawPrizeDTO;
import org.adam.lotterysystem.service.dto.WinningRecordDTO;
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@Transactional
public class DrawPrizeTest {

    private static final Long PRIZE_ID = 18L;
    private static final Long USER_ID = 43L;
    private static final String USER_NAME = "lisi";
    private static final Long SECOND_USER_ID = 44L;
    private static final String SECOND_USER_NAME = "wangwu";
    private static final String DRAW_PROCESSING_KEY_PREFIX = "DrawPrizeExecuting_";

    @Autowired
    private DrawPrizeService drawPrizeService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RedisUtil redisUtil;

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
    void testExecuteDrawReturnsServerSelectedWinners() {
        Long activityId = createTestActivity(
                "execute-draw-success-test-",
                1L,
                List.of(buildActivityUser(USER_ID, USER_NAME)));

        ExecuteDrawPrizeParam param = buildExecuteDrawParam(activityId);

        DrawPrizeDTO result = drawPrizeService.executeDraw(param);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(activityId, result.getActivityId()),
                () -> assertEquals(PRIZE_ID, result.getPrizeId()),
                () -> assertEquals(1, result.getWinnerList().size()),
                () -> assertEquals(USER_ID, result.getWinnerList().get(0).getUserId()),
                () -> assertEquals(USER_NAME, result.getWinnerList().get(0).getUserName()),
                () -> assertNotNull(result.getWinningTime())
        );
    }

    @Test
    void testExecuteDrawReturnsProcessingWhenLockExists() {
        Long activityId = createTestActivity(
                "execute-draw-processing-test-",
                1L,
                List.of(buildActivityUser(USER_ID, USER_NAME)));

        String lockKey = buildDrawProcessingKey(activityId, PRIZE_ID);
        redisUtil.set(lockKey, "occupied", 30L);

        try {
            ExecuteDrawPrizeParam param = buildExecuteDrawParam(activityId);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> drawPrizeService.executeDraw(param));

            assertEquals(ServiceErrorCodeConstants.DRAW_PRIZE_PROCESSING.getCode(), exception.getCode());
        } finally {
            redisUtil.del(lockKey);
        }
    }

    @Test
    void testExecuteDrawRejectsRepeatedPrizeDraw() {
        Long activityId = createTestActivity(
                "execute-draw-repeat-test-",
                1L,
                List.of(buildActivityUser(USER_ID, USER_NAME)));

        ExecuteDrawPrizeParam param = buildExecuteDrawParam(activityId);

        drawPrizeService.executeDraw(param);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> drawPrizeService.executeDraw(param));

        List<WinningRecordDO> winningRecords = winningRecordMapper.selectByActivityId(activityId);

        assertAll(
                () -> assertEquals(ServiceErrorCodeConstants.DRAW_PRIZE_END.getCode(), exception.getCode()),
                () -> assertEquals(1, winningRecords.size(), "重复抽奖不应删除已有中奖记录")
        );
    }

    @Test
    void testExecuteDrawRejectsWhenNotEnoughEligibleUsers() {
        Long activityId = createTestActivity(
                "execute-draw-not-enough-test-",
                2L,
                List.of(
                        buildActivityUser(USER_ID, USER_NAME),
                        buildActivityUser(SECOND_USER_ID, SECOND_USER_NAME)));
        activityUserMapper.batchUpdateStatus(
                activityId,
                List.of(SECOND_USER_ID),
                ActivityUSerStatusEnum.END.name());

        ExecuteDrawPrizeParam param = buildExecuteDrawParam(activityId);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> drawPrizeService.executeDraw(param));

        assertEquals(ServiceErrorCodeConstants.DRAW_PRIZE_CANDIDATE_NOT_ENOUGH.getCode(), exception.getCode());
    }

    @Test
    void testExecuteDrawReturnsLockErrorWhenRedisUnavailable() {
        Long activityId = createTestActivity(
                "execute-draw-lock-error-test-",
                1L,
                List.of(buildActivityUser(USER_ID, USER_NAME)));

        RedisUtil originalRedisUtil = (RedisUtil) ReflectionTestUtils.getField(drawPrizeService, "redisUtil");
        RedisUtil mockRedisUtil = Mockito.mock(RedisUtil.class);
        Mockito.when(mockRedisUtil.setIfAbsent(anyString(), anyString(), anyLong()))
                .thenThrow(new IllegalStateException("redis unavailable"));

        try {
            ReflectionTestUtils.setField(drawPrizeService, "redisUtil", mockRedisUtil);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> drawPrizeService.executeDraw(buildExecuteDrawParam(activityId)));

            assertEquals(ServiceErrorCodeConstants.DRAW_PRIZE_LOCK_ERROR.getCode(), exception.getCode());
        } finally {
            ReflectionTestUtils.setField(drawPrizeService, "redisUtil", originalRedisUtil);
        }
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
        Long activityId = createTestActivity("save-winner-records-test-");
        DrawPrizeParam param = buildDrawPrizeParam(activityId);

        List<WinningRecordDO> savedRecords = drawPrizeService.saveWinnerRecords(param);
        List<WinningRecordDO> persistedRecords = winningRecordMapper.selectByActivityId(activityId);

        assertAll(
                () -> assertEquals(1, savedRecords.size(), "应只保存一条中奖记录"),
                () -> assertEquals(1, persistedRecords.size(), "数据库中应只有一条中奖记录"),
                () -> assertEquals(USER_ID, persistedRecords.get(0).getWinnerId(), "中奖用户应与测试数据一致"),
                () -> assertEquals(activityId, persistedRecords.get(0).getActivityId(), "活动 ID 应与新建活动一致"),
                () -> assertEquals(PRIZE_ID, persistedRecords.get(0).getPrizeId(), "奖品 ID 应与测试数据一致")
        );
    }

    private Long createTestActivity(String activityNamePrefix) {
        return createTestActivity(
                activityNamePrefix,
                1L,
                List.of(buildActivityUser(USER_ID, USER_NAME)));
    }

    private Long createTestActivity(String activityNamePrefix,
                                    Long prizeAmount,
                                    List<CreateUserByActivityParam> userParams) {
        CreateActivityParam createActivityParam = new CreateActivityParam();
        createActivityParam.setActivityName(activityNamePrefix + System.currentTimeMillis());
        createActivityParam.setDescription(activityNamePrefix + " activity");

        CreatePrizeByActivityParam prizeParam = new CreatePrizeByActivityParam();
        prizeParam.setPrizeId(PRIZE_ID);
        prizeParam.setPrizeAmount(prizeAmount);
        prizeParam.setPrizeTiers(ActivityPrizeTiersEnum.FIRST_PRIZE.name());
        createActivityParam.setActivityPrizeList(List.of(prizeParam));
        createActivityParam.setActivityUserList(userParams);

        CreateActivityDTO dto = activityService.createActivity(createActivityParam);
        return dto.getActivityId();
    }

    private ExecuteDrawPrizeParam buildExecuteDrawParam(Long activityId) {
        ExecuteDrawPrizeParam param = new ExecuteDrawPrizeParam();
        param.setActivityId(activityId);
        param.setPrizeId(PRIZE_ID);
        return param;
    }

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

    private CreateUserByActivityParam buildActivityUser(Long userId, String userName) {
        CreateUserByActivityParam userParam = new CreateUserByActivityParam();
        userParam.setUserId(userId);
        userParam.setUserName(userName);
        return userParam;
    }

    private String buildDrawProcessingKey(Long activityId, Long prizeId) {
        return DRAW_PROCESSING_KEY_PREFIX + activityId + "_" + prizeId;
    }

    private Map<String, String> buildMqMessage(DrawPrizeParam param) {
        return Map.of(
                "messageId", UUID.randomUUID().toString(),
                "messageData", JacksonUtil.writeValueAsString(param)
        );
    }

    private void assertStatusRolledBack(Long activityId) {
        ActivityDO afterActivity = activityMapper.selectById(activityId);
        ActivityPrizeDO afterPrize = activityPrizeMapper.selectByActivityPrizeId(activityId, PRIZE_ID);
        List<ActivityUserDO> afterUserList = activityUserMapper.batchSelectByActivityUserIds(activityId, List.of(USER_ID));
        List<WinningRecordDO> winningRecords = winningRecordMapper.selectByActivityId(activityId);

        assertAll(
                () -> assertEquals(ActivityStatusEnum.RUNNING.name(), afterActivity.getStatus(),
                        "活动状态应回滚为 RUNNING"),
                () -> assertEquals(ActivityPrizeStatusEnum.INIT.name(), afterPrize.getStatus(),
                        "奖品状态应回滚为 INIT"),
                () -> assertEquals(ActivityUSerStatusEnum.INIT.name(), afterUserList.get(0).getStatus(),
                        "人员状态应回滚为 INIT"),
                () -> assertEquals(0, winningRecords.size(), "中奖记录应被清除")
        );
    }

    @Test
    void testDrawPrizeHappyPath() {
        Long activityId = createTestActivity("draw-test-");
        DrawPrizeParam param = buildDrawPrizeParam(activityId);

        mqReceiver.process(buildMqMessage(param));

        List<WinningRecordDO> winningRecords = winningRecordMapper.selectByActivityId(activityId);
        assertNotNull(winningRecords);
        assertFalse(winningRecords.isEmpty(), "中奖记录不应为空");

        ActivityPrizeDO afterPrize = activityPrizeMapper.selectByActivityPrizeId(activityId, PRIZE_ID);
        assertEquals(ActivityPrizeStatusEnum.END.name(), afterPrize.getStatus(), "奖品状态应为 END");

        List<ActivityUserDO> afterUserList = activityUserMapper.batchSelectByActivityUserIds(activityId, List.of(USER_ID));
        assertFalse(afterUserList.isEmpty());
        assertEquals(ActivityUSerStatusEnum.END.name(), afterUserList.get(0).getStatus(), "人员状态应为 END");
    }

    @Test
    void testDrawPrizeRollbackWhenSaveWinnerRecordsThrows() {
        Long activityId = createTestActivity("draw-rollback-test-");
        DrawPrizeParam param = buildDrawPrizeParam(activityId);
        Map<String, String> message = buildMqMessage(param);

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

    @Test
    void testMessageRedeliveryViaDlxWhenProcessFails() {
        Long activityId = createTestActivity("draw-dlx-retry-test-");
        DrawPrizeParam param = buildDrawPrizeParam(activityId);
        Map<String, String> message = buildMqMessage(param);

        DrawPrizeService spyService = Mockito.spy(drawPrizeService);
        Mockito.doThrow(new ServiceException(500, "mock: 保存中奖记录时数据库连接超时"))
                .doCallRealMethod()
                .when(spyService).saveWinnerRecords(any());

        DrawPrizeService originalService = (DrawPrizeService) ReflectionTestUtils.getField(mqReceiver, "drawPrizeService");
        try {
            ReflectionTestUtils.setField(mqReceiver, "drawPrizeService", spyService);

            assertThrows(ServiceException.class, () -> mqReceiver.process(message),
                    "第一次处理应抛出异常，触发消息进入死信队列");

            assertStatusRolledBack(activityId);

            mqReceiver.process(message);

            List<WinningRecordDO> winningRecords = winningRecordMapper.selectByActivityId(activityId);
            assertNotNull(winningRecords);
            assertFalse(winningRecords.isEmpty(), "重发后中奖记录应已持久化");

            ActivityPrizeDO afterPrize = activityPrizeMapper.selectByActivityPrizeId(activityId, PRIZE_ID);
            assertEquals(ActivityPrizeStatusEnum.END.name(), afterPrize.getStatus(), "重发后奖品状态应为 END");

            List<ActivityUserDO> afterUserList = activityUserMapper.batchSelectByActivityUserIds(activityId, List.of(USER_ID));
            assertFalse(afterUserList.isEmpty());
            assertEquals(ActivityUSerStatusEnum.END.name(), afterUserList.get(0).getStatus(), "重发后人员状态应为 END");
        } finally {
            ReflectionTestUtils.setField(mqReceiver, "drawPrizeService", originalService);
        }
    }

    @Test
    void testMqReceiverDelegatesToFinalizeDraw() {
        Long activityId = createTestActivity(
                "mq-finalize-delegate-test-",
                1L,
                List.of(buildActivityUser(USER_ID, USER_NAME)));
        DrawPrizeParam param = buildDrawPrizeParam(activityId);
        Map<String, String> message = buildMqMessage(param);

        DrawPrizeService spyService = Mockito.spy(drawPrizeService);
        DrawPrizeService originalService = (DrawPrizeService) ReflectionTestUtils.getField(mqReceiver, "drawPrizeService");
        try {
            ReflectionTestUtils.setField(mqReceiver, "drawPrizeService", spyService);

            mqReceiver.process(message);

            Mockito.verify(spyService, Mockito.times(1)).finalizeDraw(any(DrawPrizeParam.class));
        } finally {
            ReflectionTestUtils.setField(mqReceiver, "drawPrizeService", originalService);
        }
    }

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

    @Test
    void testFinalizeDrawRollbackWhenSaveWinnerRecordsThrowsServiceException() {
        Long activityId = createTestActivity(
                "finalize-draw-rollback-test-",
                1L,
                List.of(buildActivityUser(USER_ID, USER_NAME)));
        DrawPrizeParam param = buildDrawPrizeParam(activityId);

        DrawPrizeService spyService = Mockito.spy(drawPrizeService);
        Mockito.doThrow(new ServiceException(500, "mock save winner records failure"))
                .when(spyService).saveWinnerRecords(any());

        ServiceException exception = assertThrows(ServiceException.class, () -> spyService.finalizeDraw(param));

        assertEquals(500, exception.getCode());
        assertStatusRolledBack(activityId);
    }

    @Test
    void testCheckDrawPrizeStatusShouldFailWhenActivityEnded() {
        Long activityId = createTestActivity("draw-status-activity-end-test-");
        activityMapper.updateStatus(activityId, ActivityStatusEnum.END.name());

        DrawPrizeParam param = buildDrawPrizeParam(activityId);

        assertFalse(drawPrizeService.checkDrawPrizeStatus(param), "活动已结束时应拦截抽奖请求");
    }

    @Test
    void testCheckDrawPrizeStatusShouldFailWhenPrizeEnded() {
        Long activityId = createTestActivity("draw-status-prize-end-test-");
        activityPrizeMapper.updateStatus(activityId, PRIZE_ID, ActivityPrizeStatusEnum.END.name());

        DrawPrizeParam param = buildDrawPrizeParam(activityId);

        assertFalse(drawPrizeService.checkDrawPrizeStatus(param), "奖品已抽完时应拦截抽奖请求");
    }

    @Test
    void testCheckDrawPrizeStatusShouldFailWhenWinnerCountMismatch() {
        Long activityId = createTestActivity("draw-status-winner-count-test-");
        DrawPrizeParam param = buildDrawPrizeParam(activityId);

        DrawPrizeParam.Winner extraWinner = new DrawPrizeParam.Winner();
        extraWinner.setUserId(999L);
        extraWinner.setUserName("extra-user");
        param.setWinnerList(List.of(param.getWinnerList().get(0), extraWinner));

        assertFalse(drawPrizeService.checkDrawPrizeStatus(param), "中奖人数与奖项数量不一致时应拦截抽奖请求");
    }

    @Test
    void testShowWinningRecords() {
        Long activityId = createTestActivity("show-winning-records-test-");
        List<WinningRecordDO> savedRecords = drawPrizeService.saveWinnerRecords(buildDrawPrizeParam(activityId));

        ShowWinningRecordsParam param = new ShowWinningRecordsParam();
        param.setActivityId(activityId);
        List<WinningRecordDTO> winningRecordDTOS = drawPrizeService.getRecords(param);

        assertAll(
                () -> assertEquals(1, winningRecordDTOS.size(), "应查询到一条中奖记录"),
                () -> assertEquals(savedRecords.get(0).getWinnerId(), winningRecordDTOS.get(0).getWinnerId(), "中奖用户 ID 应一致"),
                () -> assertEquals(savedRecords.get(0).getWinnerName(), winningRecordDTOS.get(0).getWinnerName(), "中奖用户名应一致"),
                () -> assertEquals(savedRecords.get(0).getPrizeName(), winningRecordDTOS.get(0).getPrizeName(), "奖品名称应一致"),
                () -> assertEquals(ActivityPrizeTiersEnum.forName(savedRecords.get(0).getPrizeTier()),
                        winningRecordDTOS.get(0).getPrizeTier(), "奖品等级应一致")
        );
    }
}
