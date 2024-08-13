package com.codeages.termiusplus.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClashProxyDTO {
    //直连列表
    private List<String> direct;
    //代理列表
    private List<String> proxy;
    //拒绝列表 广告啥的
    private List<String> reject;
}
