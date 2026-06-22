let codActual = "";
let paginaActual = 1;
let totalPaginas = 1;
const TAM_PAGINA = 10;

const $ = (id) => document.getElementById(id);

function mostrarMensaje(texto, esError = true) {
    const m = $("mensaje");
    m.textContent = texto;
    m.style.color = esError ? "#c0392b" : "#27ae60";
}

async function cargarPedidos() {
    if (!codActual) return;
    try {
        const resp = await fetch(`/api/repartidor/${encodeURIComponent(codActual)}/pedidos?page=${paginaActual}&size=${TAM_PAGINA}`);
        if (!resp.ok) { mostrarMensaje("No se pudieron cargar los pedidos."); return; }
        const data = await resp.json();
        totalPaginas = data.totalPaginas || 1;
        paginaActual = data.page || 1;
        pintarTabla(data.filas || []);
        $("infoPagina").textContent = `Página ${paginaActual} de ${totalPaginas}`;
        $("btnAnterior").disabled = paginaActual <= 1;
        $("btnSiguiente").disabled = paginaActual >= totalPaginas;
        if ((data.filas || []).length === 0) mostrarMensaje("No tienes pedidos asignados.", false);
        else mostrarMensaje("");
    } catch (e) {
        mostrarMensaje("Error de conexión con el servidor.");
    }
}

function pintarTabla(filas) {
    const cuerpo = $("cuerpoTabla");
    cuerpo.innerHTML = "";
    for (const f of filas) {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${f.codPedido ?? ""}</td>
            <td>${f.nombreCliente ?? ""}</td>
            <td>${f.direccionEntrega ?? ""}</td>
            <td>${f.metodoPago ?? ""}</td>
            <td>${f.estado ?? ""}</td>`;
        const tdAccion = document.createElement("td");
        if (f.estado === "EN CAMINO") {
            const btn = document.createElement("button");
            btn.className = "btn-entregar";
            btn.textContent = "Confirmar entrega";
            btn.onclick = () => confirmarEntrega(f.codPedido, btn);
            tdAccion.appendChild(btn);
        } else {
            tdAccion.textContent = "—";
        }
        tr.appendChild(tdAccion);
        cuerpo.appendChild(tr);
    }
}

async function confirmarEntrega(codPedido, btn) {
    btn.disabled = true;
    try {
        const resp = await fetch(`/api/pedidos/${encodeURIComponent(codPedido)}/entregar?repartidor=${encodeURIComponent(codActual)}`, { method: "POST" });
        const data = await resp.json();
        if (resp.ok) {
            mostrarMensaje(`Pedido ${codPedido} entregado.`, false);
        } else {
            mostrarMensaje(data.mensaje || "No se pudo confirmar la entrega.");
        }
    } catch (e) {
        mostrarMensaje("Error de conexión con el servidor.");
    } finally {
        cargarPedidos();
    }
}

$("btnBuscar").onclick = () => {
    const cod = $("codRepartidor").value.trim();
    if (!cod) { mostrarMensaje("Ingresa tu código de repartidor."); return; }
    codActual = cod;
    paginaActual = 1;
    cargarPedidos();
};

$("btnAnterior").onclick = () => { if (paginaActual > 1) { paginaActual--; cargarPedidos(); } };
$("btnSiguiente").onclick = () => { if (paginaActual < totalPaginas) { paginaActual++; cargarPedidos(); } };

// Refresco periódico de la tabla mientras haya un repartidor cargado.
setInterval(() => { if (codActual) cargarPedidos(); }, 5000);
