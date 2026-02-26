package org.adam.lotterysystem.controller;

import jakarta.validation.Valid;
import org.adam.lotterysystem.common.errorcode.ControllerErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ControllerException;
import org.adam.lotterysystem.common.pojo.CommonResult;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.controller.param.CreatePrizeParam;
import org.adam.lotterysystem.controller.param.PageParam;
import org.adam.lotterysystem.controller.result.FindPrizeListResult;
import org.adam.lotterysystem.service.PictureService;
import org.adam.lotterysystem.service.PrizeService;
import org.adam.lotterysystem.service.dto.PageListDTO;
import org.adam.lotterysystem.service.dto.PrizeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;


@RestController
public class PrizeController {

    private static final Logger logger = LoggerFactory.getLogger(PrizeController.class);

    @Autowired
    private PrizeService prizeService;

    @Autowired
    private PictureService pictureService;

    @PostMapping("/pic/upload")
    public String uploadPicture(@RequestParam("file") MultipartFile file) {
        return pictureService.savePicture(file);
    }

    /**
     * 创建奖品
     *  RequestPart 用于接收 multipart/form-data 请求中的不同部分，适合同时上传文件和普通数据
     * @param param
     * @param picFile
     * @return
     */
    @RequestMapping("/prize/create")
    public CommonResult<Long> createPrize(@Valid @RequestPart("param") CreatePrizeParam param,
                                          @RequestPart("prizePic") MultipartFile picFile) {
        logger.info("创建奖品，param={}", JacksonUtil.writeValueAsString(param));
        return CommonResult.success(prizeService.createPrize(param, picFile));
    }

    @RequestMapping("/prize/find-list")
    public CommonResult<FindPrizeListResult> findPrizeListResult(PageParam param) {
        logger.info("查询奖品列表，param={}", JacksonUtil.writeValueAsString(param));
        PageListDTO<PrizeDTO> pageListDTO = prizeService.findPrizeList(param);
        return CommonResult.success(convertToFindPrizeListResult(pageListDTO));
    }

    private FindPrizeListResult convertToFindPrizeListResult(PageListDTO<PrizeDTO> pageListDTO) {
        if (null == pageListDTO) {
            throw new ControllerException(ControllerErrorCodeConstants.PRIZE_NOT_FOUND);
        }

        FindPrizeListResult result = new FindPrizeListResult();
        result.setTotal(pageListDTO.getTotal());
        result.setRecords(pageListDTO.getRecords().stream().map(prizeDTO -> {
            FindPrizeListResult.PrizeInfo prizeInfo = new FindPrizeListResult.PrizeInfo();
            prizeInfo.setPrizeId(String.valueOf(prizeDTO.getId()));
            prizeInfo.setPrizeName(prizeDTO.getName());
            prizeInfo.setDescription(prizeDTO.getDescription());
            prizeInfo.setPrice(prizeDTO.getPrice());
            prizeInfo.setImageUrl(prizeDTO.getImageUrl());
            return prizeInfo;
        }).collect(Collectors.toList()));
        return result;
    }
}
