package com.app.urbangarden.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.app.urbangarden.data.GeneradorNotificaciones;
import com.app.urbangarden.data.KitRepository;
import com.app.urbangarden.databinding.FragmentNotificationsBinding;
import com.app.urbangarden.model.Notificacion;

import java.util.List;

/**
 * Pantalla de notificaciones in-app. No se almacenan: se generan en vivo del
 * estado de los kits ({@link GeneradorNotificaciones}). Observa el LiveData del
 * repositorio, así la lista se regenera sola cuando cambian los kits. Si no hay
 * ninguna, muestra un estado vacío.
 */
public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private NotificacionAdapter adapter;

    //.........................................................................
    // Ciclo de vida

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);

        setupRecyclerView();
        observarNotificaciones();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //.........................................................................
    // Carga de notificaciones (en vivo)

    private void setupRecyclerView() {
        adapter = new NotificacionAdapter(null);
        binding.rvNotificaciones.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvNotificaciones.setAdapter(adapter);
    }

    /** Observa los kits y regenera las notificaciones con cada cambio. */
    private void observarNotificaciones() {
        KitRepository.getInstance().getKitsLiveData()
                .observe(getViewLifecycleOwner(), kits -> {
                    List<Notificacion> notificaciones = GeneradorNotificaciones.generar(kits);
                    if (notificaciones.isEmpty()) {
                        binding.rvNotificaciones.setVisibility(View.GONE);
                        binding.layoutVacio.setVisibility(View.VISIBLE);
                    } else {
                        binding.rvNotificaciones.setVisibility(View.VISIBLE);
                        binding.layoutVacio.setVisibility(View.GONE);
                        adapter.actualizarLista(notificaciones);
                    }
                });
    }
}
