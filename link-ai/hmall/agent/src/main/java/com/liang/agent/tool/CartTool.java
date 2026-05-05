package com.liang.agent.tool;

import com.hmall.api.client.CartFeignClient;
import com.hmall.common.domain.dto.CartFormDTO;
import com.liang.agent.config.ToolResultHolder;
import com.liang.agent.constants.Constant;
import com.liang.agent.converters.CartConverter;
import com.liang.agent.result.CartInfo;
import com.liang.agent.util.ToolContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.liang.agent.constants.Constant.ToolParamns.*;

@Component
@RequiredArgsConstructor
public class CartTool {

    private final CartFeignClient cartFeignClient;

    @Tool(name = Constant.Tools.ADD_TO_CART, description = "将商品添加到购物车")
    public CartInfo addToCart(
            @ToolParam(description = ITEM_ID) Long itemId,
            @ToolParam(description = ITEM_NAME) String name,
            @ToolParam(description = ITEM_PRICE) Integer price,
            @ToolParam(description = ITEM_IMAGE) String image,
            @ToolParam(description = ITEM_SPEC) String spec,
            @ToolParam(description = ITEM_NUM) Integer num,
            ToolContext toolContext) {

        System.out.println("========== 调用了 addToCart，商品ID: " + itemId + " ==========");

        ToolContextUtil.setUserContext(toolContext);

        CartFormDTO cartFormDTO = new CartFormDTO();
        cartFormDTO.setItemId(itemId);
        cartFormDTO.setName(name);
        cartFormDTO.setPrice(price);
        cartFormDTO.setImage(image);
        cartFormDTO.setSpec(spec);
        cartFormDTO.setNum(num);

        cartFeignClient.addItem2Cart(cartFormDTO);

        CartInfo cartInfo = CartInfo.builder()
                .itemId(itemId)
                .name(name)
                .num(num)
                .price(price / 100.0)
                .image(image)
                .spec(spec)
                .build();

        String field = String.format("%s_%d", CartInfo.class.getSimpleName(), itemId);
        String requestId = String.valueOf(toolContext.getContext().get(Constant.REQUEST_ID));
        ToolResultHolder.put(requestId, field, cartInfo);

        return cartInfo;
    }

    @Tool(name = Constant.Tools.VIEW_CART, description = "查看当前用户的购物车内容")
    public List<CartInfo> viewCart(ToolContext toolContext) {
        System.out.println("========== 调用了 viewCart ==========");

        ToolContextUtil.setUserContext(toolContext);

        return CartConverter.toCartInfoList(cartFeignClient.queryMyCarts());
    }

    @Tool(name = Constant.Tools.REMOVE_FROM_CART, description = "从购物车移除商品，需要先调用 viewCart 查看购物车获取商品ID")
    public void removeFromCart(
            @ToolParam(description = "要移除的商品ID") Long itemId,
            ToolContext toolContext) {
        System.out.println("========== 调用了 removeFromCart，商品ID: " + itemId + " ==========");

        ToolContextUtil.setUserContext(toolContext);

        cartFeignClient.deleteCartItemByIds(List.of(itemId));
    }
}