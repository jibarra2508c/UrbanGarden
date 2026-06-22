package com.app.urbangarden.ui.perfil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.app.urbangarden.LoginActivity;
import com.app.urbangarden.data.SesionManager;
import com.app.urbangarden.databinding.FragmentPerfilBinding;

/**
 * Pantalla de perfil: muestra los datos del usuario (sesión local) y ofrece las
 * opciones de cuenta, incluido cerrar sesión.
 */
public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding binding;

    //.........................................................................
    // Ciclo de vida

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        setupUI();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //.........................................................................
    // UI

    private void setupUI() {
        // Datos del usuario desde la sesión guardada (SharedPreferences).
        SesionManager sesion = new SesionManager(requireContext());
        binding.tvPerfilNombre.setText(sesion.getNombre());
        binding.tvPerfilEmail.setText(sesion.getEmail());

        binding.rowPerfil.setOnClickListener(v ->
                Toast.makeText(getContext(), "Mi perfil", Toast.LENGTH_SHORT).show());

        binding.rowPerfil.setOnClickListener(v ->
                Toast.makeText(getContext(), "Notificaciones", Toast.LENGTH_SHORT).show());

        binding.rowSincronizacion.setOnClickListener(v ->
                Toast.makeText(getContext(), "Sincronizacion", Toast.LENGTH_SHORT).show());

        binding.rowPlan.setOnClickListener(v ->
                Toast.makeText(getContext(), "Plan y suscripcion", Toast.LENGTH_SHORT).show());

        binding.rowSoporte.setOnClickListener(v ->
                Toast.makeText(getContext(), "Soporte tecnico", Toast.LENGTH_SHORT).show());

        binding.rowCerrarSesion.setOnClickListener(v -> mostrarDialogoCerrarSesion());
    }

    private void mostrarDialogoCerrarSesion() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesion")
                .setMessage("¿Estas seguro de que quieres cerrar sesion?")
                .setPositiveButton("Si, cerrar sesion",
                        (dialog, which) -> LoginActivity.cerrarSesion(requireActivity()))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
