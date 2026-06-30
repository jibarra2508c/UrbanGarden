# UrbanGarden 🌱

Aplicación **Android nativa (Java)** para la gestión de **kits de huerto urbano**: cada usuario vincula sus kits físicos (por QR o ID), sigue el crecimiento de sus plantas en tiempo real y dispone de un conjunto de herramientas de cuidado (riego, luz, poda, trasplante) y avisos automáticos.

Proyecto desarrollado como trabajo de fin del ciclo **DAM** (Desarrollo de Aplicaciones Multiplataforma). El código está documentado en español y estructurado por capas (UI / modelo / datos) para que sea fácil de leer, clonar y evaluar.

> **Idea de producto:** UrbanGarden vende kits de cultivo (maceta + sustrato + semillas) con un código único. El cliente lo escanea con la app, que "activa" el kit y le acompaña durante todo el ciclo de cultivo hasta la cosecha.

---

## 📑 Índice

- [Funcionalidades](#-funcionalidades)
- [Cómo crece el huerto: lógica de dominio](#-cómo-crece-el-huerto-lógica-de-dominio)
- [Pantallas en detalle](#-pantallas-en-detalle)
- [Arquitectura](#-arquitectura)
- [Tecnologías](#-tecnologías)
- [Puesta en marcha](#-puesta-en-marcha)
- [Modelo de datos en Firestore](#-modelo-de-datos-en-firestore)
- [Estructura del proyecto](#-estructura-del-proyecto)
- [Relación con las unidades del ciclo (DAM)](#-relación-con-las-unidades-del-ciclo-dam)
- [Autoría](#-autoría)

---

## ✨ Funcionalidades

| Área | Qué hace |
|------|----------|
| **Autenticación** | Registro e inicio de sesión con **Firebase Authentication** (email + contraseña), auto-login si ya hay sesión, validación de formularios y mensajes de error traducidos al español. |
| **Inicio (Home)** | Saludo personalizado, tarjeta **"Mi huerto"** (mini-esquema visual + resumen por categorías), tarjeta **"El tiempo hoy"** con recomendación de riego, y carrusel **"Mis kits"**. |
| **Gestión de kits** | Lista completa de kits con riego rápido, alta de kits por **QR** o **ID manual**, ficha de detalle, edición y borrado. Cada usuario solo ve **sus** kits. |
| **Crecimiento en vivo** | El progreso del cultivo y la humedad se calculan **en función del tiempo transcurrido**: las plantas "crecen" y se "secan" solas sin tocar la base de datos. |
| **Herramientas de cuidado** | Calculadora de riego, medidor de luz (sensor real del móvil), asesor premium, guía de poda y guía de trasplante. |
| **Avisos** | Notificaciones in-app generadas automáticamente según el estado de cada planta, con contador (badge) en la barra superior. |
| **Tiempo real** | Cualquier cambio en Firestore se refleja en todas las pantallas al instante gracias a `LiveData` + listeners. |

---

## 🌿 Cómo crece el huerto: lógica de dominio

El corazón de la app es la clase [`Kit`](app/src/main/java/com/app/urbangarden/model/Kit.java), que **no guarda solo valores** sino que los calcula al a partir de las fechas:

- **Edad real** (`getDiasReales`): días desde la activación + edad base de siembra. Crece sola cada día.
- **Progreso del ciclo** (`getProgresoReal`): de 0 a 100 % a lo largo de **110 días** (`DIAS_CICLO_COMPLETO`). Es lo que pinta la barra de progreso del detalle.
- **Humedad real** (`getHumedadReal`): parte del 100 % tras un riego y **decae linealmente hasta 0 % en 24 h** (`ParametrosRiego.INTERVALO_RIEGO_BASE_HORAS`).
- **¿Necesita riego?** (`necesitaRiego`): cierto cuando la humedad real baja del **50 %**.
- **Regar** (`regar`): pone la humedad al 100 % y reinicia el reloj del decaimiento.

---

## 📱 Pantallas en detalle

### Login / Registro — `LoginActivity`
Una sola pantalla con dos vistas alternables (login ↔ registro). Valida email y contraseña, delega en `SesionManager` (Firebase Auth) y, si ya existe sesión, salta directamente a la app. El cierre de sesión está centralizado en `LoginActivity.cerrarSesion()`, que además descarta el repositorio del usuario para que el siguiente login cargue sus propios kits.

### Inicio — `HomeFragment`
- **Cabecera** con el nombre del usuario (display name de Firebase).
- **Mi huerto:** dibuja un emoji por cada kit y un resumen del tipo *"2 aromáticas · 1 hortaliza"*.
- **El tiempo hoy:** consulta la API **Open-Meteo** (gratuita, sin clave) en un hilo de fondo y aconseja si regar. Ubicación **fija en Mallorca** para la demo (el GPS daba problemas en el emulador). Incluye **caché de 30 min** y **3 reintentos** ante fallos de red.
- **Mis kits:** carrusel horizontal que se actualiza solo al observar el `LiveData` del repositorio.

### Lista de kits — `KitsFragment`
Lista completa con un botón **"Regar ahora"** por tarjeta (persiste en Firestore) y un **FAB** para añadir kits nuevos (lleva a la pantalla de vinculación). Sin `notify` manual ni `onResume`: la lista se refresca al observar el `LiveData`.

### Vincular kit (QR) — `QrFragment`
Dos formas de añadir un kit:
1. **Escaneo QR** con la cámara (librería **ZXing** `zxing-android-embedded`).
2. **ID manual** con formato validado (`KIT-2024-0123`).

El ID se busca en la colección **`catalogo`** de Firestore; si existe, el kit "de fábrica" se descarga a los kits del usuario y se marca su fecha de activación. Evita duplicados y avisa si el kit no existe.

### Detalle del kit — `DetalleKitFragment`
Ficha completa de una planta (recibe solo el **ID** y observa el repositorio):
- Cabecera con emoji, nombre y especie.
- **Barra de progreso** del ciclo (en vivo) + días desde activación y estimación de cosecha.
- Agua diaria recomendada.
- Acceso a las **5 herramientas** de cuidado.
- **Editar** (nombre y especie, diálogo construido por código) y **Eliminar** (con confirmación).

### Calculadora de riego — `CalculadoraRiegoFragment`
Estima el agua diaria (ml) a partir de **ubicación** (interior/exterior), **horas de sol** (slider) y **tamaño del kit** (pequeño / mediano / grande → 0,5 / 1,5 / 3 L de sustrato). El cálculo vive en [`ParametrosRiego.mlAlDia`](app/src/main/java/com/app/urbangarden/model/ParametrosRiego.java), que aplica factores de ajuste por sol y por exterior. El botón **"Registrar riego"** riega el kit y guarda el agua/día calculada en Firestore.

### Medidor de luz — `MedidorLuzFragment`
Usa el **sensor de luz real del dispositivo** (`Sensor.TYPE_LIGHT`) para leer la iluminancia en **lux** en tiempo real. Muestra el nivel ("luz baja", "media", "sol directo"…), una barra de intensidad y un **veredicto** comparando con el rango óptimo (2 000–10 000 lux, en `ParametrosLuz`) con su consejo. Si el móvil no tiene sensor de luz, lo detecta y desactiva la medición. Detiene el sensor al salir de la pantalla para no gastar batería.

### Guía de poda — `PodaFragment`
Pantalla informativa estática: cuándo podar y los pasos, común a cualquier especie.

### Guía de trasplante — `TrasplanteFragment`
Compara la edad real del kit con el umbral de trasplante (**35 días**, `DIAS_TRASPLANTE`) para indicar si ya toca, permite **marcar el kit como trasplantado** (se persiste) y muestra una guía de pasos.

### Asesor premium — `AsesorFragment`
Pantalla de marketing del modelo **freemium**. No implementa pago real (requeriría Google Play Billing); es el gancho comercial de la suscripción.

### Notificaciones — `NotificationsFragment`
Avisos **in-app** (no push) generados **en vivo** por [`GeneradorNotificaciones`](app/src/main/java/com/app/urbangarden/data/GeneradorNotificaciones.java) a partir del estado de los kits. No se almacenan en BD: se recalculan al observar el `LiveData`. Reglas:

| Condición | Aviso |
|-----------|-------|
| Humedad real < 50 % | 💧 Toca regar |
| Progreso ≥ 95 % | 🌾 Listo para cosechar |
| Edad ≥ 35 días y sin trasplantar | 🪴 Trasplante recomendado |
| Edad ≤ 2 días | 🌱 Kit recién añadido |

El número total se muestra como **badge** sobre el icono de campana del toolbar (se oculta si es 0, muestra "9+" si pasa de 9). Al pulsar un aviso, navega al destino según su tipo (lista de kits, detalle o herramienta de trasplante).

### Perfil — `PerfilFragment`
Muestra los datos del usuario (nombre y email de Firebase) y las opciones de cuenta, incluido **cerrar sesión**.

---

## 🏗️ Arquitectura

Arquitectura por capas, con la UI desacoplada de los datos mediante **observación reactiva** (`LiveData`):

```
        ┌─────────────────────────────────────────────┐
        │                    UI                        │
        │  Activities (Login, Main) + Fragments (ui/)  │
        │  ViewBinding · Navigation Component          │
        └───────────────┬─────────────────────────────┘
                        │ observa LiveData
        ┌───────────────▼─────────────────────────────┐
        │                  DATOS (data/)               │
        │  KitRepository (singleton, Firestore)        │
        │  SesionManager (Firebase Auth)               │
        │  TiempoService (REST Open-Meteo, hilos)      │
        │  GeneradorNotificaciones (lógica en vivo)    │
        └───────────────┬─────────────────────────────┘
                        │
        ┌───────────────▼─────────────────────────────┐
        │                MODELO (model/)               │
        │  Kit · Notificacion · ParametrosRiego ·      │
        │  ParametrosLuz                               │
        └─────────────────────────────────────────────┘
```

**Decisiones de diseño destacadas:**
- **`KitRepository` es un singleton** con un *snapshot listener* de Firestore que publica un `LiveData<List<Kit>>`. Toda la UI observa esa misma fuente, así un cambio se propaga a todas las pantallas a la vez.
- Entre pantallas **solo se pasa el ID del kit**, nunca el objeto: cada pantalla relee el kit del repositorio y siempre tiene la versión actualizada.
- **Navegación:** un único grafo (`mobile_navigation.xml`) con `BottomNavigationView`. Tocar una pestaña siempre vuelve a la raíz de su sección (comportamiento centralizado en `MainActivity`).
- Los getters calculados de `Kit` van anotados con `@Exclude` para que Firestore **no** los serialice como campos.

---

## 🛠️ Tecnologías

- **Lenguaje:** Java 11
- **Plataforma:** Android — `minSdk 24` · `targetSdk 36` · `compileSdk 36`
- **UI:** Material 3, **ViewBinding**, **Navigation Component** + `BottomNavigationView`, `RecyclerView`
- **Backend:** **Firebase** (BoM 33.7.0)
  - **Cloud Firestore** — persistencia de kits y catálogo, en tiempo real.
  - **Firebase Authentication** — cuentas con email/contraseña.
- **API REST:** [Open-Meteo](https://open-meteo.com/) (gratuita, sin clave) vía `HttpURLConnection` + parseo JSON manual.
- **Sensores:** sensor de luz (`Sensor.TYPE_LIGHT`).
- **QR:** [ZXing](https://github.com/journeyapps/zxing-android-embedded) `zxing-android-embedded:4.3.0`.
- **Build:** Gradle (Kotlin DSL) con *version catalog* (`libs.versions.toml`).

---

## 🚀 Puesta en marcha

### Requisitos
- Android Studio (versión reciente) con un emulador o dispositivo **API 24+**.
- Un proyecto de **Firebase** propio (la app necesita Firestore y Authentication).

### Pasos

```bash
git clone https://github.com/jibarra2508c/UrbanGarden.git
# Abrir la carpeta en Android Studio
```

1. Crea un proyecto en la [consola de Firebase](https://console.firebase.google.com/).
2. Añade una app Android con el *package* `com.app.urbangarden`.
3. Activa **Authentication → Email/Password** y crea una base de datos **Cloud Firestore**.
4. Descarga tu `google-services.json` y colócalo en `app/`. *(El repo incluye uno de ejemplo; la API key que contiene no es secreta, viaja dentro del APK.)*
5. (Opcional pero recomendado) Crea la colección `catalogo` con algunos kits de prueba para poder vincularlos por QR/ID — ver [modelo de datos](#-modelo-de-datos-en-firestore).
6. Ejecuta en el emulador o dispositivo.

> **Permisos:** la app solo declara `INTERNET` (para el tiempo y Firebase) y usa la cámara a través del flujo de ZXing.

---

## 🗄️ Modelo de datos en Firestore

```
usuarios/{uid}/kits/{idKit}        ← los kits de cada usuario (privados por uid)
catalogo/{idKit}                   ← kits "de fábrica" que se vinculan al escanear
```

Cada documento de kit mapea automáticamente a la clase `Kit`. Campos persistidos (los calculados se excluyen):

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `nombre` | String | Nombre que el usuario da a la planta |
| `especie` | String | Especie (p. ej. *Albahaca*) |
| `tipo` | String | Categoría: aromática / hortaliza / verdura… |
| `emoji` | String | Emoji para el mini-esquema del huerto |
| `fechaActivacion` | Date | Momento en que se vinculó el kit |
| `fechaUltimoRiego` | Date | Referencia para el decaimiento de humedad |
| `humedad` | int | Humedad base (0–100) del último riego |
| `horasLuz`, `temperatura`, `diasEdad` | num | Parámetros base de la planta |
| `trasplantado` | boolean | Si el usuario ya lo marcó como trasplantado |
| `mlAlDia` | int | Agua diaria calculada (0 = sin calcular) |

Para que la vinculación funcione, basta con que `catalogo` tenga documentos cuyo **ID** sea el del kit (formato `KIT-AAAA-NNNN`).

---

## 📂 Estructura del proyecto

```
app/src/main/java/com/app/urbangarden/
├── LoginActivity.java          # Login/registro (Firebase Auth) + logout central
├── MainActivity.java           # Host de navegación, toolbar y badge de avisos
│
├── data/                       # Capa de datos
│   ├── KitRepository.java          # Singleton Firestore + LiveData (tiempo real)
│   ├── SesionManager.java          # Firebase Authentication
│   ├── TiempoService.java          # REST Open-Meteo en hilo de fondo + caché
│   └── GeneradorNotificaciones.java# Avisos derivados del estado de los kits
│
├── model/                      # Modelo de dominio
│   ├── Kit.java                    # Entidad + cálculos en vivo (progreso/humedad)
│   ├── Notificacion.java           # Aviso in-app (con su tipo)
│   ├── ParametrosRiego.java        # Cálculo de agua/día
│   └── ParametrosLuz.java          # Rango óptimo de luz y veredicto
│
└── ui/                         # Una carpeta por pantalla (Fragment + Adapter)
    ├── home/ · kits/ · detalleKit/
    ├── calculadoraRiego/ · medidorLuz/ · asesor/ · poda/ · trasplante/
    ├── qr/ · notifications/ · perfil/
```

---

## 🎓 Relación con las unidades del ciclo (DAM)

El proyecto está pensado para tocar varias unidades del módulo de Programación Multimedia / Desarrollo de Interfaces:

- **Persistencia y autenticación:** Cloud Firestore + Firebase Auth (`KitRepository`, `SesionManager`).
- **APIs REST:** consumo de Open-Meteo y parseo de JSON (`TiempoService`).
- **Concurrencia / hilos:** descarga de red en hilo de fondo y entrega reactiva con `LiveData` (`TiempoService`).
- **UI avanzada:** Material 3, Navigation Component, `RecyclerView`, ViewBinding, diálogos, animaciones.
- **Hardware del dispositivo:** sensor de luz y cámara (QR).

---

## ✍️ Autoría

**Javier Ibarra** — Proyecto del ciclo **DAM**.

> Versión de demostración: el asesor premium y la galería del lector QR son ganchos de producto sin implementación de pago/selección real.
