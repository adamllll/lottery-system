package org.adam.lotterysystem.service;

import org.adam.lotterysystem.controller.param.CreatePrizeParam;
import org.adam.lotterysystem.controller.param.PageParam;
import org.adam.lotterysystem.controller.result.FindPrizeListResult;
import org.adam.lotterysystem.dao.dataobject.PrizeDO;
import org.adam.lotterysystem.service.dto.PageListDTO;
import org.adam.lotterysystem.service.dto.PrizeDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

public interface PrizeService {

    /**
     * 创建单个奖品
     * @param param 奖品属性
     * @param picFile 奖品图片
     * @return 奖品 id
     */
    Long createPrize(CreatePrizeParam param, MultipartFile picFile);

    /**
     * 翻页查询奖品列表
     * @param param
     * @return
     */
    PageListDTO<PrizeDTO> findPrizeList(PageParam param);
}
