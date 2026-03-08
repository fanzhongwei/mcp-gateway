package com.mmyf.commons.mapper.organ;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmyf.commons.model.entity.organ.UserRight;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 用户权限 Mapper 接口
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Mapper
public interface UserRightMapper extends BaseMapper<UserRight> {

    /**
     * 获取拥有权限的人员
     *
     * @param key 权限字
     * @return java.util.List<java.lang.String> 人员
     * @author fanzhongwei
     * @date 2024/3/1 下午2:46
     **/
    @Select("select c_userid FROM system_base.t_user_right where c_rightkey = #{key} and c_type = '1' \n" +
            "union \n" +
            "select ur.c_userid from system_base.t_user_right ur left join system_base.t_role_right rr on ur.c_roleid = rr.c_role_id where rr.c_rightkey = #{key} and ur.c_type = '2' ")
    Set<String> getHasRightUser(@Param("key") String key);


    /**
     * 获取人员拥有的权限
     *
     * @param userId 人员ID
     * @return java.util.List<java.lang.String>
     * @author fanzhongwei
     * @date 2024/3/1 下午2:49
     **/
    @Select("select c_rightkey FROM system_base.t_user_right where c_userid = #{userId} and c_type = '1'\n" +
            "union\n" +
            "select rr.c_rightkey from system_base.t_user_right ur left join system_base.t_role_right rr on ur.c_roleid = rr.c_role_id where ur.c_userid = #{userId} and ur.c_type = '2' ")
    Set<String> getUserHasRight(@Param("userId") String userId);
}
