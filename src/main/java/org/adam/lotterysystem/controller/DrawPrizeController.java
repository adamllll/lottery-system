package org.adam.lotterysystem.controller;

import cn.hutool.core.collection.CollectionUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.adam.lotterysystem.common.errorcode.ControllerErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ControllerException;
import org.adam.lotterysystem.common.pojo.CommonResult;
import org.adam.lotterysystem.common.utils.JWTUtil;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.controller.param.ExecuteDrawPrizeParam;
import org.adam.lotterysystem.controller.param.ShowWinningRecordsParam;
import org.adam.lotterysystem.controller.result.DrawPrizeResult;
import org.adam.lotterysystem.controller.result.WinningRecordsResult;
import org.adam.lotterysystem.service.DrawPrizeService;
import org.adam.lotterysystem.service.dto.DrawPrizeDTO;
import org.adam.lotterysystem.service.dto.WinningRecordDTO;
import org.adam.lotterysystem.service.enums.UserIdentityEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@RestController
public class DrawPrizeController {

    private static final Logger logger = LoggerFactory.getLogger(DrawPrizeController.class);

    @Autowired
    private DrawPrizeService drawPrizeService;

    @RequestMapping("/draw-prize")
    public CommonResult<DrawPrizeResult> drawPrize(
            @Validated @RequestBody ExecuteDrawPrizeParam param,
            HttpServletRequest request) {
        logger.info("同步抽奖请求参数: {}", param);
        if (!isAdmin(request)) {
            throw new ControllerException(ControllerErrorCodeConstants.DRAW_PRIZE_FORBIDDEN);
        }
        return CommonResult.success(convertToDrawPrizeResult(drawPrizeService.executeDraw(param)));
    }

    @RequestMapping("/winning-records/show")
    public CommonResult<List<WinningRecordsResult>> showWinningRecords(
            @Validated @RequestBody ShowWinningRecordsParam param) {
        logger.info("查询中奖记录请求参数: {}", JacksonUtil.writeValueAsString(param));
        List<WinningRecordDTO> winningRecordDTOS = drawPrizeService.getRecords(param);
        return CommonResult.success(
                convertoWinningRecordsResultList(winningRecordDTOS));
    }

    // 将 WinningRecordDTO 列表转换为 WinningRecordsResult 列表
    private List<WinningRecordsResult> convertoWinningRecordsResultList(List<WinningRecordDTO> winningRecordDTOS) {
        if (CollectionUtil.isEmpty(winningRecordDTOS)) {
            return Arrays.asList();
        }
        return winningRecordDTOS.stream()
                .map(winningRecordDTO -> {
                    WinningRecordsResult result = new WinningRecordsResult();
                    result.setWinnerId(winningRecordDTO.getWinnerId());
                    result.setWinnerName(winningRecordDTO.getWinnerName());
                    result.setPrizeName(winningRecordDTO.getPrizeName());
                    result.setPrizeTier(winningRecordDTO.getPrizeTier().getMessage());
                    result.setWinningTime(winningRecordDTO.getWinningTime());
                    return result;
                }).collect(Collectors.toList());
    }

    private boolean isAdmin(HttpServletRequest request) {
        Claims claims = JWTUtil.parseJWT(request.getHeader("user_token"));
        if (claims == null) {
            return false;
        }
        Object identity = claims.get("identity");
        return identity != null && UserIdentityEnum.ADMIN.name().equalsIgnoreCase(identity.toString());
    }

    private DrawPrizeResult convertToDrawPrizeResult(DrawPrizeDTO dto) {
        DrawPrizeResult result = new DrawPrizeResult();
        result.setActivityId(dto.getActivityId());
        result.setPrizeId(dto.getPrizeId());
        result.setPrizeName(dto.getPrizeName());
        result.setPrizeTier(dto.getPrizeTier().getMessage());
        result.setWinningTime(dto.getWinningTime());
        result.setWinnerList(dto.getWinnerList().stream().map(winnerDTO -> {
            DrawPrizeResult.Winner winner = new DrawPrizeResult.Winner();
            winner.setUserId(winnerDTO.getUserId());
            winner.setUserName(winnerDTO.getUserName());
            return winner;
        }).collect(Collectors.toList()));
        return result;
    }
}
