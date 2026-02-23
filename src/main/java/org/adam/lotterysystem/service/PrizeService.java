package org.adam.lotterysystem.service;

import org.adam.lotterysystem.controller.param.CreatePrizeParam;
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
}
