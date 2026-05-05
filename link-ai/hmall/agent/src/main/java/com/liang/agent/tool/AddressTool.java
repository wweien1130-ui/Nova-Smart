package com.liang.agent.tool;

import com.hmall.api.client.AddressFeignClient;
import com.hmall.common.domain.dto.AddressDTO;
import com.liang.agent.converters.AddressConverter;
import com.liang.agent.result.AddressInfo;
import com.liang.agent.util.ToolContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.liang.agent.constants.Constant.Tools.*;
import static com.liang.agent.constants.Constant.ToolParamns.*;

@Component
@RequiredArgsConstructor
public class AddressTool {

    private final AddressFeignClient addressFeignClient;

    @Tool(name = LIST_ADDRESSES, description = "查询当前用户的所有收货地址列表")
    public List<AddressInfo> listAddresses(ToolContext toolContext) {
        System.out.println("========== 调用了 listAddresses ==========");

        ToolContextUtil.setUserContext(toolContext);

        List<AddressDTO> addresses = addressFeignClient.findMyAddresses();
        return addresses.stream()
                .map(AddressConverter::toAddressInfo)
                .toList();
    }

    @Tool(name = ADD_ADDRESS, description = "添加新的收货地址")
    public AddressInfo addAddress(
            @ToolParam(description = CONTACT) String contact,
            @ToolParam(description = MOBILE) String mobile,
            @ToolParam(description = PROVINCE) String province,
            @ToolParam(description = CITY) String city,
            @ToolParam(description = TOWN) String town,
            @ToolParam(description = STREET) String street,
            @ToolParam(description = IS_DEFAULT) Integer isDefault,
            ToolContext toolContext) {

        System.out.println("========== 调用了 addAddress ==========");

        ToolContextUtil.setUserContext(toolContext);

        AddressDTO dto = new AddressDTO();
        dto.setContact(contact);
        dto.setMobile(mobile);
        dto.setProvince(province);
        dto.setCity(city);
        dto.setTown(town);
        dto.setStreet(street);
        dto.setIsDefault(isDefault != null ? isDefault : 0);

        Long id = addressFeignClient.addAddress(dto).getId();
        dto.setId(id);

        return AddressConverter.toAddressInfo(dto);
    }

    @Tool(name = UPDATE_ADDRESS, description = "更新收货地址")
    public void updateAddress(
            @ToolParam(description = ADDRESS_ID) Long addressId,
            @ToolParam(description = CONTACT) String contact,
            @ToolParam(description = MOBILE) String mobile,
            @ToolParam(description = PROVINCE) String province,
            @ToolParam(description = CITY) String city,
            @ToolParam(description = TOWN) String town,
            @ToolParam(description = STREET) String street,
            @ToolParam(description = IS_DEFAULT) Integer isDefault,
            ToolContext toolContext) {

        System.out.println("========== 调用了 updateAddress，地址ID: " + addressId + " ==========");

        ToolContextUtil.setUserContext(toolContext);

        AddressDTO dto = addressFeignClient.findAddressById(addressId);
        if (dto != null) {
            if (contact != null) dto.setContact(contact);
            if (mobile != null) dto.setMobile(mobile);
            if (province != null) dto.setProvince(province);
            if (city != null) dto.setCity(city);
            if (town != null) dto.setTown(town);
            if (street != null) dto.setStreet(street);
            if (isDefault != null) dto.setIsDefault(isDefault);
            addressFeignClient.updateAddress(dto);
        }
    }

    @Tool(name = DELETE_ADDRESS, description = "删除收货地址")
    public void deleteAddress(
            @ToolParam(description = ADDRESS_ID) Long addressId,
            ToolContext toolContext) {
        System.out.println("========== 调用了 deleteAddress，地址ID: " + addressId + " ==========");

        ToolContextUtil.setUserContext(toolContext);

        addressFeignClient.deleteAddress(addressId);
    }
}