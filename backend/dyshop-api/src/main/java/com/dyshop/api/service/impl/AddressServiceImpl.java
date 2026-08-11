package com.dyshop.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dyshop.api.dto.AddressDTO;
import com.dyshop.api.mapper.AddressMapper;
import com.dyshop.api.vo.AddressVO;
import com.dyshop.common.entity.Address;
import com.dyshop.common.exception.BizException;
import com.dyshop.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl {

    /** 每用户最大地址数 */
    private static final int MAX_ADDRESS_COUNT = 20;

    private final AddressMapper addressMapper;

    public List<AddressVO> list(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        // 默认地址置顶，其余按创建时间倒序
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getCreateTime))
                .stream().map(this::toVo).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, AddressDTO dto) {
        Long count = addressMapper.selectCount(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId));
        if (count != null && count >= MAX_ADDRESS_COUNT) {
            throw new BizException(ResultCode.PARAM_ERROR, "地址数量已达上限（" + MAX_ADDRESS_COUNT + " 条）");
        }

        boolean isEmpty = count == null || count == 0;
        Address address = new Address();
        address.setUserId(userId);
        address.setReceiverName(dto.getReceiverName().trim());
        address.setReceiverPhone(dto.getReceiverPhone().trim());
        address.setProvince(dto.getProvince().trim());
        address.setCity(dto.getCity().trim());
        address.setDistrict(trimToNull(dto.getDistrict()));
        address.setDetail(dto.getDetail().trim());

        // 首条地址自动设为默认，保证恒有一个默认地址
        int isDefault = Objects.equals(dto.getIsDefault(), 1) || isEmpty ? 1 : 0;
        address.setIsDefault(isDefault);
        if (isDefault == 1) {
            clearDefault(userId, null);
        }
        addressMapper.insert(address);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, AddressDTO dto) {
        Address address = requireOwned(userId, id);

        // 默认地址不可直接取消：已是默认且改为非默认 -> 拒绝
        if (Objects.equals(address.getIsDefault(), 1) && !Objects.equals(dto.getIsDefault(), 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "默认地址不可取消，请先设置其他地址为默认");
        }

        address.setReceiverName(dto.getReceiverName().trim());
        address.setReceiverPhone(dto.getReceiverPhone().trim());
        address.setProvince(dto.getProvince().trim());
        address.setCity(dto.getCity().trim());
        address.setDistrict(trimToNull(dto.getDistrict()));
        address.setDetail(dto.getDetail().trim());
        if (Objects.equals(dto.getIsDefault(), 1) && !Objects.equals(address.getIsDefault(), 1)) {
            clearDefault(userId, null);
            address.setIsDefault(1);
        }
        addressMapper.updateById(address);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        Address address = requireOwned(userId, id);
        addressMapper.deleteById(id);

        // 删除的是默认地址：剩余地址中最近创建的一条自动设为默认
        if (Objects.equals(address.getIsDefault(), 1)) {
            List<Address> rest = addressMapper.selectList(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .orderByDesc(Address::getCreateTime)
                    .last("LIMIT 1"));
            if (!rest.isEmpty()) {
                Address next = rest.get(0);
                next.setIsDefault(1);
                addressMapper.updateById(next);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long id) {
        requireOwned(userId, id);
        clearDefault(userId, id);
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getId, id)
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 1));
    }

    private Address requireOwned(Long userId, Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null || !Objects.equals(address.getUserId(), userId)) {
            throw new BizException(ResultCode.NOT_FOUND, "地址不存在");
        }
        return address;
    }

    /** 清空用户全部默认地址；excludeId 指定时保留该地址的默认状态不变 */
    private void clearDefault(Long userId, Long excludeId) {
        addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .ne(excludeId != null, Address::getId, excludeId)
                .set(Address::getIsDefault, 0));
    }

    private AddressVO toVo(Address address) {
        AddressVO vo = new AddressVO();
        vo.setId(address.getId());
        vo.setReceiverName(address.getReceiverName());
        vo.setReceiverPhone(address.getReceiverPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetail(address.getDetail());
        vo.setIsDefault(address.getIsDefault());
        StringBuilder sb = new StringBuilder(address.getProvince())
                .append(address.getCity());
        if (address.getDistrict() != null) {
            sb.append(address.getDistrict());
        }
        sb.append(address.getDetail());
        vo.setFullAddress(sb.toString());
        return vo;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}