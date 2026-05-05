package com.hmall.service.impl;

import com.hmall.domain.po.Address;
import com.hmall.mapper.AddressMapper;
import com.hmall.service.IAddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 收货地址服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements IAddressService {

    @Override
    public List<Address> queryByUserId(Long userId) {
        return lambdaQuery()
                .eq(Address::getUserId, userId)
                .list();
    }

    @Override
    public boolean saveAddress(Address address) {
        return save(address);
    }

    @Override
    public boolean updateAddress(Address address) {
        return updateById(address);
    }

    @Override
    public boolean deleteAddress(Long addressId) {
        return removeById(addressId);
    }
}
