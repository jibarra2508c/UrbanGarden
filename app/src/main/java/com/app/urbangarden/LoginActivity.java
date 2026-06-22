package com.app.urbangarden;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.urbangarden.data.KitRepository;
import com.app.urbangarden.data.SesionManager;
import com.app.urbangarden.databinding.ActivityLoginBinding;

/**
 * Login y registro con Firebase Authentication (UD6).
 *
 *   - Si ya hay sesión abierta, salta directo a MainActivity.
 *   - Al registrarse, crea la cuenta en Firebase y abre sesión.
 *   - Al hacer login, Firebase valida las credenciales contra el servidor.
 *   - La cuenta NO se borra al cerrar sesión, así que se puede volver a entrar.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SesionManager sesion;

    //.........................................................................
    // Ciclo de vida

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sesion = new SesionManager(this);

        // Auto-login: si ya hay sesión abierta, ni mostramos el login.
        if (sesion.haySesionAbierta()) {
            navegarAMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBotones();
    }

    //.........................................................................
    // Botones (login, registro y alternar vistas)

    private void setupBotones() {
        // Login
        binding.btnLogin.setOnClickListener(v -> {
            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                binding.tilEmail.setError("Introduce tu email");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.setError("El email no tiene un formato válido");
                return;
            }
            if (password.isEmpty()) {
                binding.tilPassword.setError("Introduce tu contraseña");
                return;
            }

            binding.tilEmail.setError(null);
            binding.tilPassword.setError(null);

            binding.btnLogin.setEnabled(false);
            sesion.iniciarSesion(email, password, new SesionManager.AuthCallback() {
                @Override
                public void onExito() {
                    navegarAMain();
                }

                @Override
                public void onError(String mensaje) {
                    binding.btnLogin.setEnabled(true);
                    binding.tilPassword.setError(mensaje);
                }
            });
        });

        // Ir a Registro
        binding.btnIrRegistro.setOnClickListener(v -> {
            binding.layoutLogin.setVisibility(View.GONE);
            binding.layoutRegistro.setVisibility(View.VISIBLE);
        });

        // Volver al Login
        binding.btnIrLogin.setOnClickListener(v -> {
            binding.layoutRegistro.setVisibility(View.GONE);
            binding.layoutLogin.setVisibility(View.VISIBLE);
        });

        // Crear cuenta
        binding.btnRegistro.setOnClickListener(v -> {
            String nombre    = binding.etNombre.getText().toString().trim();
            String email     = binding.etEmailReg.getText().toString().trim();
            String password  = binding.etPasswordReg.getText().toString().trim();
            String password2 = binding.etPasswordReg2.getText().toString().trim();

            if (nombre.isEmpty()) {
                binding.tilNombre.setError("Introduce tu nombre");
                return;
            }
            if (email.isEmpty()) {
                binding.tilEmailReg.setError("Introduce tu email");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmailReg.setError("El email no tiene un formato válido");
                return;
            }
            if (password.isEmpty()) {
                binding.tilPasswordReg.setError("Introduce una contraseña");
                return;
            }
            if (!password.equals(password2)) {
                binding.tilPasswordReg2.setError("Las contraseñas no coinciden");
                return;
            }

            binding.tilNombre.setError(null);
            binding.tilEmailReg.setError(null);
            binding.tilPasswordReg.setError(null);
            binding.tilPasswordReg2.setError(null);

            binding.btnRegistro.setEnabled(false);
            sesion.registrar(nombre, email, password, new SesionManager.AuthCallback() {
                @Override
                public void onExito() {
                    Toast.makeText(LoginActivity.this,
                            "Cuenta creada, ¡bienvenido " + nombre + "!",
                            Toast.LENGTH_SHORT).show();
                    navegarAMain();
                }

                @Override
                public void onError(String mensaje) {
                    binding.btnRegistro.setEnabled(true);
                    binding.tilEmailReg.setError(mensaje);
                }
            });
        });
    }

    //.........................................................................
    // Navegación

    private void navegarAMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Único punto de logout de la app: borra la sesión de Firebase Auth,
     * descarta el repositorio del usuario actual (para que el próximo login cree
     * uno nuevo con sus kits) y vuelve al login vaciando la pila. Llamar desde
     * cualquier pantalla pasando su Activity.
     */
    public static void cerrarSesion(Activity origen) {
        new SesionManager(origen).cerrarSesion();
        KitRepository.cerrar();
        // Volver a LoginActivity limpiando toda la pila.
        Intent intent = new Intent(origen, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        origen.startActivity(intent);
        origen.finish();
    }
}
