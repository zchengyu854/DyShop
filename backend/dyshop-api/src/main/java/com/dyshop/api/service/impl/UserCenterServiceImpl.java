package com.dyshop.api.service.impl;

import com.dyshop.api.dto.UpdatePasswordDTO;
import com.dyshop.api.dto.UpdateProfileDTO;
import com.dyshop.api.mapper.UserMapper;
import com.dyshop.api.vo.UserVO;
import com.dyshop.common.entity.User;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserCenterServiceImpl {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserVO getProfile(Long userId) {
        return toUserVO(requireUser(userId));
    }

    public UserVO updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = requireUser(userId);
        if (StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname().trim());
        }
        user.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone() : null);
        user.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail() : null);
        userMapper.updateById(user);
        return toUserVO(user);
    }

    public void updatePassword(Long userId, UpdatePasswordDTO dto) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        return vo;
    }
}