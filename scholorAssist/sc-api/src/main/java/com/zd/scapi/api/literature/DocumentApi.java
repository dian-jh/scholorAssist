package com.zd.scapi.api.literature;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 内部接口 - 文档相关接口
 *
 * @author dian
 */
//contextId 指定FeignClient实例的上下文id，如果不设置默认为类名，value指定微服务的名称，path:指定接口地址
@FeignClient(contextId = "sc-literature", value = "sc-literature-manage", path = "/api/documents/inner")
public interface DocumentApi {
//    根据分类id查询是否有关联文档
    @GetMapping("/{categoryId}")
    Boolean hasDocumentByCategoryId(@PathVariable("categoryId") String categoryId);
}
