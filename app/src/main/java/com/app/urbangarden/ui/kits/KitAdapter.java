package com.app.urbangarden.ui.kits;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.app.urbangarden.R;
import com.app.urbangarden.model.Kit;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adapter del RecyclerView vertical de KitsFragment. Tarjeta completa por kit
 * con sus métricas y un botón "Regar ahora" (solo visible cuando toca regar)
 * que sube la humedad al 100% y notifica al fragment para refrescar la lista.
 */
public class KitAdapter extends RecyclerView.Adapter<KitAdapter.KitViewHolder> {

    /** Callback que se dispara cuando el usuario riega un kit. */
    public interface OnKitRegadoListener {
        void onKitRegado(Kit kit);
    }

    private List<Kit> listaKits;
    private final OnKitRegadoListener listenerRiego;

    public KitAdapter(List<Kit> listaKits, OnKitRegadoListener listenerRiego) {
        this.listaKits = listaKits;
        this.listenerRiego = listenerRiego;
    }

    //.........................................................................
    // Adapter

    @NonNull
    @Override
    public KitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_kit_kits, parent, false);
        return new KitViewHolder(view, listenerRiego);
    }

    @Override
    public void onBindViewHolder(@NonNull KitViewHolder holder, int position) {
        holder.bind(listaKits.get(position));
    }

    @Override
    public int getItemCount() {
        return listaKits != null ? listaKits.size() : 0;
    }

    public void actualizarLista(List<Kit> nuevaLista) {
        this.listaKits = nuevaLista;
        notifyDataSetChanged();
    }

    //.........................................................................
    // ViewHolder

    public static class KitViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvEmoji;
        private final TextView tvNombre;
        private final TextView tvEspecie;
        private final TextView tvEstado;
        private final TextView tvHumedad;
        private final TextView tvTemperatura;
        private final TextView tvLuz;
        private final TextView tvEdad;
        private final MaterialButton btnRegar;
        private final OnKitRegadoListener listenerRiego;

        public KitViewHolder(@NonNull View itemView, OnKitRegadoListener listenerRiego) {
            super(itemView);
            this.listenerRiego = listenerRiego;
            tvEmoji       = itemView.findViewById(R.id.tvKitEmoji);
            tvNombre      = itemView.findViewById(R.id.tvKitNombre);
            tvEspecie     = itemView.findViewById(R.id.tvKitEspecie);
            tvEstado      = itemView.findViewById(R.id.tvKitEstado);
            tvHumedad     = itemView.findViewById(R.id.tvHumedad);
            tvTemperatura = itemView.findViewById(R.id.tvTemperatura);
            tvLuz         = itemView.findViewById(R.id.tvLuz);
            tvEdad        = itemView.findViewById(R.id.tvEdad);
            btnRegar      = itemView.findViewById(R.id.btnRegarKit);
        }

        public void bind(Kit kit) {
            tvEmoji.setText(kit.getEmoji());
            tvNombre.setText(kit.getNombre());
            tvEspecie.setText(kit.getEspecie());
            tvHumedad.setText(kit.getHumedadReal() + "%");
            tvTemperatura.setText((int) kit.getTemperatura() + "°C");
            tvLuz.setText(kit.getHorasLuz() + "h");
            tvEdad.setText(kit.getDiasReales() + "d");

            // Estado de riego según la humedad real: <50% "Regar ya", si no "Óptimo".
            if (kit.necesitaRiego()) {
                tvEstado.setText("Regar ya");
                tvEstado.setBackgroundResource(R.drawable.bg_estado_seco_kit);
                ponerIconoChip(tvEstado, R.drawable.ic_notif_alerta);
            } else {
                tvEstado.setText("Óptimo");
                tvEstado.setBackgroundResource(R.drawable.bg_estado_ok_kit);
                ponerIconoChip(tvEstado, R.drawable.ic_ug_check);
            }

            // Click en la tarjeta → detalle del kit.
            itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("kitId", kit.getId());
                Navigation.findNavController(v)
                        .navigate(R.id.action_kits_to_detalle, bundle);
            });

            // El botón "Regar ahora" solo aparece cuando toca regar (humedad <50%).
            if (btnRegar != null) {
                btnRegar.setVisibility(kit.necesitaRiego() ? View.VISIBLE : View.GONE);
                btnRegar.setOnClickListener(v -> {
                    kit.regar();
                    if (listenerRiego != null) {
                        listenerRiego.onKitRegado(kit);
                    }
                });
            }
        }

        /** Coloca un icono vectorial a la izquierda del texto del chip, con tinte neutro. */
        private void ponerIconoChip(TextView chip, int iconoRes) {
            Drawable icono = ContextCompat.getDrawable(chip.getContext(), iconoRes);
            if (icono != null) {
                int tam = Math.round(13 * chip.getResources().getDisplayMetrics().density);
                icono.setBounds(0, 0, tam, tam);
            }
            chip.setCompoundDrawablesRelative(icono, null, null, null);
            chip.setCompoundDrawablePadding(
                    Math.round(4 * chip.getResources().getDisplayMetrics().density));
            TextViewCompat.setCompoundDrawableTintList(chip,
                    ColorStateList.valueOf(
                            ContextCompat.getColor(chip.getContext(), R.color.color_on_surface)));
        }
    }
}
