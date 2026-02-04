package org.adam.lotterysystem.dao.mapper;

import jakarta.validation.constraints.NotBlank;
import org.adam.lotterysystem.dao.dataobject.Encrypt;
import org.adam.lotterysystem.dao.dataobject.UserDO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    /**
     * 查询邮箱绑定的人数
     * @param email
     * @return
     */
    @Select("select count(*) from user where email = #{email}")
    int countByMail(@Param("email") String email);

    /**
     * 查询手机号绑定的人数
     * @param phoneNumber
     * @return
     */
    @Select("select count(*) from user where phone_number = #{phoneNumber}")
    int countByPhoneNumber(@Param("phoneNumber") Encrypt phoneNumber);

    @Insert("insert into user(user_name, email, phone_number, password, identity) " +
            "values(#{userName}, #{email}, #{phoneNumber}, #{password}, #{identity})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(UserDO userDO);
}
