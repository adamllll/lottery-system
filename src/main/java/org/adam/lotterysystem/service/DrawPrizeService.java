package org.adam.lotterysystem.service;

import org.adam.lotterysystem.controller.param.DrawPrizeParam;

public interface DrawPrizeService {

    // 异步抽奖接口
    void drawPrize(DrawPrizeParam param);

    // 校验抽奖请求是否合法
    void checkDrawPrizeStatus(DrawPrizeParam param);
}
