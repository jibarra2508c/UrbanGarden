package com.app.urbangarden.ui.qr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.app.urbangarden.data.KitRepository;
import com.app.urbangarden.databinding.FragmentQrBinding;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/**
 * Pantalla de vinculación de kits (por ID manual o escaneo QR). El repositorio
 * busca el ID en el catálogo de Firestore y, si existe, lo descarga a los kits
 * del usuario, donde aparece en Home y Kits.
 */
public class QrFragment extends Fragment {

    private FragmentQrBinding binding;

    /** Launcher del escáner QR. */
    private final ActivityResultLauncher<ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    String idEscaneado = result.getContents();
                    binding.etIdKit.setText(idEscaneado);
                    procesarIdKit(idEscaneado);
                } else {
                    Toast.makeText(getContext(),
                            "Escaneo cancelado", Toast.LENGTH_SHORT).show();
                }
            });

    //.........................................................................
    // Ciclo de vida

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentQrBinding.inflate(inflater, container, false);
        setupBotones();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //.........................................................................
    // Botones

    private void setupBotones() {
        // Cámara → lanza el escáner QR.
        binding.btnCamara.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Apunta al codigo QR del kit");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            qrLauncher.launch(options);
        });

        // Galería (no implementado en la demo).
        binding.btnGaleria.setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Galeria - proximamente", Toast.LENGTH_SHORT).show());

        // Vincular manual.
        binding.btnVincular.setOnClickListener(v -> {
            String idKit = binding.etIdKit.getText() != null
                    ? binding.etIdKit.getText().toString().trim() : "";

            if (idKit.isEmpty()) {
                binding.tilIdKit.setError("Introduce un ID de kit valido");
                return;
            }
            if (!idKit.startsWith("KIT-")) {
                binding.tilIdKit.setError("Formato incorrecto. Ej: KIT-2024-0123");
                return;
            }

            binding.tilIdKit.setError(null);
            procesarIdKit(idKit);
        });
    }

    //.........................................................................
    // Vinculación

    /**
     * Procesa el ID: si ya está vinculado, avisa; si no, lo busca en el catálogo
     * de Firestore y lo descarga si existe.
     */
    private void procesarIdKit(String idKit) {
        boolean yaExiste = KitRepository.getInstance().getKits().stream()
                .anyMatch(k -> idKit.equals(k.getId()));

        if (yaExiste) {
            Toast.makeText(getContext(),
                    "El kit " + idKit + " ya está vinculado",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        KitRepository.getInstance().vincularKit(idKit, encontrado -> {
            if (binding == null) return; // la pantalla ya no está visible
            if (encontrado) {
                Toast.makeText(getContext(),
                        "Kit " + idKit + " vinculado correctamente",
                        Toast.LENGTH_LONG).show();
                binding.etIdKit.setText("");
            } else {
                Toast.makeText(getContext(),
                        "No se ha encontrado ningún kit con el ID " + idKit,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
