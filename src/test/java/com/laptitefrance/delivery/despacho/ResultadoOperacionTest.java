package com.laptitefrance.delivery.despacho;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultadoOperacionTest {

    @Test
    void fabricasYMapeoHttp() {
        assertEquals(200, ResultadoOperacion.ok("listo", null).httpStatus());
        assertEquals(409, ResultadoOperacion.yaTomado("ya fue tomado").httpStatus());
        assertEquals(404, ResultadoOperacion.noEncontrado("no existe").httpStatus());
        assertEquals(400, ResultadoOperacion.repartidorNoDisponible("ocupado").httpStatus());
        assertEquals(500, ResultadoOperacion.errorInterno("fallo BD").httpStatus());
        assertEquals(ResultadoOperacion.Tipo.OK, ResultadoOperacion.ok("x", null).getTipo());
        assertEquals("ya fue tomado", ResultadoOperacion.yaTomado("ya fue tomado").getMensaje());
    }
}
