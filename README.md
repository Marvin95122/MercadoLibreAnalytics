<div align="center">

# Proyecto Final  

**Tecnológico Nacional de México**  
**Instituto Tecnológico de Oaxaca**

---

### Materia
**Desarrollo de Software para la Toma de Decisiones**

### Clave de materia
**DSED2302**

### Carrera
**Ingeniería en Sistemas Computacionales**

### Trabajo
**Proyecto Final**

### Catedrática
**Martínez Nieto Adelina**

### Equipo
**Equipo 2**

### Grupo
**8SB**

### Integrantes
Cruz Sánchez Jhoan Marvin  
Espinoza de la Rosa Uriel  
José Sebastián Jafet  
Juárez Monjaraz Griselda Itzel  
Ortega Patiño Nimsi Joana  

### Lugar y fecha
**Oaxaca de Juárez, Oaxaca — 04 de junio de 2026**

</div>

---

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
- Documento para generación de reportes PDF
- CloverDX
- Power BI

---

## Base de datos

La aplicación trabaja con una base de datos local llamada `kpis.db`, ubicada dentro del proyecto en:

app/src/main/assets/databases/kpis.db


# Proceso ETL

El proceso ETL fue una parte fundamental del proyecto, ya que permitió preparar los datos antes de utilizarlos dentro de la aplicación.

ETL significa:

- **Extract:** extracción de datos.
- **Transform:** transformación, limpieza y validación de datos.
- **Load:** carga de datos hacia una base de datos.

En este proyecto, el proceso ETL permitió integrar registros de productos en una base de datos relacional de MercadoLibre. La información original se encontraba en archivos CSV, por lo que fue necesario limpiarla, transformarla y cargarla correctamente en MySQL.

---

## 1. Extracción de datos

La fase de extracción consistió en obtener los datos desde un archivo CSV que contenía información de productos.

Para esta etapa se utilizó CloverDX. Primero se creó un proyecto llamado:

```text
ETL_Mercadolibre2
```

Dentro del proyecto se agregó el archivo CSV en la carpeta:

```text
data-in
```

Posteriormente, se creó un grafo ETL en CloverDX y se agregó el componente:

```text
UniversalDataReader
```

Este componente permitió leer el archivo CSV y enviar los datos hacia la siguiente etapa del proceso.

Durante esta fase también se configuraron los metadatos del archivo, indicando los nombres de las columnas y los tipos de datos correspondientes. Esto fue necesario para que CloverDX interpretara correctamente cada campo del archivo.

También se configuró el lector para omitir la primera fila del CSV, ya que esta contenía los encabezados de las columnas.

---

## 2. Transformación de datos

La fase de transformación consistió en limpiar, corregir y adaptar los datos para que coincidieran con la estructura de la base de datos destino.

Para esta etapa se utilizó el componente:

```text
Map
```

Este componente permitió relacionar los campos del archivo CSV con las columnas de la tabla `productos` en MySQL.

Las principales transformaciones realizadas fueron:

- Mapeo de columnas del CSV hacia la tabla destino.
- Conversión del campo `fecha_publicacion` a un formato compatible con MySQL.
- Validación de campos numéricos como `precio`, `stock`, `id_marca`, `id_categoria` e `id_tienda`.
- Reemplazo de descripciones vacías por el texto `SIN DESCRIPCIÓN`.
- Revisión de que los nombres, descripciones, precios y existencias no quedaran desplazados.
- Ajuste de metadatos para evitar errores durante la carga.
- Validación de que cada dato quedara ubicado en la columna correcta.

Esta fase fue importante porque el archivo CSV no tenía exactamente la misma estructura que la tabla destino. Además, algunos registros contenían comas dentro de las descripciones, por lo que fue necesario configurar correctamente la lectura del archivo para evitar que esas comas fueran interpretadas como separadores de columnas.

---

## 3. Carga de datos

La fase de carga consistió en insertar los datos transformados dentro de la base de datos MySQL.

Para esta etapa se utilizó el componente:

```text
DatabaseWriter
```

Este componente permitió conectar CloverDX con MySQL y cargar los registros en la tabla:

```text
productos
```

Primero se creó una conexión hacia la base de datos MySQL, indicando el servidor, usuario, contraseña y base de datos correspondiente.

Después se configuró el mapeo entre los campos de CloverDX y los campos de la tabla `productos`.

Finalmente, se ejecutó el grafo ETL y se validó que la carga se realizara correctamente.

Durante la carga se registraron 9999 productos en la base de datos.

---

# Validaciones realizadas después del ETL

Después de cargar los datos en MySQL, se realizaron consultas SQL para comprobar que la información estuviera correctamente almacenada.

Las validaciones principales fueron:

- Verificar que los productos se cargaran en el rango esperado.
- Revisar que `id_producto` estuviera en orden.
- Comprobar que `sku_referencia` estuviera en su columna correspondiente.
- Validar que `nombre_producto` no estuviera desplazado.
- Validar que `descripcion` no estuviera movida a otra columna.
- Comprobar que `precio` y `stock` fueran valores numéricos.
- Verificar que `fecha_publicacion` tuviera un formato correcto.
- Comprobar que `id_marca`, `id_categoria` e `id_tienda` fueran valores numéricos.
- Validar que las descripciones vacías fueran reemplazadas por `SIN DESCRIPCIÓN`.

Ejemplo de consulta para revisar una muestra de registros cargados:

```sql
SELECT 
    id_producto,
    sku_referencia,
    nombre_producto,
    descripcion,
    precio,
    stock,
    fecha_publicacion,
    estado_producto,
    condicion_producto,
    modelo,
    id_marca,
    id_categoria,
    id_tienda
FROM productos
WHERE id_producto BETWEEN 40001 AND 50000
ORDER BY id_producto
LIMIT 10;
```

---

## Validación de integridad referencial

También se realizaron validaciones para comprobar que los productos estuvieran relacionados correctamente con sus marcas, categorías y tiendas.

### Validación de marcas

```sql
SELECT COUNT(*) AS marcas_no_encontradas
FROM productos p
LEFT JOIN marcas m ON p.id_marca = m.id_marca
WHERE m.id_marca IS NULL
AND p.id_producto BETWEEN 40001 AND 50000;
```

### Validación de categorías

```sql
SELECT COUNT(*) AS categorias_no_encontradas
FROM productos p
LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
WHERE c.id_categoria IS NULL
AND p.id_producto BETWEEN 40001 AND 50000;
```

### Validación de tiendas

```sql
SELECT COUNT(*) AS tiendas_no_encontradas
FROM productos p
LEFT JOIN tiendas t ON p.id_tienda = t.id_tienda
WHERE t.id_tienda IS NULL
AND p.id_producto BETWEEN 40001 AND 50000;
```

Estas validaciones ayudaron a comprobar que los productos no quedaran asociados a identificadores inexistentes.

---

# Retos encontrados durante el proceso ETL

Durante el proceso ETL se presentaron varios retos importantes.

## Formato de fecha

Uno de los principales problemas fue el formato del campo `fecha_publicacion`, ya que el archivo CSV manejaba fechas en formato:

```text
DD/MM/YYYY HH:MM:SS
```

MySQL necesitaba un formato compatible para almacenar las fechas correctamente. Por ello, fue necesario ajustar los metadatos y aplicar la conversión correspondiente dentro de CloverDX.

## Descripciones con comas

Algunas descripciones de productos contenían comas dentro del texto. Esto podía provocar que CloverDX interpretara esas comas como separadores de columnas, desplazando los datos hacia campos incorrectos.

Para corregirlo, se configuró el lector del archivo para reconocer cadenas de texto entre comillas, evitando que las descripciones afectaran la estructura del registro.

## Registros duplicados

También apareció un error relacionado con la llave primaria, ya que algunos productos del rango utilizado ya existían previamente en la tabla `productos`.

MySQL no permitió insertar identificadores duplicados, lo cual ayudó a detectar el problema y mantener la integridad de la información.

Después de revisar los registros existentes y limpiar la tabla en el rango correspondiente, la carga pudo realizarse correctamente.

---

# Importancia de la calidad del dato

La calidad del dato fue fundamental para obtener resultados correctos en la aplicación.

Si los datos se hubieran cargado sin limpieza ni validación, las consultas SQL, los KPIs, las gráficas y los reportes PDF podrían haber mostrado información incorrecta o poco útil.

Por esta razón, antes de utilizar los datos dentro de la aplicación fue necesario:

- Limpiar campos vacíos.
- Corregir formatos de fecha.
- Validar columnas.
- Revisar tipos de datos.
- Evitar registros duplicados.
- Comprobar relaciones con otras tablas.
- Mantener consistencia en la información.

Gracias a este proceso, la aplicación pudo trabajar con datos más confiables, completos y organizados.

---

# Análisis de datos

Después del proceso ETL, la información cargada fue utilizada para realizar consultas SQL y generar indicadores.

Una consulta representativa del proyecto permitió responder la siguiente pregunta de negocio:

**¿Qué categorías y marcas representan el mayor valor de inventario disponible?**

Para responder esta pregunta, se utilizó una consulta con `INNER JOIN` entre las tablas `productos`, `categorias` y `marcas`.

También se aplicó `GROUP BY` para agrupar los productos por categoría y marca, y se calcularon métricas como:

- Total de productos.
- Unidades disponibles.
- Precio promedio.
- Valor total de inventario.

Consulta utilizada:

```sql
SELECT 
    c.nombre_categoria,
    m.nombre_marca,
    COUNT(p.id_producto) AS total_productos,
    SUM(p.stock) AS unidades_disponibles,
    ROUND(AVG(p.precio), 2) AS precio_promedio,
    ROUND(SUM(p.precio * p.stock), 2) AS valor_total_inventario
FROM productos p
INNER JOIN categorias c ON p.id_categoria = c.id_categoria
INNER JOIN marcas m ON p.id_marca = m.id_marca
WHERE p.id_producto BETWEEN 40001 AND 50000
GROUP BY c.nombre_categoria, m.nombre_marca
ORDER BY valor_total_inventario DESC
LIMIT 10;
```

Esta consulta permite identificar las combinaciones de categoría y marca que concentran mayor valor económico dentro del inventario.

La información obtenida puede apoyar decisiones relacionadas con:

- Compras.
- Surtido.
- Promociones.
- Control de existencias.
- Identificación de productos con mayor valor comercial.

---

# Funcionamiento de la aplicación

La aplicación funciona como una herramienta de consulta y análisis de información comercial.

El usuario puede ingresar a la aplicación, seleccionar un módulo o KPI, visualizar los resultados mediante gráficas y generar reportes en PDF.

El funcionamiento general es el siguiente:

1. El usuario ingresa a la aplicación.
2. La aplicación muestra un menú principal con los módulos disponibles.
3. El usuario selecciona el KPI o módulo que desea consultar.
4. La aplicación realiza consultas sobre la base de datos local.
5. Los resultados se muestran mediante tarjetas, gráficas o listados.
6. El usuario puede interpretar los resultados mediante una lectura rápida.
7. En algunos módulos se pueden aplicar filtros por periodo o categoría.
8. La aplicación permite generar reportes PDF con los resultados obtenidos.

La información presentada en la aplicación proviene de datos previamente procesados mediante el proceso ETL, lo que permite trabajar con información más limpia, ordenada y confiable.

---

# Módulos de la aplicación

## 1. Módulo de inicio

Este módulo funciona como pantalla principal de la aplicación.

Desde aquí el usuario puede acceder a los diferentes apartados de análisis. Su objetivo es facilitar la navegación y presentar de forma ordenada las opciones disponibles.

---

## 2. Módulo de KPIs

Este módulo concentra los indicadores clave de rendimiento de la aplicación.

Permite consultar métricas relacionadas con:

- Inventario.
- Ventas.
- Pedidos.
- Productos.
- Pagos.
- Envíos.
- Devoluciones.
- Búsquedas.
- Reseñas.

Su finalidad es mostrar de forma resumida el comportamiento general de la información comercial.

---

## 3. Módulo de inventario

Este módulo permite analizar el valor económico del inventario disponible.

Incluye información relacionada con:

- Valor total del inventario.
- Stock disponible.
- Precio promedio.
- Productos con mayor valor.
- Valor por categoría.
- Valor por marca.

Este módulo ayuda a identificar qué productos, marcas o categorías representan mayor inversión dentro del inventario.

---

## 4. Módulo de ventas

Este módulo permite analizar las ventas registradas en la base de datos.

Puede mostrar información como:

- Ventas totales.
- Ventas por mes.
- Meses con mayor venta.
- Meses con menor venta.
- Comportamiento de ventas por periodo.

Este módulo ayuda a identificar tendencias comerciales y periodos de aumento o disminución en las ventas.

Consulta base utilizada:

```sql
SELECT IFNULL(SUM(total_pagar), 0) 
FROM pedidos;
```

---

## 5. Módulo de productos

Este módulo permite consultar información relacionada con los productos almacenados.

Puede incluir datos como:

- Nombre del producto.
- SKU o referencia.
- Descripción.
- Precio.
- Stock.
- Estado del producto.
- Condición del producto.
- Marca.
- Categoría.
- Tienda.

Este módulo es importante porque los productos son la base de varios indicadores de la aplicación.

Consulta base:

```sql
SELECT COUNT(*) 
FROM productos;
```

---

## 6. Módulo de pedidos

Este módulo permite analizar los pedidos registrados.

Puede mostrar información relacionada con:

- Total de pedidos.
- Pedidos por estado.
- Pedidos completados.
- Pedidos pendientes.
- Pedidos cancelados.

Este módulo ayuda a conocer el comportamiento operativo de los pedidos.

Consulta base:

```sql
SELECT COUNT(*) 
FROM pedidos;
```

---

## 7. Módulo de pagos

Este módulo permite analizar los métodos de pago utilizados por los clientes.

Puede mostrar información como:

- Cantidad total de pagos.
- Métodos de pago más utilizados.
- Distribución de pagos por tipo.
- Preferencias de pago de los clientes.

Este análisis puede apoyar decisiones relacionadas con promociones, métodos de cobro y experiencia de compra.

Consulta base:

```sql
SELECT COUNT(*) 
FROM pagos;
```

---

## 8. Módulo de envíos

Este módulo permite analizar el estado de los envíos registrados.

Puede mostrar información como:

- Envíos completados.
- Envíos pendientes.
- Envíos cancelados.
- Envíos en proceso.

Este módulo ayuda a detectar posibles problemas logísticos y evaluar el cumplimiento de entregas.

Consulta base:

```sql
SELECT COUNT(*) 
FROM envios;
```

---

## 9. Módulo de devoluciones

Este módulo permite analizar las devoluciones realizadas por los clientes.

Puede mostrar:

- Total de devoluciones.
- Motivos principales de devolución.
- Productos con más devoluciones.
- Tendencias de devolución.

Este módulo es útil para identificar problemas de calidad, errores en descripciones, fallas en productos o inconformidades de los clientes.

Consulta base:

```sql
SELECT COUNT(*) 
FROM devoluciones;
```

---

## 10. Módulo de búsquedas

Este módulo permite analizar las búsquedas realizadas dentro de la plataforma.

Uno de los indicadores principales es detectar búsquedas con pocos resultados. Esto puede indicar falta de productos, baja disponibilidad o problemas en el catálogo.

Consulta base:

```sql
SELECT COUNT(*) 
FROM busquedas 
WHERE resultados_encontrados <= 5;
```

Este módulo puede apoyar decisiones relacionadas con surtido de productos y mejora de la experiencia del usuario.

---

## 11. Módulo de reseñas

Este módulo permite analizar la calificación de los productos.

Puede mostrar información como:

- Promedio de calificaciones.
- Productos peor calificados.
- Productos mejor calificados.
- Posibles problemas de satisfacción del cliente.

Consulta base:

```sql
SELECT IFNULL(AVG(calificacion), 0) 
FROM resenas;
```

Este módulo ayuda a identificar productos que pueden requerir revisión, mejora o sustitución.

---

## 12. Módulo de reportes PDF

Este módulo permite generar reportes en formato PDF con los resultados obtenidos en la aplicación.

Los reportes pueden incluir:

- Nombre del KPI.
- Periodo consultado.
- Resultado principal.
- Gráfica.
- Interpretación.
- Recomendación.
- Fecha de generación.

Este módulo facilita la entrega de evidencias y permite presentar los resultados de manera más clara.

---

# KPIs principales de la aplicación

## KPI 1: Valor del inventario

Permite conocer el valor total del inventario disponible, considerando el precio y el stock de los productos.

```sql
SELECT IFNULL(SUM(precio * stock), 0) 
FROM productos;
```

---

## KPI 2: Valor por categoría y marca

Permite identificar qué categorías y marcas concentran mayor valor dentro del inventario.

```sql
SELECT 
    c.nombre_categoria,
    m.nombre_marca,
    ROUND(SUM(p.precio * p.stock), 2) AS valor_total_inventario
FROM productos p
INNER JOIN categorias c ON p.id_categoria = c.id_categoria
INNER JOIN marcas m ON p.id_marca = m.id_marca
GROUP BY c.nombre_categoria, m.nombre_marca
ORDER BY valor_total_inventario DESC;
```

---

## KPI 3: Ventas por mes

Permite analizar el comportamiento de las ventas a lo largo del tiempo.

```sql
SELECT IFNULL(SUM(total_pagar), 0) 
FROM pedidos;
```

---

## KPI 4: Pedidos por estado

Permite conocer la cantidad de pedidos registrados de acuerdo con su estado.

```sql
SELECT COUNT(*) 
FROM pedidos;
```

---

## KPI 5: Productos por categoría

Permite visualizar cuántos productos existen por categoría.

```sql
SELECT COUNT(*) 
FROM productos;
```

---

## KPI 6: Uso de métodos de pago

Permite identificar los métodos de pago utilizados por los clientes.

```sql
SELECT COUNT(*) 
FROM pagos;
```

---

## KPI 7: Envíos por estado

Permite analizar el estado de los envíos.

```sql
SELECT COUNT(*) 
FROM envios;
```

---

## KPI 8: Devoluciones por motivo

Permite identificar los principales motivos por los que los clientes realizan devoluciones.

```sql
SELECT COUNT(*) 
FROM devoluciones;
```

---

## KPI 9: Búsquedas con pocos resultados

Permite detectar búsquedas donde los usuarios encontraron pocos productos.

```sql
SELECT COUNT(*) 
FROM busquedas 
WHERE resultados_encontrados <= 5;
```

---

## KPI 10: Productos peor calificados

Permite identificar productos con calificaciones bajas.

```sql
SELECT IFNULL(AVG(calificacion), 0) 
FROM resenas;
```

---

# Dashboard

El dashboard permite representar visualmente los resultados obtenidos mediante consultas SQL.

Su objetivo es facilitar la interpretación rápida de los datos y apoyar la toma de decisiones comerciales.

El dashboard incluye gráficas relacionadas con:

- Valor de inventario.
- Categorías con mayor valor.
- Marcas con mayor cantidad de productos.
- Ventas por mes.
- Pedidos por estado.
- Métodos de pago.
- Envíos por estado.
- Devoluciones por motivo.
- Productos peor calificados.

Estas visualizaciones ayudan a comprender los datos de manera más clara que una consulta SQL tradicional.

# Conclusión

Este proyecto permitió integrar un proceso ETL con una aplicación móvil enfocada en la toma de decisiones comerciales.

A través de la extracción, transformación y carga de datos, fue posible preparar información limpia, consistente y confiable. Posteriormente, estos datos fueron utilizados dentro de la aplicación para generar KPIs, gráficas, análisis y reportes PDF.

La aplicación facilita la interpretación de información comercial relacionada con productos, ventas, inventario, pedidos, pagos, envíos, devoluciones, búsquedas y reseñas.

En general, el proyecto demuestra que la calidad de los datos es fundamental para obtener resultados útiles. No basta con tener mucha información; también es necesario limpiarla, validarla y organizarla correctamente para que pueda apoyar la toma de decisiones.

---
