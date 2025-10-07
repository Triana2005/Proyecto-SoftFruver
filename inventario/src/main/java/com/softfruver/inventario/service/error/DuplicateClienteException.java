package com.softfruver.inventario.service.error;

public class DuplicateClienteException extends RuntimeException {
  public DuplicateClienteException(String message) {
    super(message);
  }
}
