package com.app.urbangarden.ui.asesor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.app.urbangarden.databinding.FragmentAsesorBinding;

/**
 * Asesor de plantas (función Premium). Pantalla de marketing de la suscripción:
 * no implementa pago real (requeriría Google Play Billing), solo el gancho
 * comercial del modelo freemium.
 */
public class AsesorFragment extends Fragment {

    private FragmentAsesorBinding binding;

    //.........................................................................
    // Ciclo de vida

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAsesorBinding.inflate(inflater, container, false);
        configurarBotones();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //.........................................................................
    // Botones

    private void configurarBotones() {
        binding.btnSuscribirse.setOnClickListener(v ->
                // TODO: integrar Google Play Billing para la compra real
                Toast.makeText(getContext(),
                        "Suscripción no disponible en la versión de demostración",
                        Toast.LENGTH_LONG).show());

        binding.tvQuizaDespues.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }
}
