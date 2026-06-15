package com.laptitefrance.delivery.controllers;

import java.util.List;

public class PaginationModels {

    public static class PageResult<T> {
        private final List<T> items;
        private final int page;
        private final int pageSize;
        private final int totalItems;
        private final int totalPages;

        public PageResult(List<T> items, int page, int pageSize, int totalItems, int totalPages) {
            this.items = items;
            this.page = page;
            this.pageSize = pageSize;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
        }

        public List<T> getItems() {
            return items;
        }

        public int getPage() {
            return page;
        }

        public int getPageSize() {
            return pageSize;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public int getTotalPages() {
            return totalPages;
        }
    }
}


