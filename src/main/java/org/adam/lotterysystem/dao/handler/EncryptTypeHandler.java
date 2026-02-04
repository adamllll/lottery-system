package org.adam.lotterysystem.dao.handler;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import org.adam.lotterysystem.dao.dataobject.Encrypt;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.util.StringUtils;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(Encrypt.class) // 指定该 TypeHandler处理的Java类型 (被处理的类型)
@MappedJdbcTypes(JdbcType.VARCHAR) // 指定该 TypeHandler处理的JDBC类型 (转换后的JDBC类型)
public class EncryptTypeHandler extends BaseTypeHandler<Encrypt> {
    // 密钥
    private final byte[] key = "AdamIsAGoodMan12".getBytes();

    /**
     * 设置参数
     * @param ps SQL 预编译的对象
     * @param i 需要赋值的索引位置
     * @param parameter 原本位置 i需要赋的值
     * @param jdbcType JDBC类型
     * @throws SQLException
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Encrypt parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.getValue() == null) {
            ps.setString(i, null);
            return;
        }
        // 打印日志
        System.out.println("加密的内容: " + parameter.getValue());
        // 加密
        AES aes = SecureUtil.aes(key);
        String encryptedValue = aes.encryptHex(parameter.getValue());
        ps.setString(i, encryptedValue); // 设置加密后的值
    }

    /**
     * 获取结果
     * @param rs 结果集
     * @param columnName 索引名
     * @return
     * @throws SQLException
     */
    @Override
    public Encrypt getNullableResult(ResultSet rs, String columnName) throws SQLException {
        System.out.println("ResultSet 获取加密内容" + rs.getString(columnName));
        return decrypt(rs.getString(columnName));
    }

    /**
     * 获取结果
     * @param rs 结果集
     * @param columnIndex 索引位置
     * @return
     * @throws SQLException
     */
    @Override
    public Encrypt getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        System.out.println("ResultSet 获取加密内容" + rs.getString(columnIndex));
        return decrypt(rs.getString(columnIndex));
    }

    /**
     * 获取结果
     * @param cs 存储过程结果集
     * @param columnIndex 索引位置
     * @return
     * @throws SQLException
     */
    @Override
    public Encrypt getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        System.out.println("CallableStatement 获取加密内容" + cs.getString(columnIndex));
        return decrypt(cs.getString(columnIndex));
    }

    /**
     * 解密
     * @param encryptedValue
     * @return
     */
    private Encrypt decrypt(String encryptedValue) {
        if (!StringUtils.hasText(encryptedValue)) {
            return null;
        }
       return new Encrypt(SecureUtil.aes(key).decryptStr(encryptedValue));
    }
}
