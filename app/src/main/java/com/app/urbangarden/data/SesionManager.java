package com.app.urbangarden.data;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Gestiona la sesión y la cuenta del usuario con Firebase Authentication.
 *
 * Antes esto se guardaba en SharedPreferences (email y contraseña en texto
 * plano). Ahora las credenciales viven en Firebase: la contraseña nunca se
 * almacena en el dispositivo y la validación la hace el servidor.
 *
 *   - LA CUENTA: la crea Firebase con email + contraseña. El nombre se guarda
 *     como "display name" del usuario.
 *   - LA SESIÓN: Firebase recuerda al usuario logueado entre arranques; basta
 *     con preguntar si hay un usuario actual.
 *
 * Las operaciones de red son asíncronas, por eso registrar() e iniciarSesion()
 * devuelven el resultado por un callback en lugar de un boolean.
 */
public class SesionManager {

    /** Resultado de una operación de autenticación (registro o login). */
    public interface AuthCallback {
        void onExito();
        void onError(String mensaje);
    }

    private final FirebaseAuth auth;

    public SesionManager(Context context) {
        this.auth = FirebaseAuth.getInstance();
    }

    //.........................................................................
    // Registro de la cuenta

    /** Crea una cuenta nueva en Firebase y le asigna el nombre como display name. */
    public void registrar(String nombre, String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onError(traducirError(task.getException()));
                        return;
                    }
                    FirebaseUser usuario = auth.getCurrentUser();
                    if (usuario == null) {
                        callback.onExito();
                        return;
                    }
                    // Guardamos el nombre en el perfil del usuario de Firebase.
                    UserProfileChangeRequest perfil = new UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre)
                            .build();
                    usuario.updateProfile(perfil)
                            .addOnCompleteListener(t -> callback.onExito());
                });
    }

    //.........................................................................
    // Sesión

    /** Inicia sesión validando las credenciales contra Firebase. */
    public void iniciarSesion(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onExito();
                    } else {
                        callback.onError(traducirError(task.getException()));
                    }
                });
    }

    /** Cierra la sesión. La cuenta sigue existiendo en Firebase para volver a entrar. */
    public void cerrarSesion() {
        auth.signOut();
    }

    /** ¿Hay una sesión abierta? Se usa al arrancar para saltar el login. */
    public boolean haySesionAbierta() {
        return auth.getCurrentUser() != null;
    }

    //.........................................................................
    // Datos del usuario

    public String getNombre() {
        FirebaseUser usuario = auth.getCurrentUser();
        if (usuario != null && usuario.getDisplayName() != null
                && !usuario.getDisplayName().isEmpty()) {
            return usuario.getDisplayName();
        }
        return "Usuario";
    }

    public String getEmail() {
        FirebaseUser usuario = auth.getCurrentUser();
        return usuario != null && usuario.getEmail() != null ? usuario.getEmail() : "";
    }

    //.........................................................................
    // Utilidades

    /** Traduce las excepciones de Firebase a mensajes claros para el usuario. */
    private String traducirError(Exception e) {
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "La contraseña es demasiado débil (mínimo 6 caracteres).";
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Ya existe una cuenta con ese email.";
        }
        if (e instanceof FirebaseAuthInvalidUserException) {
            return "No existe ninguna cuenta con ese email.";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Email o contraseña incorrectos.";
        }
        return e != null && e.getMessage() != null
                ? e.getMessage()
                : "Se ha producido un error. Inténtalo de nuevo.";
    }
}
