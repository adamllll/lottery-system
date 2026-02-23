package org.adam.lotterysystem.service.impl;

import org.adam.lotterysystem.controller.param.CreatePrizeParam;
import org.adam.lotterysystem.dao.dataobject.PrizeDO;
import org.adam.lotterysystem.dao.mapper.PrizeMapper;
import org.adam.lotterysystem.service.PictureService;
import org.adam.lotterysystem.service.PrizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PrizeServiceImpl implements PrizeService {

    @Autowired
    private PictureService pictureService;

    @Autowired
    private PrizeMapper prizeMapper;

    @Override
    public Long createPrize(CreatePrizeParam param, MultipartFile picFile) {
        // 上传图片
        String fileName = pictureService.savePicture(picFile);
        // 存库
        PrizeDO prizeDO = new PrizeDO();
        prizeDO.setName(param.getPrizeName());
        prizeDO.setDescription(param.getPrizeDescription());
        prizeDO.setImageUrl(fileName);
        prizeDO.setPrice(param.getPrizePrice());
        prizeMapper.insert(prizeDO);
        return prizeDO.getId();
    }
}
