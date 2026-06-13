package com.laptitefrance.delivery.controllers;

import java.util.List;

public class PaginationModels {

    public record PageResult<T>(
            List<T> items,
            int page,
            int pageSize,
            int totalItems,
            int totalPages
    ) {
    }
}

