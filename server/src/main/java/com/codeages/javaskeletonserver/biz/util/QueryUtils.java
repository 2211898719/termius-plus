package com.codeages.javaskeletonserver.biz.util;

import cn.hutool.core.collection.CollectionUtil;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QueryUtils {

    /**
     * 批量查询，数据拼装
     * one to one 类型
     *
     * @param list               需要拼入数据的list
     * @param getJoinFieldKeyFun 获取关联的字段的方法
     * @param idInQueryFun       使用关联字段批量查询关联对象的方法
     * @param joinFieldGetKeyFun 关联对象获取关联字段的方法
     * @param execFun            拼入数据的方法
     * @param <T>                需要拼入数据的list的类型
     * @param <E>                关联对象的类型
     */
    public static <T, E> void batchQueryJoinTable(List<E> list,
                                                  Function<E, Long> getJoinFieldKeyFun,
                                                  Function<List<Long>, List<T>> idInQueryFun,
                                                  Function<T, Long> joinFieldGetKeyFun,
                                                  BiConsumer<E, T> execFun) {

        List<Long> joinIds = list.stream().map(getJoinFieldKeyFun).collect(Collectors.toList());
        List<T> joinElements = idInQueryFun.apply(joinIds);
        Map<Long, T> collect = joinElements.stream().collect(Collectors.toMap(joinFieldGetKeyFun, Function.identity()));

        list.forEach(element -> {
            T el = collect.get(getJoinFieldKeyFun.apply(element));
            if (el == null) {
                return;
            }

            execFun.accept(element, el);
        });
    }


}
