package com.laptitefrance.delivery.models;

import java.time.LocalDateTime;
import java.util.Objects;

public class Atencion {
    private String idCliente; // PK part
    private String codAsistente; // PK part
    private LocalDateTime fechaAtencion;

    public Atencion() {
    }

    public Atencion(String idCliente, String codAsistente, LocalDateTime fechaAtencion) {
        this.idCliente = idCliente;
        this.codAsistente = codAsistente;
        this.fechaAtencion = fechaAtencion;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getCodAsistente() {
        return codAsistente;
    }

    public void setCodAsistente(String codAsistente) {
        this.codAsistente = codAsistente;
    }

    public LocalDateTime getFechaAtencion() {
        return fechaAtencion;
    }

    public void setFechaAtencion(LocalDateTime fechaAtencion) {
        this.fechaAtencion = fechaAtencion;
    }

    /** PK compuesta: (IDCliente, CodAsistente) */
    public static final class IdAtencion {
        private final String idCliente;
        private final String codAsistente;

        public IdAtencion(String idCliente, String codAsistente) {
            this.idCliente = idCliente;
            this.codAsistente = codAsistente;
        }

        public String getIdCliente() {
            return idCliente;
        }

        public String getCodAsistente() {
            return codAsistente;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IdAtencion)) return false;
            IdAtencion that = (IdAtencion) o;
            return Objects.equals(idCliente, that.idCliente) && Objects.equals(codAsistente, that.codAsistente);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idCliente, codAsistente);
        }
    }
}

