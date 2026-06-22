package com.app.urbangarden.data;

import com.app.urbangarden.model.Kit;
import com.app.urbangarden.model.Notificacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Genera notificaciones in-app en vivo a partir del estado de cada kit (no se
 * guardan en BD), así la lista siempre refleja la situación real.
 *
 * Reglas:
 *   - Humedad real < 50%                                    → toca regar
 *   - Progreso >= 95%                                       → listo para cosechar
 *   - Edad >= {@link Kit#DIAS_TRASPLANTE} y sin trasplantar → trasplante
 *   - Edad real <= 2 días                                   → kit nuevo
 */
public final class GeneradorNotificaciones {

    private GeneradorNotificaciones() {} // utilidad, no se instancia

    /** Recorre los kits y devuelve las notificaciones aplicables, en orden de detección. */
    public static List<Notificacion> generar(List<Kit> kits) {
        List<Notificacion> notificaciones = new ArrayList<>();

        if (kits == null) return notificaciones;

        for (Kit kit : kits) {
            String nombre = kit.getNombre() != null ? kit.getNombre() : "Tu planta";

            // Toca regar: la humedad ha bajado del 50%.
            if (kit.necesitaRiego()) {
                notificaciones.add(new Notificacion(
                        nombre + " necesita riego",
                        "La humedad ha bajado del 50%. Riégala para devolverla al 100%.",
                        kit,
                        Notificacion.Tipo.RIEGO
                ));
            }

            // Listo para cosechar.
            if (kit.getProgresoReal() >= 95) {
                notificaciones.add(new Notificacion(
                        nombre + " listo para cosechar",
                        "¡Enhorabuena! Tu cultivo ha completado su ciclo.",
                        kit,
                        Notificacion.Tipo.COSECHA
                ));
            }

            // Trasplante: ya toca y el usuario no lo ha marcado aún.
            if (kit.getDiasReales() >= Kit.DIAS_TRASPLANTE && !kit.isTrasplantado()) {
                notificaciones.add(new Notificacion(
                        "Trasplanta " + nombre,
                        "Lleva " + kit.getDiasReales() + " días. Pásala a una maceta mayor.",
                        kit,
                        Notificacion.Tipo.TRASPLANTE
                ));
            }

            // Kit recién añadido (0-2 días).
            if (kit.getDiasReales() <= 2) {
                notificaciones.add(new Notificacion(
                        nombre + " añadido a tu huerto",
                        "Empieza a cuidarlo: consulta sus necesidades en el detalle.",
                        kit,
                        Notificacion.Tipo.NUEVO
                ));
            }
        }

        return notificaciones;
    }
}
