(function () {
  function cloneSelectProducto() {
    const proto = document.getElementById('protoSelectProducto');
    const sel = proto.cloneNode(true);
    sel.removeAttribute('id');
    sel.className = 'sf-input';
    sel.name = 'productoId';
    sel.required = true;
    return sel;
  }

  // --- Conecta eventos a una fila y calcula su subtotal ---
  function wireFila(tr) {
    const selProd = tr.querySelector('select[name="productoId"]');
    const inCant  = tr.querySelector('input[name="cantidadKg"]');
    const inPrec  = tr.querySelector('input[name="precioUnit"]');
    const tdSub   = tr.querySelector('.subtotal');
    const btnQuit = tr.querySelector('.btn-quitar');

    function recalc() {
      const c = parseFloat((inCant && inCant.value) || '0');
      const p = parseFloat((inPrec && inPrec.value) || '0');
      const sub = c * p;
      if (tdSub) tdSub.textContent = isFinite(sub) ? sub.toFixed(2) : '0.00';
    }

    if (inCant) inCant.addEventListener('input', recalc);
    if (inPrec) inPrec.addEventListener('input', recalc);

    if (btnQuit) {
      btnQuit.addEventListener('click', function () {
        const tbody = document.getElementById('tbodyItems');
        const filas = tbody ? tbody.querySelectorAll('tr.fila-item') : [];
        // Evita quedarse sin filas
        if (filas.length > 1) {
          tr.remove();
        } else {
          if (selProd) selProd.value = '';
          if (inCant)  inCant.value = '';
          if (inPrec)  inPrec.value = '';
          if (tdSub)   tdSub.textContent = '0.00';
        }
      });
    }

    // Calcular al cargar (útil en editar)
    recalc();
  }

  function nuevaFila() {
    const trProto = document.getElementById('protoFila');
    const tr = trProto.cloneNode(true);
    tr.removeAttribute('id');

    // Insertar select de producto en la primera celda
    const td0 = tr.children[0];
    td0.innerHTML = '';
    td0.appendChild(cloneSelectProducto());

    // Limpiar inputs de la nueva fila
    const inCant = tr.querySelector('input[name="cantidadKg"]');
    const inPrec = tr.querySelector('input[name="precioUnit"]');
    const tdSub  = tr.querySelector('.subtotal');
    if (inCant) inCant.value = '';
    if (inPrec) inPrec.value = '';
    if (tdSub)  tdSub.textContent = '0.00';

    // Conectar eventos
    wireFila(tr);

    document.getElementById('tbodyItems').appendChild(tr);
  }

  // Hook de "Agregar ítem" y wiring de TODAS las filas existentes
  document.addEventListener('DOMContentLoaded', function () {
    const btnAdd = document.getElementById('agregarFila');
    if (btnAdd) btnAdd.addEventListener('click', nuevaFila);

    // Conectar a todas las filas ya presentes (nuevo/editar)
    const filas = document.querySelectorAll('#tbodyItems tr.fila-item');
    filas.forEach(wireFila);
  });
})();
