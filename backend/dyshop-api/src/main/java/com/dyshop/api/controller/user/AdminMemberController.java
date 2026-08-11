package com.dyshop.api.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dyshop.api.mapper.UserMapper;
import com.dyshop.api.service.impl.MemberLevelServiceImpl;
import com.dyshop.api.vo.AdminMemberUserVO;
import com.dyshop.api.vo.MemberLevelVO;
import com.dyshop.common.entity.User;
import com.dyshop.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 后台会员管理接口（需 ROLE_ADMIN，ch09）。
 */
@RestController
@RequestMapping("/api/admin/member")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberLevelServiceImpl memberLevelService;
    private final UserMapper userMapper;

    @GetMapping("/levels")
    public Result<List<MemberLevelVO>> levels() {
        return Result.success(memberLevelService.listLevels());
    }

    @PutMapping("/levels/{id}")
    public Result<Void> updateLevel(@PathVariable Long id, @RequestBody UpdateMemberLevelDTO dto) {
        memberLevelService.updateLevel(id, dto.threshold(), dto.discountRate(), dto.pointRate());
        return Result.success();
    }

    @GetMapping("/users")
    public Result<IPage<AdminMemberUserVO>> users(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            qw.and(w -> w.like(User::getUsername, kw)
                    .or().like(User::getNickname, kw)
                    .or().like(User::getPhone, kw));
        }
        qw.orderByDesc(User::getId);
        IPage<User> p = userMapper.selectPage(new Page<>(page, size), qw);
        List<AdminMemberUserVO> vos = p.getRecords().stream().map(u -> {
            var overview = memberLevelService.overview(u.getId());
            AdminMemberUserVO vo = new AdminMemberUserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setPhone(u.getPhone());
            vo.setRole(u.getRole());
            vo.setStatus(u.getStatus());
            vo.setLevel(overview.getLevel());
            vo.setAnnualConsumption(overview.getAnnualConsumption());
            vo.setTotalConsumption(overview.getTotalConsumption());
            vo.setPoints(overview.getPoints());
            vo.setCreateTime(u.getCreateTime());
            return vo;
        }).toList();
        Page<AdminMemberUserVO> result = new Page<>(page, size, p.getTotal());
        result.setRecords(vos);
        return Result.success(result);
    }

    public record UpdateMemberLevelDTO(BigDecimal threshold, BigDecimal discountRate, BigDecimal pointRate) {
    }
}