package org.adam.lotterysystem.controller;

import cn.hutool.core.collection.CollectionUtil;
import org.adam.lotterysystem.common.pojo.CommonResult;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.controller.param.ShowWinningRecordsParam;
import org.adam.lotterysystem.controller.result.WinningRecordsResult;
import org.adam.lotterysystem.service.DrawPrizeService;
import org.adam.lotterysystem.service.dto.WinningRecordDTO;
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
    public CommonResult<Boolean> drawPrize(
            @Validated @RequestBody DrawPrizeParam param) {
        logger.info("抽奖请求参数: {}", param);
        drawPrizeService.drawPrize(param);
        return CommonResult.success(true);
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
}
