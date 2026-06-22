package com.app.urbangarden.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.app.urbangarden.R;
import com.app.urbangarden.model.Notificacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de la lista de notificaciones in-app. Muestra icono (según el tipo de
 * aviso), título y descripción, y navega al pulsar según el tipo.
 */
public class NotificacionAdapter
        extends RecyclerView.Adapter<NotificacionAdapter.NotifViewHolder> {

    private List<Notificacion> notificaciones;

    public NotificacionAdapter(List<Notificacion> notificaciones) {
        this.notificaciones = notificaciones != null ? notificaciones : new ArrayList<>();
    }

    public void actualizarLista(List<Notificacion> nuevas) {
        this.notificaciones = nuevas != null ? nuevas : new ArrayList<>();
        notifyDataSetChanged();
    }

    //.........................................................................
    // Adapter

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notificacion, parent, false);
        return new NotifViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        holder.bind(notificaciones.get(position));
    }

    @Override
    public int getItemCount() {
        return notificaciones.size();
    }

    //.........................................................................
    // ViewHolder

    static class NotifViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivIcono;
        private final TextView tvTitulo;
        private final TextView tvDescripcion;

        NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcono       = itemView.findViewById(R.id.ivNotifIcono);
            tvTitulo      = itemView.findViewById(R.id.tvNotifTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvNotifDescripcion);
        }

        void bind(Notificacion notif) {
            tvTitulo.setText(notif.getTitulo());
            tvDescripcion.setText(notif.getDescripcion());

            // Icono según el tipo: alerta (riego), maceta (trasplante/cosecha), brote (nuevo).
            int iconoRes;
            switch (notif.getTipo()) {
                case RIEGO:
                    iconoRes = R.drawable.ic_notif_alerta;
                    break;
                case NUEVO:
                    iconoRes = R.drawable.ic_ug_plant;
                    break;
                default: // TRASPLANTE, COSECHA
                    iconoRes = R.drawable.ic_ug_pot;
                    break;
            }
            // Iconos en verde (color_primary); título en negro.
            ivIcono.setImageResource(iconoRes);
            ivIcono.setColorFilter(
                    ContextCompat.getColor(itemView.getContext(), R.color.color_primary));
            tvTitulo.setTextColor(
                    ContextCompat.getColor(itemView.getContext(), R.color.color_on_surface));

            // Navegación según el tipo: riego → kits, trasplante → herramienta, resto → detalle.
            if (notif.getKit() != null) {
                itemView.setClickable(true);
                itemView.setOnClickListener(v -> {
                    androidx.navigation.NavController nav = Navigation.findNavController(v);
                    if (notif.getTipo() == Notificacion.Tipo.RIEGO) {
                        nav.navigate(R.id.navigation_kits);
                    } else if (notif.getTipo() == Notificacion.Tipo.TRASPLANTE) {
                        Bundle bundle = new Bundle();
                        bundle.putString("kitId", notif.getKit().getId());
                        nav.navigate(R.id.action_notificaciones_to_trasplante, bundle);
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putString("kitId", notif.getKit().getId());
                        nav.navigate(R.id.action_notificaciones_to_detalle, bundle);
                    }
                });
            } else {
                itemView.setOnClickListener(null);
                itemView.setClickable(false);
            }
        }
    }
}
