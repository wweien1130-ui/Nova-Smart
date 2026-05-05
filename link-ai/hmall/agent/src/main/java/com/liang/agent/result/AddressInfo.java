package com.liang.agent.result;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressInfo {
    @JsonPropertyDescription("地址ID")
    private Long id;

    @JsonPropertyDescription("联系人")
    private String contact;

    @JsonPropertyDescription("手机号")
    private String mobile;

    @JsonPropertyDescription("省份")
    private String province;

    @JsonPropertyDescription("城市")
    private String city;

    @JsonPropertyDescription("区县")
    private String town;

    @JsonPropertyDescription("详细地址")
    private String street;

    @JsonPropertyDescription("是否为默认地址")
    private Integer isDefault;

    @JsonPropertyDescription("完整地址")
    private String fullAddress;
}