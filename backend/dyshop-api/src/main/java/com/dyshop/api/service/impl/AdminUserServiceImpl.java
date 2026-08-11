package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.mapper.UserMapper;
import com.dyshop.api.vo.AdminUserVO;
import com.dyshop.common.entity.User;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 后台用户管理实现。
 */
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl {

    private final UserMapper userMapper;

    public IPage<AdminUserVO> list(String keyword, long page, long size) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .and(hasKeyword, w -> w.like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword)
                        .or().like(User::getPhone, keyword))
                .orderByDesc(User::getId);
        IPage<User> p = userMapper.selectPage(new Page<>(page, size), qw);
        if (p.getRecords().isEmpty()) {
            return new Page<>(page, size);
        }
        List<AdminUserVO> vos = p.getRecords().stream().map(this::toVo).toList();
        Page<AdminUserVO> result = new Page<>(page, size, p.getTotal());
        result.setRecords(vos);
        return result;
    }

    public void changeStatus(Long id, Integer status, Long currentUserId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态参数错误");
        }
        User user = requireUser(id);
        if (Objects.equals(id, currentUserId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "不能操作当前登录账号");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public void changeRole(Long id, Integer role, Long currentUserId) {
        if (role == null || (role != 0 && role != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "角色参数错误");
        }
        User user = requireUser(id);
        if (Objects.equals(id, currentUserId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "不能操作当前登录账号");
        }
        user.setRole(role);
        userMapper.updateById(user);
    }

    public AdminUserVO me(Long currentUserId) {
        User user = requireUser(currentUserId);
        return toVo(user);
    }

    // ---------- 私有 ----------

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private AdminUserVO toVo(User u) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setPhone(u.getPhone());
        vo.setEmail(u.getEmail());
        vo.setRole(u.getRole());
        vo.setStatus(u.getStatus());
        vo.setCreateTime(u.getCreateTime());
        return vo;
    }
}