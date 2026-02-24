package org.adam.lotterysystem.common.converter;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 处理 multipart 请求中 JSON 部分的反序列化。
 * 仅支持读取（反序列化），禁止写入（序列化），避免干扰正常的 JSON 响应输出。
 */
@Component
public class MultipartJackson2HttpMessageConverter extends AbstractJacksonHttpMessageConverter<JsonMapper> {

    protected MultipartJackson2HttpMessageConverter(JsonMapper jsonMapper) {
        // MediaType.APPLICATION_OCTET_STREAM 表示这个转换器用于处理二进制流数据，通常用于文件上传。
        super(jsonMapper, MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public boolean canWrite(ResolvableType type, Class<?> clazz, MediaType mediaType) {
        return false;
    }
}