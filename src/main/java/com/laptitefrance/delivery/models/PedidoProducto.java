package com.laptitefrance.delivery.models;

import java.util.Objects;

public class PedidoProducto {
    private String codProducto; // PK part
    private String codPedido; // PK part
    private Short cantProd;

    public PedidoProducto() {
    }

    public PedidoProducto(String codProducto, String codPedido, Short cantProd) {
        this.codProducto = codProducto;
        this.codPedido = codPedido;
        this.cantProd = cantProd;
    }

    public String getCodProducto() {
        return codProducto;
    }

    public void setCodProducto(String codProducto) {
        this.codProducto = codProducto;
    }

    public String getCodPedido() {
        return codPedido;
    }

    public void setCodPedido(String codPedido) {
        this.codPedido = codPedido;
    }

    public Short getCantProd() {
        return cantProd;
    }

    public void setCantProd(Short cantProd) {
        this.cantProd = cantProd;
    }

    /** PK compuesta: (CodProducto, CodPedido) */
    public static final class IdPedidoProducto {
        private final String codProducto;
        private final String codPedido;

        public IdPedidoProducto(String codProducto, String codPedido) {
            this.codProducto = codProducto;
            this.codPedido = codPedido;
        }

        public String getCodProducto() {
            return codProducto;
        }

        public String getCodPedido() {
            return codPedido;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IdPedidoProducto)) return false;
            IdPedidoProducto that = (IdPedidoProducto) o;
            return Objects.equals(codProducto, that.codProducto) && Objects.equals(codPedido, that.codPedido);
        }

        @Override
        public int hashCode() {
            return Objects.hash(codProducto, codPedido);
        }
    }
}

