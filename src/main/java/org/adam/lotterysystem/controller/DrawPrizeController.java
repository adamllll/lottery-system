package org.adam.lotterysystem.controller;

import org.adam.lotterysystem.common.pojo.CommonResult;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.service.DrawPrizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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
}
