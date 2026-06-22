package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.models.Pedido;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakePedidoRepositoryTest {

    @Test
    void insertAutogeneraCodigoYFindByIdLoEncuentra() {
        FakePedidoRepository repo = new FakePedidoRepository();
        Pedido p = new Pedido();
        p.setEstado("EN ESPERA");
        repo.insert(p);

        assertEquals("P0001", p.getCodPedido());
        assertTrue(repo.findById("P0001").isPresent());
        assertEquals("EN ESPERA", repo.findById("P0001").get().getEstado());
    }
}
