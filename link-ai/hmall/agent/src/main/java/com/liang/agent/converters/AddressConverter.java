package com.liang.agent.converters;

import cn.hutool.core.bean.BeanUtil;
import com.hmall.common.domain.dto.AddressDTO;
import com.hmall.common.domain.vo.CartVO;
import com.liang.agent.result.AddressInfo;

public class AddressConverter {

    /**
     * 将AddressDTO转换为AddressInfo
     * **/
    public static AddressInfo toAddressInfo(AddressDTO dto) {
        if (dto == null) {
            return null;
        }
        AddressInfo info = BeanUtil.toBean(dto, AddressInfo.class);
        info.setFullAddress(dto.getProvince() + dto.getCity() + dto.getTown() + dto.getStreet());
        return info;
    }



}