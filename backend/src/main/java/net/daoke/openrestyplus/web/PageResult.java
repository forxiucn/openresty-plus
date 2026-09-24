package net.daoke.openrestyplus.web;

import java.util.List;

/** 统一的服务端分页响应，页码从 0 开始。 */
public record PageResult<T>(List<T> items, long total, int page, int size, int totalPages) {
    public static <T> PageResult<T> of(List<T> values, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 200);
        int from = Math.min(normalizedPage * normalizedSize, values.size());
        int to = Math.min(from + normalizedSize, values.size());
        int pages = values.isEmpty() ? 0 : (values.size() + normalizedSize - 1) / normalizedSize;
        return new PageResult<>(List.copyOf(values.subList(from, to)), values.size(), normalizedPage, normalizedSize, pages);
    }
}
