# MercadoLibre App

Aplicación movil Android desarrollada para el analisis de información comercial basada en una base de datos de MercadoLibre.  
El sistema esta orientado para un ejecutivo, con el objetivo de apoyar la toma de decisiones mediante KPIs, gráficas interactivas, análisis automático, detección de riesgos y generación de reportes PDF.

---

## Objetivo del proyecto

El objetivo de la aplicación es presentar indicadores clave de desempeño relacionados con inventario, ventas, operación, comportamiento de usuarios y calidad del servicio, utilizando una base de datos local en SQLite.

La aplicación permite consultar información sin necesidad de conexión a internet, ya que la base de datos se encuentra integrada dentro del dispositivo móvil.

---

## Tecnologías utilizadas

- Android Studio
- Kotlin
- Jetpack Compose
- SQLite local
- Canvas para gráficas personalizadas
- PdfDocument para generación de reportes PDF

---

## Base de datos

La aplicación trabaja con una base de datos local llamada `kpis.db`, ubicada dentro del proyecto en:

```txt
app/src/main/assets/databases/kpis.db
