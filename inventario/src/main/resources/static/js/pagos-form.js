(function () {
  const $ = (sel) => document.querySelector(sel);

  function setDisabled(el, disabled) {
    if (!el) return;
    el.disabled = !!disabled;
    if (disabled) {
      el.value = '';
      el.classList.add('opacity-70');
    } else {
      el.classList.remove('opacity-70');
    }
  }

  function onTipoChange() {
    const tipo = $('#tipo').value;
    const selCli = $('#selCliente');
    const selProv = $('#selProveedor');
    const refId = $('#refId');

    if (tipo === 'CLIENTE') {
      setDisabled(selCli, false);
      setDisabled(selProv, true);
      refId.value = selCli.value || '';
    } else if (tipo === 'PROVEEDOR') {
      setDisabled(selCli, true);
      setDisabled(selProv, false);
      refId.value = selProv.value || '';
    } else {
      setDisabled(selCli, true);
      setDisabled(selProv, true);
      refId.value = '';
    }
  }

  function onRefChange() {
    const tipo = $('#tipo').value;
    const selCli = $('#selCliente');
    const selProv = $('#selProveedor');
    const refId = $('#refId');

    if (tipo === 'CLIENTE') {
      refId.value = selCli.value || '';
    } else if (tipo === 'PROVEEDOR') {
      refId.value = selProv.value || '';
    } else {
      refId.value = '';
    }
  }

  function onSubmit(e) {
    const tipo = $('#tipo').value;
    const refId = $('#refId').value;
    if (!tipo) {
      e.preventDefault();
      alert('Seleccione el tipo de pago (Cliente o Proveedor).');
      return;
    }
    if (!refId) {
      e.preventDefault();
      alert(tipo === 'CLIENTE'
        ? 'Seleccione el cliente.'
        : 'Seleccione el proveedor.');
      return;
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    const tipo = $('#tipo');
    const selCli = $('#selCliente');
    const selProv = $('#selProveedor');
    const form = $('#formPago');

    if (tipo) tipo.addEventListener('change', onTipoChange);
    if (selCli) selCli.addEventListener('change', onRefChange);
    if (selProv) selProv.addEventListener('change', onRefChange);
    if (form) form.addEventListener('submit', onSubmit);

    // Estado inicial por si el servidor precargó valores
    onTipoChange();
    onRefChange();
  });
})();
