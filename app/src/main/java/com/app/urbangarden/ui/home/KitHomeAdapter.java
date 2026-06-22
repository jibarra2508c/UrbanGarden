package com.app.urbangarden.ui.home;

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

import java.util.List;

/**
 * Adapter del RecyclerView horizontal de HomeFragment. Tarjeta compacta con
 * emoji, nombre y estado; al pulsarla navega al detalle de ese kit.
 */
public class KitHomeAdapter extends RecyclerView.Adapter<KitHomeAdapter.KitHomeViewHolder> {

    private List<Kit> listaKits;

    public KitHomeAdapter(List<Kit> listaKits) {
        this.listaKits = listaKits;
    }

    //.........................................................................
    // Adapter

    @NonNull
    @Override
    public KitHomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_kit_home, parent, false);
        return new KitHomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KitHomeViewHolder holder, int position) {
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

    public static class KitHomeViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvEmoji;
        private final TextView tvNombre;
        private final TextView tvEstado;

        public KitHomeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji  = itemView.findViewById(R.id.tvKitEmoji);
            tvNombre = itemView.findViewById(R.id.tvKitNombre);
            tvEstado = itemView.findViewById(R.id.tvKitEstado);
        }

        public void bind(Kit kit) {
            tvEmoji.setText(kit.getEmoji());
            tvNombre.setText(kit.getNombre());

            // Mismo estado de riego que en "Mis kits": <50% "Regar ya", si no "Óptimo".
            if (kit.necesitaRiego()) {
                tvEstado.setText("Regar ya");
                tvEstado.setBackgroundResource(R.drawable.bg_estado_seco_kit);
                configurarChipIcono(tvEstado, R.drawable.ic_notif_alerta);
            } else {
                tvEstado.setText("Óptimo");
                tvEstado.setBackgroundResource(R.drawable.bg_estado_ok_kit);
                configurarChipIcono(tvEstado, R.drawable.ic_ug_check);
            }

            // Click en la tarjeta → detalle de ese kit.
            itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("kitId", kit.getId());
                Navigation.findNavController(v).navigate(R.id.action_home_to_detalle, args);
            });
        }

        /** Coloca un icono vectorial a la izquierda del texto del chip, a 14dp y tintado. */
        private void configurarChipIcono(TextView chip, int iconoRes) {
            Drawable icono = ContextCompat.getDrawable(chip.getContext(), iconoRes);
            if (icono != null) {
                int tam = Math.round(14 * chip.getResources().getDisplayMetrics().density);
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
