package org.adam.lotterysystem.controller;

import org.adam.lotterysystem.common.errorcode.ControllerErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ControllerException;
import org.adam.lotterysystem.common.pojo.CommonResult;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.controller.param.CreateActivityParam;
import org.adam.lotterysystem.controller.param.PageParam;
import org.adam.lotterysystem.controller.result.CreateActivityResult;
import org.adam.lotterysystem.controller.result.FindActivityListResult;
import org.adam.lotterysystem.controller.result.GetActivityDetailResult;
import org.adam.lotterysystem.service.ActivityService;
import org.adam.lotterysystem.service.dto.ActivityDTO;
import org.adam.lotterysystem.service.dto.ActivityDetailDTO;
import org.adam.lotterysystem.service.dto.CreateActivityDTO;
import org.adam.lotterysystem.service.dto.PageListDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.stream.Collectors;


@RestController
public class ActivityController {

    private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);

    @Autowired
    private ActivityService activityService;

    /**
     * 创建活动
     * @param param
     * @return
     */
    @RequestMapping("/activity/create")
    public CommonResult<CreateActivityResult> createActivity(@Validated @RequestBody CreateActivityParam param) {
        logger.info("创建活动, param: {}", JacksonUtil.writeValueAsString(param));
        return CommonResult.success(convertoCreateActivityResult (activityService.createActivity(param)));

    }

    @RequestMapping("/activity/find-list")
    public CommonResult<FindActivityListResult> findActivityList(PageParam param) {
        logger.info("查询活动列表, param: {}", JacksonUtil.writeValueAsString(param));
        return CommonResult.success(converToFindActivityListResult (activityService.findActivityList(param)));
    }

    @RequestMapping("/activity-detail/find")
    public CommonResult<GetActivityDetailResult> getActivityDetail(Long activityId) {
        logger.info("查询活动详情, activityId: {}", activityId);
        ActivityDetailDTO detailDTO = activityService.getActivityDetail(activityId);
        return CommonResult.success(convertoGetActivityDetailRestult(detailDTO));
    }

    private GetActivityDetailResult convertoGetActivityDetailRestult(ActivityDetailDTO detailDTO) {
        if (null == detailDTO) {
            throw new ControllerException(ControllerErrorCodeConstants.GET_ACTIVITY_DETAIL_ERROR);
        }

        GetActivityDetailResult result = new GetActivityDetailResult();
        result.setActivityId(detailDTO.getActivityId());
        result.setActivityName(detailDTO.getActivityName());
        result.setDescription(detailDTO.getDescription());
        result.setValid(detailDTO.valid());
        // 抽奖顺序：1 2 3 等奖
        result.setPrizeDTOList(
                detailDTO.getPrizeDTOList()
                        .stream()
                        .sorted(Comparator.comparingInt(prizeDTO -> prizeDTO.getTiers().getCode()))
                        .map(PrizeDTO -> {
                            GetActivityDetailResult.Prize prize = new GetActivityDetailResult.Prize();
                            prize.setId(PrizeDTO.getId());
                            prize.setName(PrizeDTO.getName());
                            prize.setImageUrl(PrizeDTO.getImageUrl());
                            prize.setPrice(PrizeDTO.getPrice());
                            prize.setDescription(PrizeDTO.getDescription());
                            prize.setTiers(PrizeDTO.getTiers().getMessage());
                            prize.setPrizeAmount(PrizeDTO.getPrizeAmount());
                            prize.setValid(PrizeDTO.valid());
                            return prize;
                        }).collect(Collectors.toList())
        );
        result.setUserDTOList(
                detailDTO.getUserDTOList()
                        .stream()
                        .map(userDTO -> {
                            GetActivityDetailResult.User user = new GetActivityDetailResult.User();
                            user.setUserId(userDTO.getUserId());
                            user.setUserName(userDTO.getUserName());
                            user.setValid(userDTO.valid());
                            return user;
                        }).collect(Collectors.toList())
        );
        return result;
    }

    private FindActivityListResult converToFindActivityListResult(PageListDTO<ActivityDTO> activityList) {
        if (null == activityList) {
            throw new ControllerException(ControllerErrorCodeConstants.FIND_ACTIVITY_LIST_ERROR);
        }
        FindActivityListResult result = new FindActivityListResult();
        result.setTotal(activityList.getTotal());
        result.setRecords(activityList.getRecords().stream().map(activityDTO -> {
            FindActivityListResult.ActivityInfo activityInfo = new FindActivityListResult.ActivityInfo();
            activityInfo.setActivityId(activityDTO.getActivityId());
            activityInfo.setActivityName(activityDTO.getActivityName());
            activityInfo.setDescription(activityDTO.getDescription());
            activityInfo.setValid(activityDTO.Valid());
            return activityInfo;
        }).collect(Collectors.toList()));
        return result;
    }

    private CreateActivityResult convertoCreateActivityResult(CreateActivityDTO createActivityDTO) {
        if (null== createActivityDTO) {
            throw new ControllerException(ControllerErrorCodeConstants.CREATE_ACTIVITY_ERROR);
        }
        CreateActivityResult result = new CreateActivityResult();
        result.setActivityId(createActivityDTO.getActivityId());
        return result;
    }

}
