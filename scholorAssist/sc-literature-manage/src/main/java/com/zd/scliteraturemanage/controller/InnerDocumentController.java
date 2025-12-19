package com.zd.scliteraturemanage.controller;

import com.zd.scapi.api.literature.DocumentApi;
import com.zd.scliteraturemanage.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents/inner")
@RequiredArgsConstructor
@Validated
@Tag(name = "内部接口")
public class InnerDocumentController implements DocumentApi {
    private final DocumentService documentService;
    @Override
    @GetMapping("/{categoryId}")
    @Operation(summary = "根据分类id查询是否有关联文档", description = "根据分类id查询是否有关联文档")
    public Boolean hasDocumentByCategoryId(@PathVariable("categoryId") String categoryId) {
        return documentService.hasDocumentByCategoryId(categoryId);
    }
}
