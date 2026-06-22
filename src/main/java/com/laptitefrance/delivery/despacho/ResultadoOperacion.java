package com.laptitefrance.delivery.despacho;

/** Resultado de una operación de despacho; se mapea a un código HTTP en la capa REST. */
public class ResultadoOperacion {

    public enum Tipo {
        OK, YA_TOMADO, NO_ENCONTRADO, REPARTIDOR_NO_DISPONIBLE, ERROR_INTERNO
    }

    private final Tipo tipo;
    private final String mensaje;
    private final Object datos;

    public ResultadoOperacion(Tipo tipo, String mensaje, Object datos) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.datos = datos;
    }

    public static ResultadoOperacion ok(String mensaje, Object datos) {
        return new ResultadoOperacion(Tipo.OK, mensaje, datos);
    }

    public static ResultadoOperacion yaTomado(String mensaje) {
        return new ResultadoOperacion(Tipo.YA_TOMADO, mensaje, null);
    }

    public static ResultadoOperacion noEncontrado(String mensaje) {
        return new ResultadoOperacion(Tipo.NO_ENCONTRADO, mensaje, null);
    }

    public static ResultadoOperacion repartidorNoDisponible(String mensaje) {
        return new ResultadoOperacion(Tipo.REPARTIDOR_NO_DISPONIBLE, mensaje, null);
    }

    public static ResultadoOperacion errorInterno(String mensaje) {
        return new ResultadoOperacion(Tipo.ERROR_INTERNO, mensaje, null);
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Object getDatos() {
        return datos;
    }

    public int httpStatus() {
        switch (tipo) {
            case OK:                        return 200;
            case YA_TOMADO:                 return 409;
            case NO_ENCONTRADO:             return 404;
            case REPARTIDOR_NO_DISPONIBLE:  return 400;
            case ERROR_INTERNO:
            default:                        return 500;
        }
    }
}
