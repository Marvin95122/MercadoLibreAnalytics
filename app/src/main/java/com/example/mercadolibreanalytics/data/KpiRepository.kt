package com.example.mercadolibreanalytics.data

import android.content.Context
import android.database.Cursor

data class DashboardKpi(
    val title: String,
    val value: String,
    val description: String
)

data class KpiOption(
    val id: String,
    val title: String,
    val description: String,
    val section: String = "General"
)

data class ChartItem(
    val label: String,
    val value: Double
)

data class KpiDetail(
    val title: String,
    val mainValue: String,
    val description: String,
    val interpretation: String,
    val recommendation: String,
    val chartItems: List<ChartItem>,
    val valuePrefix: String = "",
    val valueSuffix: String = ""
)

data class ExecutiveAlert(
    val title: String,
    val value: String,
    val level: String,
    val description: String
)

class KpiRepository(context: Context) {

    private val database = DatabaseHelper(context).getDatabase()

    fun getExecutiveAlerts(): List<ExecutiveAlert> {
        val stockCritico = getInt(
            """
        SELECT COUNT(*) 
        FROM productos 
        WHERE stock <= 5
        """
        )

        val busquedasPocosResultados = getInt(
            """
        SELECT COUNT(*) 
        FROM busqueda 
        WHERE resultados_encontrados <= 5
        """
        )

        val devoluciones = getInt(
            """
        SELECT COUNT(*) 
        FROM devoluciones
        """
        )

        val pagosRechazados = getInt(
            """
        SELECT COUNT(*) 
        FROM pagos 
        WHERE LOWER(estado_pago) IN ('rechazado', 'rechazada', 'fallido', 'cancelado')
        """
        )

        val enviosRetrasados = getInt(
            """
        SELECT COUNT(*) 
        FROM envios 
        WHERE fecha_entrega_real > fecha_entrega_estimada
        """
        )

        val pedidosCancelados = getInt(
            """
        SELECT COUNT(*) 
        FROM pedido 
        WHERE LOWER(estado_pedido) IN ('cancelado', 'cancelada')
        """
        )

        return listOf(
            ExecutiveAlert(
                title = "Demanda no atendida",
                value = formatNumber(busquedasPocosResultados),
                level = getRiskLevel(busquedasPocosResultados, 1000, 3000),
                description = "Búsquedas frecuentes con pocos resultados disponibles."
            ),
            ExecutiveAlert(
                title = "Devoluciones",
                value = formatNumber(devoluciones),
                level = getRiskLevel(devoluciones, 1000, 5000),
                description = "Solicitudes de devolución registradas en la plataforma."
            ),
            ExecutiveAlert(
                title = "Pagos rechazados",
                value = formatNumber(pagosRechazados),
                level = getRiskLevel(pagosRechazados, 500, 2000),
                description = "Pagos que no fueron aprobados correctamente."
            ),
            ExecutiveAlert(
                title = "Envíos retrasados",
                value = formatNumber(enviosRetrasados),
                level = getRiskLevel(enviosRetrasados, 300, 1000),
                description = "Entregas realizadas después de la fecha estimada."
            ),
            ExecutiveAlert(
                title = "Pedidos cancelados",
                value = formatNumber(pedidosCancelados),
                level = getRiskLevel(pedidosCancelados, 500, 2000),
                description = "Pedidos cancelados dentro del flujo comercial."
            )
        )
    }

    private fun getRiskLevel(value: Int, mediumLimit: Int, highLimit: Int): String {
        return when {
            value >= highLimit -> "Alto"
            value >= mediumLimit -> "Medio"
            else -> "Bajo"
        }
    }

    fun getDashboardKpis(): List<DashboardKpi> {
        return listOf(
            DashboardKpi(
                title = "Ventas totales",
                value = formatCurrency(getDouble("SELECT IFNULL(SUM(total_pagar), 0) FROM pedido")),
                description = "Ingresos acumulados por pedidos registrados"
            ),
            DashboardKpi(
                title = "Pedidos registrados",
                value = formatNumber(getInt("SELECT COUNT(*) FROM pedido")),
                description = "Total de pedidos almacenados en la base"
            ),
            DashboardKpi(
                title = "Productos registrados",
                value = formatNumber(getInt("SELECT COUNT(*) FROM productos")),
                description = "Cantidad total de productos del catálogo"
            ),
            DashboardKpi(
                title = "Usuarios registrados",
                value = formatNumber(getInt("SELECT COUNT(*) FROM usuarios")),
                description = "Clientes o usuarios registrados en la plataforma"
            ),
            DashboardKpi(
                title = "Tiendas registradas",
                value = formatNumber(getInt("SELECT COUNT(*) FROM tiendas")),
                description = "Tiendas o vendedores dentro de la plataforma"
            ),
            DashboardKpi(
                title = "Pagos aprobados",
                value = formatNumber(getInt("SELECT COUNT(*) FROM pagos WHERE LOWER(estado_pago) = 'aprobado'")),
                description = "Pagos confirmados correctamente"
            ),
            DashboardKpi(
                title = "Devoluciones",
                value = formatNumber(getInt("SELECT COUNT(*) FROM devoluciones")),
                description = "Solicitudes de devolución registradas"
            ),
            DashboardKpi(
                title = "Calificación promedio",
                value = String.format("%.2f", getDouble("SELECT IFNULL(AVG(calificacion), 0) FROM resenas")),
                description = "Promedio general de reseñas de productos"
            )
        )
    }

    fun getKpiOptions(): List<KpiOption> {
        return listOf(
            KpiOption(
                id = "etl_valor_inventario",
                title = "Valor del inventario",
                description = "Valor económico total calculado con precio por stock",
                section = "Inventario"
            ),
            KpiOption(
                id = "etl_valor_categoria_marca",
                title = "Valor por categoría y marca",
                description = "Combinaciones con mayor valor económico de inventario",
                section = "Inventario"
            ),
            KpiOption(
                id = "ventas_mes",
                title = "Ventas por mes",
                description = "Evolución mensual de ingresos generados por pedidos",
                section = "Ventas"
            ),
            KpiOption(
                id = "pedidos_estado",
                title = "Pedidos por estado",
                description = "Distribución de pedidos según su situación actual",
                section = "Ventas"
            ),
            KpiOption(
                id = "productos_categoria",
                title = "Productos por categoría",
                description = "Categorías con mayor cantidad de productos publicados",
                section = "Ventas"
            ),
            KpiOption(
                id = "metodos_pago",
                title = "Métodos de pago",
                description = "Formas de pago más utilizadas por los compradores",
                section = "Operación"
            ),
            KpiOption(
                id = "envios_estado",
                title = "Envíos por estado",
                description = "Situación logística de los pedidos enviados",
                section = "Operación"
            ),
            KpiOption(
                id = "devoluciones_motivo",
                title = "Devoluciones por motivo",
                description = "Principales causas de devolución registradas",
                section = "Operación"
            ),
            KpiOption(
                id = "busquedas_pocos_resultados",
                title = "Búsquedas con pocos resultados",
                description = "Palabras clave con demanda y baja oferta disponible",
                section = "Comportamiento"
            ),
            KpiOption(
                id = "productos_peor_calificados",
                title = "Productos peor calificados",
                description = "Productos con menor promedio de calificación",
                section = "Comportamiento"
            )
        )
    }

    fun getKpiDetail(kpiId: String): KpiDetail {
        return when (kpiId) {
            "etl_total_productos" -> KpiDetail(
                title = "Inventario cargado",
                mainValue = formatNumber(
                    getInt("SELECT COUNT(*) FROM productos")
                ),
                description = "Muestra la cantidad total de productos disponibles en la base local.",
                interpretation = "Este indicador confirma el volumen total de productos registrados en la base local para el análisis del inventario.",
                recommendation = "Usar este total como punto de control para revisar crecimiento del catálogo, cobertura de categorías y concentración de productos por marca o tienda.",
                chartItems = getChartItems(
                    """
        SELECT 'Productos registrados' AS label,
               COUNT(*) AS value
        FROM productos
        """
                )
            )

            "etl_valor_inventario" -> KpiDetail(
                title = "Valor del inventario",
                mainValue = formatCurrency(getDouble("SELECT IFNULL(SUM(precio * stock), 0) FROM productos")),
                description = "Calcula el valor económico total del inventario disponible.",
                interpretation = "Este indicador permite conocer cuánto dinero representa el inventario actual. Si el valor es alto, puede indicar una fuerte concentración de capital en productos almacenados.",
                recommendation = "Priorizar la rotación de categorías con mayor valor de inventario mediante descuentos controlados, envío gratis en productos seleccionados y paquetes promocionales para liberar mercancía de alto costo.",
                chartItems = getChartItems(
                    """
        SELECT c.nombre_categoria AS label,
               ROUND(SUM(p.precio * p.stock), 2) AS value
        FROM productos p
        INNER JOIN categorias c ON p.id_categoria = c.id_categoria
        GROUP BY c.nombre_categoria
        ORDER BY value DESC
        LIMIT 30 
        """
                ),
                valuePrefix = "$"
            )

            "etl_stock_total" -> KpiDetail(
                title = "Stock disponible",
                mainValue = formatNumber(getInt("SELECT IFNULL(SUM(stock), 0) FROM productos")),
                description = "Muestra la cantidad total de unidades disponibles en inventario.",
                interpretation = "Este indicador permite conocer la capacidad de respuesta del inventario. Un stock alto puede ser positivo si existe demanda, pero también puede representar acumulación si los productos no tienen suficiente rotación.",
                recommendation = "Revisar las categorías con mayor stock y aplicar promociones por volumen, descuentos por temporada o campañas para productos con baja rotación.",
                chartItems = getChartItems(
                    """
                    SELECT c.nombre_categoria AS label,
                    SUM(p.stock) AS value
                    FROM productos p
                    INNER JOIN categorias c ON p.id_categoria = c.id_categoria
                                GROUP BY c.nombre_categoria
                    ORDER BY value DESC
                    LIMIT 30
                    """
                )
            )

            "etl_precio_promedio" -> KpiDetail(
                title = "Precio promedio",
                mainValue = formatCurrency(getDouble("SELECT IFNULL(AVG(precio), 0) FROM productos")),
                description = "Muestra el precio promedio general de los productos registrados.",
                interpretation = "Este indicador ayuda a entender el nivel general de precios del catálogo. Si el precio promedio es elevado, puede requerirse una estrategia de financiamiento, descuentos o segmentación por tipo de cliente.",
                recommendation = "Para categorías con precio promedio alto, se recomienda ofrecer meses sin intereses, cupones limitados, envío gratis o promociones por compra mínima.",
                chartItems = getChartItems(
                    """
                    SELECT c.nombre_categoria AS label,
                            ROUND(AVG(p.precio), 2) AS value
                    FROM productos p
                    INNER JOIN categorias c ON p.id_categoria = c.id_categoria
                                GROUP BY c.nombre_categoria
                    ORDER BY value DESC
                    LIMIT 30
                    """
                ),
                valuePrefix = "$"
            )

            "etl_productos_estado" -> KpiDetail(
                title = "Productos por estado",
                mainValue = formatNumber(getInt("SELECT COUNT(*) FROM productos")),
                description = "Muestra la distribución de productos según su estado dentro del catálogo.",
                interpretation = "Este indicador permite conocer cuántos productos están activos, pausados, agotados o eliminados. Si hay demasiados productos pausados o agotados, el catálogo pierde disponibilidad para el comprador.",
                recommendation = "Revisar productos pausados o agotados. Para productos agotados con demanda, priorizar reabastecimiento. Para productos pausados, mejorar precio, fotografías, descripción y condiciones de publicación.",
                chartItems = getChartItems(
                    """
                    SELECT estado_producto AS label,
                           COUNT(*) AS value
                    FROM productos
                    GROUP BY estado_producto
                    ORDER BY value DESC
                    """
                )
            )

            "etl_marcas_productos" -> KpiDetail(
                title = "Marcas con más productos",
                mainValue = formatNumber(getInt("SELECT COUNT(DISTINCT id_marca) FROM productos")),
                description = "Identifica las marcas con mayor cantidad de productos dentro del inventario.",
                interpretation = "Este indicador muestra qué marcas tienen mayor presencia en el catálogo. Una concentración alta en pocas marcas puede limitar la variedad comercial.",
                recommendation = "Fortalecer marcas con buen desempeño y buscar nuevas marcas en categorías con baja cobertura para ampliar la variedad disponible para el comprador.",
                chartItems = getChartItems(
                    """
                    SELECT m.nombre_marca AS label,
                           COUNT(p.id_producto) AS value
                    FROM productos p
                    INNER JOIN marcas m ON p.id_marca = m.id_marca
                                GROUP BY m.nombre_marca
                    ORDER BY value DESC
                    LIMIT 30
                    """
                )
            )

            "etl_valor_categoria_marca" -> KpiDetail(
                title = "Valor por categoría y marca",
                mainValue = formatCurrency(getDouble("SELECT IFNULL(SUM(precio * stock), 0) FROM productos")),
                description = "Muestra las combinaciones de categoría y marca con mayor valor económico de inventario.",
                interpretation = "Este indicador permite detectar dónde se concentra el mayor valor económico del inventario. Es útil para decidir qué categorías y marcas deben recibir mayor atención comercial.",
                recommendation = "En las combinaciones con mayor valor, aplicar campañas de rotación, descuentos graduales, promociones por paquete o envío gratis para evitar acumulación de inventario costoso.",
                chartItems = getChartItems(
                    """
                    SELECT c.nombre_categoria || ' - ' || m.nombre_marca AS label,
                           ROUND(SUM(p.precio * p.stock), 2) AS value
                    FROM productos p
                    INNER JOIN categorias c ON p.id_categoria = c.id_categoria
                    INNER JOIN marcas m ON p.id_marca = m.id_marca
                                GROUP BY c.nombre_categoria, m.nombre_marca
                    ORDER BY value DESC
                    LIMIT 30
                    """
                ),
                valuePrefix = "$"
            )

            "ventas_mes" -> KpiDetail(
                title = "Ventas mensuales",
                mainValue = formatCurrency(getDouble("SELECT IFNULL(SUM(total_pagar), 0) FROM pedido")),
                description = "Muestra el ingreso mensual generado por los pedidos registrados.",
                interpretation = "Este indicador compara las ventas de cada mes para detectar meses fuertes, meses débiles y posibles caídas. Una caída ocurre cuando un mes vende menos que el mes anterior o queda por debajo del promedio del año. Esta lectura ayuda a identificar cuándo conviene reforzar promociones, campañas o disponibilidad de productos.",
                recommendation = "Utilizar el análisis por año para identificar el mes con menor venta, la mayor caída mensual y la comparación contra el año anterior. La acción comercial específica se genera automáticamente en la tarjeta de comportamiento.",
                chartItems = getChartItems(
                    """
                    SELECT substr(fecha_pedido, 1, 7) AS label,
                           ROUND(SUM(total_pagar), 2) AS value
                    FROM pedido
                    GROUP BY substr(fecha_pedido, 1, 7)
                    ORDER BY label
                    """
                ),
                valuePrefix = "$"
            )

            "pedidos_estado" -> KpiDetail(
                title = "Pedidos por estado",
                mainValue = formatNumber(getInt("SELECT COUNT(*) FROM pedido")),
                description = "Presenta la cantidad de pedidos agrupados por estado.",
                interpretation = "Permite conocer si existen demasiados pedidos pendientes, cancelados o en proceso.",
                recommendation = "Dar seguimiento a los estados con mayor acumulación para mejorar el flujo operativo.",
                chartItems = getChartItems(
                    """
                    SELECT estado_pedido AS label,
                           COUNT(*) AS value
                    FROM pedido
                    GROUP BY estado_pedido
                    ORDER BY value DESC
                    """
                )
            )

            "productos_categoria" -> KpiDetail(
                title = "Productos por categoría",
                mainValue = formatNumber(getInt("SELECT COUNT(*) FROM productos")),
                description = "Muestra las categorías con mayor cantidad de productos.",
                interpretation = "Ayuda a detectar categorías saturadas o categorías con baja presencia en el catálogo.",
                recommendation = "Equilibrar el catálogo y fortalecer categorías con poca oferta pero alta demanda.",
                chartItems = getChartItems(
                    """
                    SELECT c.nombre_categoria AS label,
                           COUNT(p.id_producto) AS value
                    FROM productos p
                    INNER JOIN categorias c ON p.id_categoria = c.id_categoria
                    GROUP BY c.id_categoria, c.nombre_categoria
                    ORDER BY value DESC
                    LIMIT 30
                    """
                )
            )

            "stock_critico" -> KpiDetail(
                title = "Stock crítico",
                mainValue = formatNumber(getInt("SELECT COUNT(*) FROM productos WHERE stock <= 5")),
                description = "Lista productos con bajo inventario disponible.",
                interpretation = "Un stock bajo puede ocasionar pérdida de ventas y afectar la disponibilidad del catálogo.",
                recommendation = "Reabastecer productos críticos y priorizar aquellos con mayor rotación comercial.",
                chartItems = getChartItems(
                    """
                    SELECT nombre_producto AS label,
                           stock AS value
                    FROM productos
                    ORDER BY stock ASC
                    LIMIT 30
                    """
                )
            )

            "metodos_pago" -> KpiDetail(
                title = "Uso de métodos de pago",
                mainValue = formatNumber(getInt("SELECT COUNT(*) FROM pagos")),
                description = "Muestra cómo se distribuyen las formas de pago utilizadas por los compradores.",
                interpretation = "Este indicador permite identificar qué medios de pago tienen mayor uso y cuáles podrían requerir revisión. Si una forma de pago concentra demasiada operación, conviene asegurar su estabilidad. Si otra tiene bajo uso, puede indicar falta de confianza, poca visibilidad o problemas durante el pago.",
                recommendation = "Reforzar Mercado Pago como opción principal, revisar el comportamiento de tarjetas de débito y crédito por separado, y mostrar alternativas claras cuando una operación con tarjeta sea rechazada.",
                chartItems = getChartItems(
                    """
                    SELECT 
                        CASE LOWER(metodo_pago)
                            WHEN 'tarjeta' THEN 'Tarjeta general'
                            WHEN 'debito' THEN 'Tarjeta de débito'
                            WHEN 'credito' THEN 'Tarjeta de crédito'
                            WHEN 'transferencia' THEN 'Transferencia bancaria'
                            WHEN 'mercado_pago' THEN 'Mercado Pago'
                            WHEN 'efectivo' THEN 'Efectivo'
                            ELSE metodo_pago
                        END AS label,
                        COUNT(*) AS value
                    FROM pagos
                    GROUP BY LOWER(metodo_pago)
                    ORDER BY value DESC
                    """
                )
            )

            "envios_estado" -> KpiDetail(
                title = "Envíos por estado",
                mainValue = formatNumber(getInt("SELECT COUNT(*) FROM envios")),
                description = "Presenta la distribución de envíos por estado logístico.",
                interpretation = "Permite observar si existen retrasos, entregas pendientes o problemas de distribución.",
                recommendation = "Revisar los estados con mayor acumulación para mejorar la eficiencia logística.",
                chartItems = getChartItems(
                    """
                    SELECT estado_envio AS label,
                           COUNT(*) AS value
                    FROM envios
                    GROUP BY estado_envio
                    ORDER BY value DESC
                    """
                )
            )

            "busquedas_pocos_resultados" -> KpiDetail(
                title = "Búsquedas con pocos resultados",
                mainValue = formatNumber(getInt("SELECT COUNT(*) FROM busqueda WHERE resultados_encontrados <= 5")),
                description = "Detecta palabras clave buscadas por usuarios con pocos productos disponibles.",
                interpretation = "Este KPI muestra una posible demanda no atendida dentro de la plataforma.",
                recommendation = "Aumentar la oferta de productos relacionados con esas búsquedas y atraer vendedores de esas categorías.",
                chartItems = getChartItems(
                    """
                    SELECT palabra_clave AS label,
                           COUNT(*) AS value
                    FROM busqueda
                    WHERE resultados_encontrados <= 5
                    GROUP BY palabra_clave
                    ORDER BY value DESC
                    LIMIT 30
                    """
                )
            )

            "devoluciones_motivo" -> KpiDetail(
                title = "Devoluciones por motivo",
                mainValue = formatNumber(getInt("SELECT COUNT(*) FROM devoluciones")),
                description = "Agrupa las devoluciones según el motivo registrado.",
                interpretation = "Permite identificar problemas de calidad, descripción, entrega o satisfacción del cliente.",
                recommendation = "Atender los motivos más frecuentes para reducir devoluciones y mejorar la experiencia del comprador.",
                chartItems = getChartItems(
                    """
                    SELECT motivo_devolucion AS label,
                           COUNT(*) AS value
                    FROM devoluciones
                    GROUP BY motivo_devolucion
                    ORDER BY value DESC
                    """
                )
            )

            "productos_peor_calificados" -> KpiDetail(
                title = "Productos peor calificados",
                mainValue = String.format("%.2f", getDouble("SELECT IFNULL(AVG(calificacion), 0) FROM resenas")),
                description = "Muestra productos con menor promedio de calificación.",
                interpretation = "Una baja calificación puede afectar la confianza del comprador y disminuir la recompra.",
                recommendation = "Revisar productos mal evaluados, mejorar descripciones, calidad, empaque o atención postventa.",
                chartItems = getChartItems(
                    """
                    SELECT p.nombre_producto AS label,
                           ROUND(AVG(r.calificacion), 2) AS value
                    FROM resenas r
                    INNER JOIN productos p ON r.id_producto = p.id_producto
                    GROUP BY p.id_producto, p.nombre_producto
                    ORDER BY value ASC
                    LIMIT 30
                    """
                ),
                valueSuffix = "/5"
            )

            else -> KpiDetail(
                title = "KPI no encontrado",
                mainValue = "0",
                description = "No existe información para este indicador.",
                interpretation = "No hay datos disponibles.",
                recommendation = "Seleccione otro KPI.",
                chartItems = emptyList()
            )
        }
    }

    private fun getChartItems(query: String): List<ChartItem> {
        val items = mutableListOf<ChartItem>()
        val cursor: Cursor = database.rawQuery(query.trimIndent(), null)

        cursor.use {
            while (it.moveToNext()) {
                val label = it.getString(0) ?: "Sin dato"
                val value = it.getDouble(1)
                items.add(ChartItem(label, value))
            }
        }

        return items
    }

    private fun getInt(query: String): Int {
        val cursor = database.rawQuery(query, null)
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    private fun getDouble(query: String): Double {
        val cursor = database.rawQuery(query, null)
        cursor.use {
            return if (it.moveToFirst()) it.getDouble(0) else 0.0
        }
    }

    private fun formatCurrency(value: Double): String {
        return "$" + String.format("%,.2f", value)
    }

    private fun formatNumber(value: Int): String {
        return String.format("%,d", value)
    }
}