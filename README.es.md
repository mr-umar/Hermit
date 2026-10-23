# Hermit

> **Una aplicación Android de código abierto e independiente para sesiones de concentración consciente mediante etiquetas NFC.**
> 
> *Read in English: [README.md](README.md)*

Hermit convierte etiquetas NFC estándar y económicas en un ancla física frente a las distracciones digitales. Al vincular el gesto tangible de acercar el teléfono con el bloqueo genuino de aplicaciones a través del `AccessibilityService` de Android, Hermit te ayuda a cultivar un estado de concentración profundo y sereno, sin hardware propietario, sin suscripciones y sin rastreadores en la nube.

---

## Tabla de Contenidos

1. [Filosofía y Funcionamiento](#filosofía-y-funcionamiento)
2. [Funcionalidades Principales](#funcionalidades-principales)
3. [Arquitectura y Tecnologías](#arquitectura-y-tecnologías)
4. [Compatibilidad de Etiquetas NFC](#compatibilidad-de-etiquetas-nfc)
5. [Especificación del Payload NDEF](#especificación-del-payload-ndef)
6. [Implementación del Bloqueo de Aplicaciones](#implementación-del-bloqueo-de-aplicaciones)
7. [Limitaciones de Fabricantes de Android](#limitaciones-de-fabricantes-de-android)
8. [Permisos y Privacidad](#permisos-y-privacidad)
9. [Guía de Compilación e Instalación](#guía-de-compilación-e-instalación)
10. [Pruebas Automatizadas](#pruebas-automatizadas)
11. [Sitio Web y Despliegue](#sitio-web-y-despliegue)
12. [Licencia](#licencia)

---

## Filosofía y Funcionamiento

Los teléfonos inteligentes modernos están optimizados para captar la atención de forma constante. La fuerza de voluntad frente a la pantalla suele ser insuficiente para frenar la memoria muscular automática.

Hermit introduce una **fricción física intencionada**:
1. Creas un perfil de enfoque (por ejemplo, Trabajo Profundo, Estudio o Descanso) seleccionando las aplicaciones que deseas bloquear.
2. Grabas la acción deseada en una etiqueta NFC física estándar (NTAG213, NTAG215 o NTAG216).
3. Acercar el teléfono a la etiqueta inicia la sesión de concentración y levanta el escudo protector.
4. Cualquier intento de abrir una app bloqueada muestra inmediatamente una pantalla de pausa reflexiva que te invita a respirar, exigiendo volver a tocar la etiqueta NFC o cumplir una cuenta atrás para salir.
5. Todos los datos, perfiles y estadísticas permanecen de forma estrictamente local en tu dispositivo.

---

## Funcionalidades Principales

- **Gestor Completo de Etiquetas NFC:**
  - Creación, grabación y registro de etiquetas dentro de la app.
  - Nombres personalizados y asignación de acciones: `TOGGLE` (alternar), `ACTIVATE` (activar), `DEACTIVATE` (desactivar), `SWITCH` (cambiar perfil).
  - Flujo de grabación guiado con verificación inmediata de lectura posterior.
  - Alerta de seguridad antes de sobrescribir etiquetas con datos NDEF preexistentes.
  - Inspector de etiquetas que decodifica UID, chip, estado de solo lectura y memoria NDEF.
- **Bloqueo Real de Aplicaciones:**
  - `HermitAccessibilityService` supervisa cambios de ventana en primer plano en tiempo real.
  - Lista blanca de seguridad que preserva el teléfono de llamadas, emergencias, instalador de paquetes, launcher de inicio y ajustes del sistema.
  - Ventana de amortiguación (debounce) de 1200 ms para evitar bucles de eventos.
  - Persistencia de sesión tras reinicio del dispositivo (`BootReceiver`).
- **Perfiles de Concentración:**
  - Perfiles configurables con nombres, descripciones, iconos y colores.
  - Selector de aplicaciones instaladas con búsqueda en tiempo real.
  - Duración de sesión opcional con temporizador visible o modo indefinido.
  - Modo estricto con pausa reflexiva de 60 segundos antes de permitir salidas de emergencia.
- **Métricas e Historial Local:**
  - Registro de horas de enfoque, sesiones completadas y distracciones evitadas en base de datos Room.
  - Cero peticiones de red o telemetría externa.
- **Interfaz Bilingüe:**
  - Soporte completo en español (`values-es/`) e inglés (`values/`).
  - Detección automática del idioma del sistema con opción de cambio manual en Ajustes.

---

## Arquitectura y Tecnologías

Hermit se ha desarrollado de forma nativa en Android siguiendo las directrices de diseño de Apple HIG y Material 3:

- **Lenguaje:** Kotlin 2.0+
- **Interfaz de Usuario:** Jetpack Compose con componentes Material 3.
- **Principios de Diseño:**
  - Respuesta física táctil inmediata con muelles críticamente amortiguados (`damping: 1.0`, `scale(0.96)`).
  - Superficies translúcidas con desenfoque de fondo en barras de navegación.
  - Jerarquía tipográfica con espaciado negativo en títulos de gran tamaño.
- **Persistencia de Datos:**
  - **Room Database:** Almacenamiento estructurado de perfiles, etiquetas y sesiones.
  - **Preferences DataStore:** Estado atómico de la sesión activa, tema e idioma.
- **Concurrencia:** Corrutinas de Kotlin y flujos reactivos (`Flow`, `StateFlow`).
- **NFC:** Android `NfcAdapter`, `Ndef`, `NdefFormatable` y `enableReaderMode`.
- **Supervisión de Sistema:** Android `AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`).

---

## Compatibilidad de Etiquetas NFC

Hermit prioriza etiquetas estándar no propietarias compatibles con NFC Forum Tipo 2 e ISO 14443-3A:

| Familia de Chip | Memoria Útil | Estado de Compatibilidad | Notas |
| :--- | :--- | :--- | :--- |
| **NTAG213** | 144 bytes | **100% Compatible** | El payload compacto (&lt;120B) cabe holgadamente. Muy económico. |
| **NTAG215** | 504 bytes | **100% Compatible** | El estándar más común y accesible en el mercado. |
| **NTAG216** | 888 bytes | **100% Compatible** | Máxima capacidad para etiquetas Tipo 2. |
| **Tipo 2 Genérico / Formateable** | Variable | **Compatible** | Formateo NDEF automático si el hardware de la etiqueta lo admite. |
| **ISO 14443-4 / Tipo 4** | Variable | **Condicional** | Compatible si el controlador NFC del teléfono lo soporta como NDEF estándar. |

---

## Especificación del Payload NDEF

Las etiquetas grabadas por Hermit contienen un registro MIME específico de la aplicación seguido de un Android Application Record (AAR):

### Registro 1: Registro MIME Personalizado
- **Tipo MIME:** `application/vnd.xyz.umarmohammad.hermit`
- **Formato:** JSON codificado en UTF-8 con control de versiones:
  ```json
  {
    "v": 1,
    "id": "e4b3c2a1-6789-4def-0123-456789abcdef",
    "act": "TOGGLE",
    "pid": "default_deep_work",
    "name": "Etiqueta Escritorio"
  }
  ```
  - `v`: Versión del formato de payload (actualmente `1`).
  - `id`: UUID generado criptográficamente que identifica el registro de la etiqueta.
  - `act`: Acción de enfoque (`TOGGLE`, `ACTIVATE`, `DEACTIVATE`, `SWITCH`).
  - `pid`: Identificador del perfil asociado (opcional).
  - `name`: Nombre descriptivo de la etiqueta (opcional).

### Registro 2: Android Application Record (AAR)
- **Valor AAR:** `xyz.umarmohammad.hermit`
- Le indica al sistema operativo Android que abra o instale Hermit automáticamente al escanear la etiqueta.

---

## Implementación del Bloqueo de Aplicaciones

El bloqueo se ejecuta a través de `HermitAccessibilityService`:

1. **Filtrado de Eventos:** Escucha únicamente eventos `TYPE_WINDOW_STATE_CHANGED`. La lectura de contenidos de pantalla está deshabilitada (`canRetrieveWindowContent="false"`).
2. **Inspección de Primer Plano:** Obtiene el nombre del paquete de la aplicación que pasa a primer plano y lo compara con la lista de bloqueo del perfil activo.
3. **Lista Blanca de Seguridad:** El lanzador del sistema, la aplicación de teléfono, servicios de emergencia y los ajustes están completamente exentos.
4. **Protección de Bucle:** Un intervalo de 1200 ms evita disparos repetidos al alternar entre pantallas.
5. **Pantalla de Pausa:** Abre `BlockOverlayActivity` mostrando el mensaje consciente, el tiempo restante y la instrucción de desbloquear con la etiqueta NFC.

---

## Limitaciones de Fabricantes de Android

- **Optimización de Batería:** Fabricantes con políticas agresivas de gestión energética (Xiaomi MIUI/HyperOS, Huawei, Samsung OneUI) pueden suspender servicios de accesibilidad en segundo plano. Hermit incluye un acceso directo en **Ajustes &gt; Optimización de Batería** para solicitar la ejecución sin restricciones.
- **Ubicación de la Antena NFC:** La antena varía según el dispositivo (parte central en Google Pixel, esquina superior junto a la cámara en Samsung). Mantén la etiqueta firme durante la grabación.
- **Ajustes Restringidos en Android 13+:** En aplicaciones instaladas manualmente fuera de tiendas oficiales, Android exige autorizar "Ajustes restringidos" en la información de la app antes de habilitar el Servicio de Accesibilidad.

---

## Permisos y Privacidad

| Permiso | Propósito | Impacto en Privacidad |
| :--- | :--- | :--- |
| `android.permission.NFC` | Lectura y grabación de etiquetas físicas | Exclusivamente hardware local |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | Detectar cuándo se abren apps bloqueadas | Sin lectura de textos ni pulsaciones; solo nombres de paquetes |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Restaurar sesiones de enfoque tras reiniciar | Estado de sesión local |
| `android.permission.VIBRATE` | Respuesta háptica al escanear | Sensorial |
| `android.permission.QUERY_ALL_PACKAGES` | Listar aplicaciones en el selector de bloqueo | Almacenado localmente en Room |

**Cero Acceso a Internet:** Hermit no incluye el permiso `INTERNET` en su manifiesto. Es técnicamente incapaz de enviar datos fuera del dispositivo.

---

## Guía de Compilación e Instalación

### Requisitos
- JDK 17 o superior (verificado con OpenJDK 25).
- Android SDK con `compileSdk 35` y `build-tools 34.0.0+`.

### Compilar APK de Depuración
```bash
git clone https://github.com/mr-umar/Hermit.git
cd Hermit

# Configurar ruta del SDK en local.properties
echo "sdk.dir=C\:/Users/Umar/AppData/Local/Android/Sdk" > local.properties

# Compilar APK de depuración
./gradlew assembleDebug
```
El archivo APK se genera en:
`app/build/outputs/apk/debug/app-debug.apk`

### Instalar vía ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Pruebas Automatizadas

Ejecutar la suite completa de pruebas unitarias:
```bash
./gradlew testDebugUnitTest
```

---

## Sitio Web y Despliegue

El repositorio incluye un sitio web estático en el directorio [`website/`](website/):

- **Dominio de Producción:** [https://hermit.umarmohammad.xyz](https://hermit.umarmohammad.xyz)
- **Documentación de Despliegue:**
  - [Información del Sitio](website/README.md)
  - [Configuración DNS (CNAME)](website/DNS.md)
  - [Guía de Despliegue en Nginx](website/DEPLOY.md)

---

## Licencia

Este proyecto está licenciado bajo la licencia **GNU Affero General Public License v3.0 (AGPL-3.0-or-later)**. Consulta el archivo [LICENSE](LICENSE) para más detalles.
