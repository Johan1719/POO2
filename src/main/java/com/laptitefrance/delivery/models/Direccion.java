package com.laptitefrance.delivery.models;

import java.util.Objects;

public class Direccion {
    private String ubicacion; // Ubicacion VARCHAR(30)
    private String distrito; // distrito VARCHAR(30)
    private String areaLoc; // AreaLoc VARCHAR(30)
    private String idCliente; // FK -> Cliente(IDCliente)

    public Direccion() {
    }

    public Direccion(String ubicacion, String distrito, String areaLoc, String idCliente) {
        this.ubicacion = ubicacion;
        this.distrito = distrito;
        this.areaLoc = areaLoc;
        this.idCliente = idCliente;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getAreaLoc() {
        return areaLoc;
    }

    public void setAreaLoc(String areaLoc) {
        this.areaLoc = areaLoc;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    /**
     * PK compuesta (Ubicacion, distrito, AreaLoc, IDCliente)
     */
    public static final class IdDireccion {
        private final String ubicacion;
        private final String distrito;
        private final String areaLoc;
        private final String idCliente;

        public IdDireccion(String ubicacion, String distrito, String areaLoc, String idCliente) {
            this.ubicacion = ubicacion;
            this.distrito = distrito;
            this.areaLoc = areaLoc;
            this.idCliente = idCliente;
        }

        public String getUbicacion() {
            return ubicacion;
        }

        public String getDistrito() {
            return distrito;
        }

        public String getAreaLoc() {
            return areaLoc;
        }

        public String getIdCliente() {
            return idCliente;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IdDireccion)) return false;
            IdDireccion that = (IdDireccion) o;
            return Objects.equals(ubicacion, that.ubicacion)
                    && Objects.equals(distrito, that.distrito)
                    && Objects.equals(areaLoc, that.areaLoc)
                    && Objects.equals(idCliente, that.idCliente);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ubicacion, distrito, areaLoc, idCliente);
        }
    }
}

