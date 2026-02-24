package org.adam.lotterysystem.service.impl;

import org.adam.lotterysystem.controller.param.CreatePrizeParam;
import org.adam.lotterysystem.controller.param.PageParam;
import org.adam.lotterysystem.dao.dataobject.PrizeDO;
import org.adam.lotterysystem.dao.mapper.PrizeMapper;
import org.adam.lotterysystem.service.PictureService;
import org.adam.lotterysystem.service.PrizeService;
import org.adam.lotterysystem.service.dto.PageListDTO;
import org.adam.lotterysystem.service.dto.PrizeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public PageListDTO<PrizeDTO> findPrizeList(PageParam param) {
        // 查询总数
        int total = prizeMapper.countPrizes();
        // 查询当前页列表
        List<PrizeDTO> prizeDTOList = new ArrayList<>();
        List<PrizeDO> prizeDOList = prizeMapper.selectPrizeList(param.offset(), param.getPageSize());
        for (PrizeDO prizeDO : prizeDOList) {
            PrizeDTO prizeDTO = new PrizeDTO();
            prizeDTO.setId(prizeDO.getId());
            prizeDTO.setName(prizeDO.getName());
            prizeDTO.setDescription(prizeDO.getDescription());
            prizeDTO.setPrice(prizeDO.getPrice());
            prizeDTO.setImageUrl(prizeDO.getImageUrl());
            prizeDTOList.add(prizeDTO);
        }
        return new PageListDTO<>(total, prizeDTOList);
    }
}
