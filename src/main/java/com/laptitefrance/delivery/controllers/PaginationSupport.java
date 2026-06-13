package com.laptitefrance.delivery.controllers;

/**
 * Utilidad mínima para paginado en UI (solo usa page/pageSize).
 */
public final class PaginationSupport {
    private PaginationSupport() {
    }

    public static int normalizePage(int page) {
        return Math.max(1, page);
    }

    public static int normalizePageSize(int pageSize) {
        return Math.max(1, pageSize);
    }

    /**
     * Devuelve el offset para SQL con OFFSET/FETCH.
     */
    public static int offsetOf(int page, int pageSize) {
        return (page - 1) * pageSize;
    }
}

