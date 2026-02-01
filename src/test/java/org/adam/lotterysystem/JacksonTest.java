package org.adam.lotterysystem;

import org.adam.lotterysystem.common.pojo.CommonResult;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class JacksonTest {

    @Test
    void jacksonTest() {
        ObjectMapper objectMapper = new ObjectMapper();
        CommonResult<String> result = CommonResult.error(500,"hello jackson");
        String str;

        // 序列化
        str = objectMapper.writeValueAsString(result);
        System.out.println("序列化后的字符串：" + str);

        // 反序列化
        CommonResult<String> readResult = objectMapper.readValue(str, CommonResult.class);
        System.out.println(readResult.getCode() + ","+ readResult.getMsg());

        // List 序列化
        List<CommonResult<String>> commonResults = Arrays.asList(
                CommonResult.success("success1"),
                CommonResult.success("success2")
        );
        str = objectMapper.writeValueAsString(commonResults);
        System.out.println("List 序列化后的字符串：" + str);

        // List 反序列化
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(List.class, CommonResult.class);
        List<CommonResult<String>> readList = objectMapper.readValue(str, javaType);
        for (CommonResult<String> commonResult : commonResults) {
            System.out.println(commonResult.getCode() + ","+ commonResult.getData());
        }
    }

    @Test
    void JacksonUtilTest() {
        CommonResult<String> result = CommonResult.success("success");
        String str;

        str = JacksonUtil.writeValueAsString(result);
        System.out.println("JacksonUtil 序列化后的字符串：" + str);

        result = JacksonUtil.readValue(str, CommonResult.class);
        System.out.println("JacksonUtil 反序列化后的字符串" + result.getData());

        List<CommonResult<String>> commonResults = Arrays.asList(
                CommonResult.success("success1"),
                CommonResult.success("success2")
        );
        str = JacksonUtil.writeValueAsString(commonResults);
        System.out.println("List 序列化：" + str);

        JacksonUtil.readListValue(str, CommonResult.class);
        for (CommonResult<String> commonResult : commonResults) {
            System.out.println("List 反序列化" + commonResult.getCode() + ","+ commonResult.getData());
        }
    }



}
