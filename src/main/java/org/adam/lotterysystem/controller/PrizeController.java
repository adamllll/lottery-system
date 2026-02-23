package org.adam.lotterysystem.controller;

import jakarta.validation.Valid;
import org.adam.lotterysystem.common.pojo.CommonResult;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.controller.param.CreatePrizeParam;
import org.adam.lotterysystem.service.PictureService;
import org.adam.lotterysystem.service.PrizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
public class PrizeController {

    private static final Logger logger = LoggerFactory.getLogger(PrizeController.class);

    @Autowired
    private PrizeService prizeService;

    @Autowired
    private PictureService pictureService;

    @PostMapping("/pics/upload")
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
}
