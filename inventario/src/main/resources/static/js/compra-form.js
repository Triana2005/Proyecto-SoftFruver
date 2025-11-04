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

    // Calcula al cargar (útil en editar)
    recalc();
  }

  function nuevaFila() {
    const trProto = document.getElementById('protoFila');
    const tr = trProto.cloneNode(true);
    tr.removeAttribute('id');

    const td0 = tr.children[0];
    td0.innerHTML = '';
    td0.appendChild(cloneSelectProducto());

    const inCant = tr.querySelector('input[name="cantidadKg"]');
    const inPrec = tr.querySelector('input[name="precioUnit"]');
    const tdSub  = tr.querySelector('.subtotal');
    if (inCant) inCant.value = '';
    if (inPrec) inPrec.value = '';
    if (tdSub)  tdSub.textContent = '0.00';

    wireFila(tr);

    document.getElementById('tbodyItems').appendChild(tr);
  }

  document.addEventListener('DOMContentLoaded', function () {
    const btnAdd = document.getElementById('agregarFila');
    if (btnAdd) btnAdd.addEventListener('click', nuevaFila);

    const filas = document.querySelectorAll('#tbodyItems tr.fila-item');
    filas.forEach(wireFila);
  });
})();
