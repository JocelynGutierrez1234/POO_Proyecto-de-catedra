package com.taquicardia.dao;

// Error de regla de negocio al cobrar o anular (el mensaje se muestra al usuario)
public class CobroException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Integer idFacturaExistente;

    public CobroException(String mensaje) {
        this(mensaje, null);
    }

    public CobroException(String mensaje, Integer idFacturaExistente) {
        super(mensaje);
        this.idFacturaExistente = idFacturaExistente;
    }

    public Integer getIdFacturaExistente() {
        return idFacturaExistente;
    }
}
