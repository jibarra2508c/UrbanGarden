package com.app.urbangarden.model;

/**
 * Modelo de una notificación in-app.
 *
 * Representa un aviso que se muestra al usuario dentro de la app (no es una
 * notificación push del sistema). Se generan en vivo a partir del estado de
 * los kits: riego pendiente, cosecha lista, kit recién añadido, etc.
 */
public class Notificacion {

    /**
     * Tipo del aviso. Determina tanto el ICONO como el DESTINO de navegación
     * al pulsar la tarjeta:
     *   RIEGO      → lista de kits
     *   TRASPLANTE → herramienta de trasplante de la planta
     *   COSECHA / NUEVO → detalle del kit
     */
    public enum Tipo {
        RIEGO, TRASPLANTE, COSECHA, NUEVO
    }

    private final String titulo;
    private final String descripcion;
    /** Kit que originó el aviso; permite navegar a su detalle al pulsar. */
    private final Kit kit;
    private final Tipo tipo;

    public Notificacion(String titulo, String descripcion, Kit kit, Tipo tipo) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.kit = kit;
        this.tipo = tipo;
    }

    //.........................................................................
    // Getters

    public String getTitulo()       { return titulo; }
    public String getDescripcion()  { return descripcion; }
    public Kit getKit()             { return kit; }
    public Tipo getTipo()           { return tipo; }
}
