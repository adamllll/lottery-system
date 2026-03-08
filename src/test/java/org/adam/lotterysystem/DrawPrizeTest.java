package org.adam.lotterysystem;

import org.adam.lotterysystem.controller.param.CreateActivityParam;
import org.adam.lotterysystem.controller.param.CreatePrizeByActivityParam;
import org.adam.lotterysystem.controller.param.CreateUserByActivityParam;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.dao.dataobject.ActivityDO;
import org.adam.lotterysystem.dao.dataobject.ActivityPrizeDO;
import org.adam.lotterysystem.dao.dataobject.ActivityUserDO;
import org.adam.lotterysystem.dao.dataobject.PrizeDO;
import org.adam.lotterysystem.dao.dataobject.UserDO;
import org.adam.lotterysystem.dao.dataobject.WinningRecordDO;
import org.adam.lotterysystem.dao.mapper.ActivityMapper;
import org.adam.lotterysystem.dao.mapper.ActivityPrizeMapper;
import org.adam.lotterysystem.dao.mapper.ActivityUserMapper;
import org.adam.lotterysystem.dao.mapper.PrizeMapper;
import org.adam.lotterysystem.dao.mapper.UserMapper;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
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

    @Autowired
    private PrizeMapper prizeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WinningRecordMapper winningRecordMapper;

    @Test
    void drawPrizeTest() {
        DrawPrizeParam param = new DrawPrizeParam();
        param.setActivityId(1L);
        param.setPrizeId(1L);
        param.setPrizeTiers(ActivityPrizeTiersEnum.FIRST_PRIZE.name());
        param.setWinningTime(new Date());

        DrawPrizeParam.Winner winner = new DrawPrizeParam.Winner();
        winner.setUserId(1L);
        winner.setUserName("zhangsan");
        param.setWinnerList(List.of(winner));

        drawPrizeService.drawPrize(param);
    }

    @Test
    void testConvertActivityStatus() {
        Long prizeId = 18L;
        Long userId = 44L;

        CreateActivityParam createActivityParam = new CreateActivityParam();
        createActivityParam.setActivityName("status-test-" + System.currentTimeMillis());
        createActivityParam.setDescription("status test activity");

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
        convertActivityStatusDTO.setUserIds(List.of(userId));
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

    @Test
    void testSaveWinnerRecords() {
        DrawPrizeParam param = new DrawPrizeParam();
        param.setActivityId(24L);
        param.setPrizeId(19L);
        param.setPrizeTiers("FIRST_PRIZE");
        param.setWinningTime(new Date());
        List<DrawPrizeParam.Winner> winnerList = new ArrayList<>();
        DrawPrizeParam.Winner winner = new DrawPrizeParam.Winner();
        winner.setUserId(43L);
        winner.setUserName("lisi");
        winnerList.add(winner);
        param.setWinnerList(winnerList);
        drawPrizeService.saveWinnerRecords(param);
    }

}
