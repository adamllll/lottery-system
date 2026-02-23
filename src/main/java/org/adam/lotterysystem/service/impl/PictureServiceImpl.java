package org.adam.lotterysystem.service.impl;

import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.service.PictureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class PictureServiceImpl implements PictureService {
    private static final Logger logger = LoggerFactory.getLogger(PictureServiceImpl.class);

    @Value("${pic.local-path}")
    private String localPath;

    @Override
    public String savePicture(MultipartFile multipartFile) {
        // 统一使用绝对路径，避免相对路径随启动目录变化
        File dir = new File(localPath).getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            logger.error("创建图片目录失败，dir={}", dir.getAbsolutePath());
            throw new ServiceException(ServiceErrorCodeConstants.PICTURE_SAVE_ERROR);
        }

        // 生成唯一文件名，保留原后缀
        String filename = multipartFile.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            logger.error("原始文件名非法，filename={}", filename);
            throw new ServiceException(ServiceErrorCodeConstants.PICTURE_SAVE_ERROR);
        }
        String suffix = filename.substring(filename.lastIndexOf('.'));
        String savedFilename = UUID.randomUUID() + suffix;

        // 使用 NIO 流复制，规避 transferTo 在部分容器场景下的兼容问题
        Path target = dir.toPath().resolve(savedFilename);
        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("保存图片失败，target={}", target, e);
            throw new ServiceException(ServiceErrorCodeConstants.PICTURE_SAVE_ERROR);
        }

        return savedFilename;
    }
}
