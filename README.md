# UrbanGarden 🌱

Aplicación Android para la gestión de **kits de huerto urbano**: seguimiento de tus plantas, consejos de cuidado y herramientas de ayuda (riego, luz, poda, trasplante). Proyecto desarrollado como trabajo de fin del ciclo **DAM** (Desarrollo de Aplicaciones Multiplataforma).

## Características

- **Inicio** con una *tarjeta del tiempo* que aconseja si regar hoy, consultando la API meteorológica gratuita [Open-Meteo](https://open-meteo.com/) (sin clave). Debajo, la lista de "Mis kits".
- **Calculadora de riego**: estima la frecuencia/cantidad a partir de ubicación interior/exterior, horas de sol, tamaño del kit y especie.
- **Medidor de luz**, **asesor**, y guías de **poda** y **trasplante**.
- **Lector QR** para identificar kits.
- **Notificaciones** generadas según el estado de las plantas.
- **Login** y **perfil** de usuario.

## Tecnologías

- **Java** sobre Android (minSdk 24 · targetSdk 36)
- **Material 3**, **ViewBinding**
- **Navigation Component** con `BottomNavigationView`
- Llamadas REST en hilos en segundo plano (`TiempoService`) y parseo JSON
- Arquitectura por *fragments* (`ui/...`), modelos (`model/`) y datos (`data/`)

## Ejecución

El proyecto funciona en **modo local** (datos de demostración en memoria vía `DatosDemo`), por lo que se puede ejecutar directamente en Android Studio sin configurar backend. Solo requiere permiso de `INTERNET` para la tarjeta del tiempo.

```
git clone <url>
# Abrir en Android Studio y ejecutar en emulador o dispositivo (API 24+)
```

> Existe una versión preparada para conectar **Firebase Firestore** (backup en el repositorio); para activarla hay que restaurar el repositorio de datos correspondiente y añadir las dependencias y `google-services.json`.

## Estructura

```
app/src/main/java/com/app/urbangarden/
├── LoginActivity.java · MainActivity.java
├── data/        # KitRepository, TiempoService, SesionManager, notificaciones
├── model/       # Kit, Notificacion, ParametrosRiego, ParametrosLuz
└── ui/          # home, kits, calculadoraRiego, medidorLuz, asesor,
                 # poda, trasplante, qr, notifications, perfil, detalleKit
```

---
Autor: **Javier Ibarra**
