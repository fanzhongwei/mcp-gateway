package com.mmyf.commons.service.right;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mmyf.commons.constant.SystemCodeEnum;
import com.mmyf.commons.mapper.organ.RightMapper;
import com.mmyf.commons.mapper.organ.UserRightMapper;
import com.mmyf.commons.model.entity.organ.Right;
import com.mmyf.commons.model.entity.organ.UserRight;
import com.mmyf.commons.model.vo.organ.RightVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mmyf.commons.service.DbOperateService;
import com.mmyf.commons.util.uuid.UUIDHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * <p>
 * 权限定义 服务实现类
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@Service
public class RightService extends ServiceImpl<RightMapper, Right> implements IService<Right> {

    @Autowired
    private DbOperateService dbOperateService;

    @Resource
    private UserRightMapper userRightMapper;

    /**
     * 添加权限点
     *
     * @param rightVo 权限信息
     */
    public void addRight(RightVo rightVo) {
        List<UserRight> userRights = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(rightVo.getUserIds())) {
            rightVo.getUserIds().forEach(userId -> {
                UserRight userRight = new UserRight();
                userRight.setId(UUIDHelper.getUuid());
                userRight.setUserid(userId);
                userRight.setRightkey(rightVo.getRightkey());
                userRight.setType(SystemCodeEnum.UserRigtType.RIGHT.getCode());
                userRights.add(userRight);
            });
        }
        dbOperateService.executeInTransaction(() -> {
            this.save(rightVo);
            dbOperateService.batchInsert(userRights);
        });
    }

    /**
     * 更新权限
     *
     * @param rightVo 权限信息
     */
    public void updateRight(RightVo rightVo) {
        List<UserRight> userRights = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(rightVo.getUserIds())) {
            rightVo.getUserIds().forEach(userId -> {
                UserRight userRight = new UserRight();
                userRight.setId(UUIDHelper.getUuid());
                userRight.setUserid(userId);
                userRight.setRightkey(rightVo.getRightkey());
                userRight.setType(SystemCodeEnum.UserRigtType.RIGHT.getCode());
                userRights.add(userRight);
            });
        }
        dbOperateService.executeInTransaction(() -> {
            if (CollectionUtils.isNotEmpty(rightVo.getUserIds())) {
                userRightMapper.delete(Wrappers.<UserRight>lambdaQuery()
                                               .eq(UserRight::getRightkey, rightVo.getRightkey()));
            }
            this.updateById(rightVo);
            dbOperateService.batchInsert(userRights);
        });
    }

    /**
     * 删除权限
     *
     * @param rightKey 权限key
     */
    public void deleteRight(String rightKey) {
        dbOperateService.executeInTransaction(() -> {
            userRightMapper.delete(Wrappers.<UserRight>lambdaQuery().eq(UserRight::getRightkey, rightKey));
            this.removeById(rightKey);
        });
    }

    /**
     * 获取权限信息
     *
     * @param key 权限字
     * @return RightVo
     */
    public RightVo getRightInfo(String key) {
        Right right = this.getById(key);
        RightVo rightVo = new RightVo(right);
        rightVo.setUserIds(userRightMapper.getHasRightUser(key));
        return rightVo;
    }

    /**
     * 获取用户所拥有的权限
     *
     * @param id user id
     * @return List<String>
     */
    public Set<String> getRightsByUserId(String id) {
        return userRightMapper.getUserHasRight(id);
    }

    /**
     * 获取所有权限点
     *
     * @return allRightKeys
     */
    public List<Right> allRight() {
        return this.list(Wrappers.emptyWrapper());
    }

    /**
     * 判断当前用户是否拥有权限
     * @param userId userId
     * @param rightKey rightKey
     * @return
     */
    public boolean hasRight(String userId, String rightKey) {
        Set<String> rightKeySet = userRightMapper.getUserHasRight(userId);
        return rightKeySet.contains(rightKey);
    }

    public void deleteUserRightAndRole(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        userRightMapper.delete(Wrappers.<UserRight>lambdaQuery().eq(UserRight::getUserid, userId));
    }
}
