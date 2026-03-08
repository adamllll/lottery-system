package org.adam.lotterysystem.service;

import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.dao.dataobject.WinningRecordDO;

import java.util.List;

public interface DrawPrizeService {

    // 异步抽奖接口
    void drawPrize(DrawPrizeParam param);

    // 校验抽奖请求是否合法
    void checkDrawPrizeStatus(DrawPrizeParam param);

    // 保存中奖者名单
    List<WinningRecordDO> saveWinnerRecords(DrawPrizeParam param);

    // 删除 活动/奖品 中奖者名单
    void deleteWinnerRecords(Long activityId, Long prizeId);
}
