package com.hmall.service;

import com.hmall.domain.po.Address;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 收货地址服务接口
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
public interface IAddressService extends IService<Address> {

    /**
     * 根据用户ID查询地址列表
     * @param userId 用户ID
     * @return 地址列表
     */
    List<Address> queryByUserId(Long userId);

    /**
     * 保存地址
     * @param address 地址对象
     * @return 是否保存成功
     */
    boolean saveAddress(Address address);

    /**
     * 更新地址
     * @param address 地址对象
     * @return 是否更新成功
     */
    boolean updateAddress(Address address);

    /**
     * 删除地址
     * @param addressId 地址ID
     * @return 是否删除成功
     */
    boolean deleteAddress(Long addressId);
}
