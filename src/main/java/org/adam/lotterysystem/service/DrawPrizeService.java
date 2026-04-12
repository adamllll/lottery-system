package org.adam.lotterysystem.service;

import org.adam.lotterysystem.controller.param.ExecuteDrawPrizeParam;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.controller.param.ShowWinningRecordsParam;
import org.adam.lotterysystem.dao.dataobject.WinningRecordDO;
import org.adam.lotterysystem.service.dto.DrawPrizeDTO;
import org.adam.lotterysystem.service.dto.WinningRecordDTO;

import java.util.List;

public interface DrawPrizeService {

    // 异步抽奖接口
    void drawPrize(DrawPrizeParam param);

    // 同步抽奖接口
    DrawPrizeDTO executeDraw(ExecuteDrawPrizeParam param);

    // 共享开奖执行内核
    List<WinningRecordDO> finalizeDraw(DrawPrizeParam param);

    // 校验抽奖请求是否合法
    Boolean checkDrawPrizeStatus(DrawPrizeParam param);

    // 保存中奖者名单
    List<WinningRecordDO> saveWinnerRecords(DrawPrizeParam param);

    // 删除 活动/奖品 中奖者名单
    void deleteWinnerRecords(Long activityId, Long prizeId);

    // 获取中奖记录
    List<WinningRecordDTO> getRecords(ShowWinningRecordsParam param);
}
