package org.adam.lotterysystem;

import org.adam.lotterysystem.controller.param.CreateActivityParam;
import org.adam.lotterysystem.controller.param.CreatePrizeByActivityParam;
import org.adam.lotterysystem.controller.param.CreateUserByActivityParam;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.dao.dataobject.ActivityDO;
import org.adam.lotterysystem.dao.dataobject.ActivityPrizeDO;
import org.adam.lotterysystem.dao.dataobject.ActivityUserDO;
import org.adam.lotterysystem.dao.mapper.ActivityMapper;
import org.adam.lotterysystem.dao.mapper.ActivityPrizeMapper;
import org.adam.lotterysystem.dao.mapper.ActivityUserMapper;
import org.adam.lotterysystem.service.ActivityService;
import org.adam.lotterysystem.service.DrawPrizeService;
import org.adam.lotterysystem.service.activitystatus.ActivityStatusManager;
import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.adam.lotterysystem.service.dto.CreateActivityDTO;
import org.adam.lotterysystem.service.enums.ActivityPrizeTiersEnum;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityUSerStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class DrawPrizeTest {

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

    @Test
    void drawPrizeTest() {

        DrawPrizeParam param = new DrawPrizeParam();
        param.setActivityId(1L);
        param.setPrizeId(1L);
        param.setPrizeTiers("一等奖");
        param.setWinningTime(new Date());

        List<DrawPrizeParam.Winner> winnerList = new ArrayList<>();
        DrawPrizeParam.Winner winner = new DrawPrizeParam.Winner();
        winner.setUserId(1L);
        winner.setUserName("张三");
        winnerList.add(winner);
        param.setWinnerList(winnerList);
        drawPrizeService.drawPrize(param);
    }

    @Test
    void testConvertActivityStatus() {
        Long prizeId = 18L;
        Long userId = 44L;

        CreateActivityParam createActivityParam = new CreateActivityParam();
        createActivityParam.setActivityName("状态扭转测试-" + System.currentTimeMillis());
        createActivityParam.setDescription("状态扭转测试活动");

        CreatePrizeByActivityParam prizeParam = new CreatePrizeByActivityParam();
        prizeParam.setPrizeId(prizeId);
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
        ActivityPrizeDO beforePrize = activityPrizeMapper.selectByActivityPrizeId(activityId, prizeId);
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
        convertActivityStatusDTO.setPrizeId(prizeId);
        convertActivityStatusDTO.setTargetPrizeStatus(ActivityPrizeStatusEnum.END);
        List<Long> userIds = List.of(userId);
        convertActivityStatusDTO.setUserIds(userIds);
        convertActivityStatusDTO.setTargetUserStatus(ActivityUSerStatusEnum.END);
        activityStatusManager.handlerEvent(convertActivityStatusDTO);

        ActivityDO afterActivity = activityMapper.selectById(activityId);
        ActivityPrizeDO afterPrize = activityPrizeMapper.selectByActivityPrizeId(activityId, prizeId);
        List<ActivityUserDO> afterUserList = activityUserMapper.batchSelectByActivityUserIds(activityId, List.of(userId));

        assertNotNull(afterActivity);
        assertNotNull(afterPrize);
        assertFalse(afterUserList.isEmpty());
        assertEquals(ActivityStatusEnum.END.name(), afterActivity.getStatus());
        assertEquals(ActivityPrizeStatusEnum.END.name(), afterPrize.getStatus());
        assertEquals(ActivityUSerStatusEnum.END.name(), afterUserList.get(0).getStatus());
    }
}
