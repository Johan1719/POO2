package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.dtos.PedidoRepartidorRow;

import java.util.List;

/** Página de pedidos de un repartidor, con metadatos para "Página X de N". */
public class PaginaRepartidor {
    public final List<PedidoRepartidorRow> filas;
    public final int page;
    public final int totalPaginas;
    public final long totalItems;

    public PaginaRepartidor(List<PedidoRepartidorRow> filas, int page, int totalPaginas, long totalItems) {
        this.filas = filas;
        this.page = page;
        this.totalPaginas = totalPaginas;
        this.totalItems = totalItems;
    }
}
