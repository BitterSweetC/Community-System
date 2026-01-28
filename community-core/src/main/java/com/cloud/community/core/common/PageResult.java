package com.cloud.community.core.common;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setList(page.getContent());
        result.setTotal(page.getTotalElements());
        result.setPage(page.getNumber() + 1); // Frontend usually 1-based, backend 0-based. Let's normalize to 1-based for response
        result.setSize(page.getSize());
        result.setTotalPages(page.getTotalPages());
        return result;
    }
    
    // Helper to wrap List directly if needed (e.g. no actual pagination but consistent format)
    public static <T> PageResult<T> of(List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(list.size());
        result.setPage(1);
        result.setSize(list.size());
        result.setTotalPages(1);
        return result;
    }

    public <R> PageResult<R> map(java.util.function.Function<T, R> mapper) {
        PageResult<R> result = new PageResult<>();
        result.setList(this.list.stream().map(mapper).collect(java.util.stream.Collectors.toList()));
        result.setTotal(this.total);
        result.setPage(this.page);
        result.setSize(this.size);
        result.setTotalPages(this.totalPages);
        return result;
    }
}
