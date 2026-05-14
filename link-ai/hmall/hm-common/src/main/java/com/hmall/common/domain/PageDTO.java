package com.hmall.common.domain;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.common.utils.Convert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDTO<T> {
    protected Long total;
    protected Long pages;
    protected List<T> list;

    // 无论后端查出的是什么类型（User、Order、Item……）
// 统一返回给前端都是这种结构：

    public static <T> PageDTO<T> empty(Long total, Long pages) {
        return new PageDTO<>(total, pages, CollUtils.emptyList());
    }
    public static <T> PageDTO<T> empty(Page<?> page) {
        return new PageDTO<>(page.getTotal(), page.getPages(), CollUtils.emptyList());
    }

    public static <T> PageDTO<T> of(Page<T> page) {    //无转换（直接返回）
        if(page == null){
            return new PageDTO<>(); // 空分页结果
        }
        if (CollUtils.isEmpty(page.getRecords())) {
            return empty(page);     // 无数据，返回 total=0, pages=0, list=[]
        }
        return new PageDTO<>(page.getTotal(), page.getPages(), page.getRecords());
        //                                        ↑ total       ↑ pages    ↑ 数据列表
    }
    public static <T,R> PageDTO<T> of(Page<R> page, Function<R, T> mapper) {
        if(page == null){
            return new PageDTO<>();
        }
        if (CollUtils.isEmpty(page.getRecords())) {
            return empty(page);
        }
        return new PageDTO<>(page.getTotal(), page.getPages(),
                page.getRecords().stream().map(mapper).collect(Collectors.toList()));
        //                              ↑ 将 R 类型列表通过 mapper 转换为 T 类型列表

    }
    public static <T> PageDTO<T> of(Page<?> page, List<T> list) {
        return new PageDTO<>(page.getTotal(), page.getPages(), list);
    }

    public static <T, R> PageDTO<T> of(Page<R> page, Class<T> clazz) {
        // BeanUtils.copyList 底层遍历 records，调用 BeanUtils.copyProperties 逐个拷贝
        return new PageDTO<>(page.getTotal(), page.getPages(), BeanUtils.copyList(page.getRecords(), clazz));
    }

    public static <T, R> PageDTO<T> of(Page<R> page, Class<T> clazz, Convert<R, T> convert) {
        return new PageDTO<>(page.getTotal(), page.getPages(), BeanUtils.copyList(page.getRecords(), clazz, convert));
    }
}
