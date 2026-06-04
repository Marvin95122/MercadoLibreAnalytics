package com.example.mercadolibreanalytics.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.mercadolibreanalytics.data.DashboardKpi
import com.example.mercadolibreanalytics.data.KpiDetail
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.mercadolibreanalytics.data.ChartItem

object PdfGenerator {

    fun generateKpiReport(
        context: Context,
        detail: KpiDetail,
        selectedItem: ChartItem? = null,
        allItems: List<ChartItem> = detail.chartItems,
        reportPeriod: String = "Todos los datos disponibles"
    ): File {
        val pdfDocument = PdfDocument()

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
        }

        val sectionPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val normalPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 11f
        }

        val boldPaint = Paint().apply {
            color = Color.rgb(30, 90, 168)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val smallPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9.5f
        }

        val topItem = allItems.maxByOrNull { it.value }
        val minItem = allItems.minByOrNull { it.value }
        val selected = selectedItem ?: topItem
        val total = allItems.sumOf { it.value }

        val selectedPercent = if (selected != null && total > 0) {
            selected.value * 100.0 / total
        } else {
            0.0
        }

        val topPercent = if (topItem != null && total > 0) {
            topItem.value * 100.0 / total
        } else {
            0.0
        }

        // Página 1
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        canvas1.drawColor(Color.WHITE)

        val headerPaint = Paint().apply {
            color = Color.rgb(30, 90, 168)
        }

        canvas1.drawRect(0f, 0f, 595f, 118f, headerPaint)
        canvas1.drawText("MercadoLibre Analytics", 36f, 38f, titlePaint)
        canvas1.drawText("Reporte de indicador para toma de decisiones", 36f, 59f, subtitlePaint)
        canvas1.drawText("Periodo analizado: $reportPeriod", 36f, 79f, subtitlePaint)
        canvas1.drawText("Generado el ${getDate()}", 36f, 98f, subtitlePaint)

        var y = 148f

        canvas1.drawText(detail.title, 36f, y, sectionPaint)
        y += 24f

        y = drawWrappedText(
            canvas = canvas1,
            text = detail.description,
            x = 36f,
            startY = y,
            paint = normalPaint,
            maxWidth = 520f,
            lineHeight = 15f
        )

        y += 14f

        canvas1.drawText("Periodo analizado", 36f, y, sectionPaint)
        y += 22f
        y = drawWrappedText(
            canvas = canvas1,
            text = reportPeriod,
            x = 36f,
            startY = y,
            paint = normalPaint,
            maxWidth = 520f,
            lineHeight = 15f
        )

        y += 18f

        canvas1.drawText("Indicador principal", 36f, y, sectionPaint)
        y += 24f
        canvas1.drawText(detail.mainValue, 36f, y, boldPaint)

        y += 36f

        canvas1.drawText("Lectura rápida", 36f, y, sectionPaint)
        y += 22f

        val quickText = buildPdfQuickReadingText(
            detail = detail,
            topItem = topItem,
            minItem = minItem,
            topPercent = topPercent
        )

        y = drawWrappedText(
            canvas = canvas1,
            text = quickText,
            x = 36f,
            startY = y,
            paint = normalPaint,
            maxWidth = 520f,
            lineHeight = 15f
        )

        y += 18f

        canvas1.drawText("Detalle seleccionado", 36f, y, sectionPaint)
        y += 22f

        val selectedText = if (selected != null) {
            "Elemento seleccionado: ${selected.label}. Valor registrado: ${
                formatValue(
                    selected.value,
                    detail.valuePrefix,
                    detail.valueSuffix
                )
            }. Participación aproximada dentro del total analizado: ${String.format("%.2f", selectedPercent)}%."
        } else {
            "No se seleccionó ningún elemento de la gráfica."
        }

        y = drawWrappedText(
            canvas = canvas1,
            text = selectedText,
            x = 36f,
            startY = y,
            paint = normalPaint,
            maxWidth = 520f,
            lineHeight = 15f
        )

        y += 18f

        canvas1.drawText("Resultados principales", 36f, y, sectionPaint)
        y += 20f

        drawTableHeader(canvas1, y)
        y += 20f

        allItems.take(10).forEachIndexed { index, item ->
            val label = "${index + 1}. ${item.label}".take(48)
            val value = formatValue(item.value, detail.valuePrefix, detail.valueSuffix)

            canvas1.drawText(label, 42f, y, normalPaint)
            canvas1.drawText(value, 430f, y, normalPaint)

            y += 18f
        }

        canvas1.drawText(
            "Página 1 de 2 · Reporte generado desde base SQLite local.",
            36f,
            805f,
            smallPaint
        )

        pdfDocument.finishPage(page1)

        // Página 2
        val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val canvas2 = page2.canvas

        canvas2.drawColor(Color.WHITE)

        val headerPaint2 = Paint().apply {
            color = Color.rgb(15, 23, 42)
        }

        canvas2.drawRect(0f, 0f, 595f, 100f, headerPaint2)
        canvas2.drawText("MercadoLibre Analytics", 36f, 38f, titlePaint)
        canvas2.drawText("Análisis e interpretación del indicador", 36f, 60f, subtitlePaint)
        canvas2.drawText("Periodo analizado: $reportPeriod", 36f, 80f, subtitlePaint)

        var y2 = 135f

        canvas2.drawText("Lectura del indicador", 36f, y2, sectionPaint)
        y2 += 24f

        y2 = drawWrappedText(
            canvas = canvas2,
            text = detail.interpretation,
            x = 36f,
            startY = y2,
            paint = normalPaint,
            maxWidth = 520f,
            lineHeight = 15f
        )

        y2 += 20f

        canvas2.drawText("Acción sugerida", 36f, y2, sectionPaint)
        y2 += 24f

        y2 = drawWrappedText(
            canvas = canvas2,
            text = detail.recommendation,
            x = 36f,
            startY = y2,
            paint = normalPaint,
            maxWidth = 520f,
            lineHeight = 15f
        )

        y2 += 22f

        canvas2.drawText("Conclusión para la toma de decisiones", 36f, y2, sectionPaint)
        y2 += 24f

        val conclusion = buildPdfConclusion(
            detail = detail,
            selectedItem = selected,
            topItem = topItem,
            reportPeriod = reportPeriod
        )

        drawWrappedText(
            canvas = canvas2,
            text = conclusion,
            x = 36f,
            startY = y2,
            paint = normalPaint,
            maxWidth = 520f,
            lineHeight = 15f
        )

        canvas2.drawText(
            "Página 2 de 2 · Documento generado automáticamente por la aplicación móvil.",
            36f,
            805f,
            smallPaint
        )

        pdfDocument.finishPage(page2)

        val file = createPdfFile(context, "Reporte_${cleanFileName(detail.title)}")

        FileOutputStream(file).use { output ->
            pdfDocument.writeTo(output)
        }

        pdfDocument.close()
        return file
    }

    fun generateDashboardReport(context: Context, kpis: List<DashboardKpi>): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
        }

        val sectionPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val normalPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 11f
        }

        val valuePaint = Paint().apply {
            color = Color.rgb(30, 90, 168)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
        }

        canvas.drawRect(0f, 0f, 595f, 95f, headerPaint)
        canvas.drawText("MercadoLibre Analytics", 36f, 42f, titlePaint)
        canvas.drawText("Reporte ejecutivo general", 36f, 62f, subtitlePaint)
        canvas.drawText("Fecha: ${getDate()}", 36f, 80f, subtitlePaint)

        var y = 130f

        canvas.drawText("Resumen general de KPIs", 36f, y, sectionPaint)
        y += 28f

        kpis.forEach { kpi ->
            canvas.drawText(kpi.title, 42f, y, normalPaint)
            canvas.drawText(kpi.value, 390f, y, valuePaint)
            y += 16f

            y = drawWrappedText(
                canvas = canvas,
                text = kpi.description,
                x = 42f,
                startY = y,
                paint = normalPaint,
                maxWidth = 500f,
                lineHeight = 13f
            )

            y += 13f
        }

        y += 12f

        canvas.drawText("Conclusión ejecutiva", 36f, y, sectionPaint)
        y += 22f

        drawWrappedText(
            canvas = canvas,
            text = "La información presentada permite analizar el comportamiento general de la plataforma mediante ventas, pedidos, productos, usuarios, tiendas, pagos, devoluciones y calificación promedio. Estos indicadores apoyan la toma de decisiones estratégicas desde una aplicación móvil con base de datos local.",
            x = 36f,
            startY = y,
            paint = normalPaint,
            maxWidth = 520f,
            lineHeight = 15f
        )

        pdfDocument.finishPage(page)

        val file = createPdfFile(context, "Reporte_General_MercadoLibre")
        FileOutputStream(file).use { output ->
            pdfDocument.writeTo(output)
        }

        pdfDocument.close()
        return file
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "Compartir reporte PDF")
        )
    }

    private fun createPdfFile(context: Context, baseName: String): File {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(directory, "${baseName}_$timestamp.pdf")
    }

    private fun getDate(): String {
        val date = Date()
        val day = SimpleDateFormat("d", Locale.getDefault()).format(date)
        val monthNumber = SimpleDateFormat("MM", Locale.getDefault()).format(date).toIntOrNull() ?: 0
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        val monthName = when (monthNumber) {
            1 -> "enero"
            2 -> "febrero"
            3 -> "marzo"
            4 -> "abril"
            5 -> "mayo"
            6 -> "junio"
            7 -> "julio"
            8 -> "agosto"
            9 -> "septiembre"
            10 -> "octubre"
            11 -> "noviembre"
            12 -> "diciembre"
            else -> ""
        }
        return "$day de $monthName del $year, $time"
    }

    private fun cleanFileName(text: String): String {
        return text.replace(Regex("[^A-Za-z0-9_-]"), "_")
    }

    private fun drawTableHeader(canvas: android.graphics.Canvas, y: Float) {
        val paint = Paint().apply {
            color = Color.rgb(232, 240, 254)
        }

        val textPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawRect(36f, y - 14f, 555f, y + 7f, paint)
        canvas.drawText("Concepto", 42f, y, textPaint)
        canvas.drawText("Valor", 430f, y, textPaint)
    }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        text: String,
        x: Float,
        startY: Float,
        paint: Paint,
        maxWidth: Float,
        lineHeight: Float
    ): Float {
        val words = text.split(" ")
        var line = ""
        var y = startY

        words.forEach { word ->
            val testLine = if (line.isEmpty()) word else "$line $word"

            if (paint.measureText(testLine) <= maxWidth) {
                line = testLine
            } else {
                canvas.drawText(line, x, y, paint)
                y += lineHeight
                line = word
            }
        }

        if (line.isNotEmpty()) {
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }

        return y
    }

    private fun buildPdfQuickReadingText(
        detail: KpiDetail,
        topItem: ChartItem?,
        minItem: ChartItem?,
        topPercent: Double
    ): String {
        if (topItem == null || minItem == null) {
            return "No hay datos suficientes para generar una lectura rápida del indicador."
        }

        return when {
            detail.title.contains("Ventas", ignoreCase = true) -> {
                "El periodo con mayor venta es ${topItem.label}, con ${
                    formatValue(
                        topItem.value,
                        detail.valuePrefix,
                        detail.valueSuffix
                    )
                }. El periodo con menor venta es ${minItem.label}, con ${
                    formatValue(
                        minItem.value,
                        detail.valuePrefix,
                        detail.valueSuffix
                    )
                }. El mejor periodo concentra aproximadamente ${String.format("%.1f", topPercent)}% del total analizado."
            }

            detail.title.contains("Valor del inventario", ignoreCase = true) -> {
                "La mayor concentración económica se encuentra en ${topItem.label}, con ${
                    formatValue(
                        topItem.value,
                        detail.valuePrefix,
                        detail.valueSuffix
                    )
                }. Esto permite identificar dónde se concentra el mayor valor del inventario."
            }

            detail.title.contains("Stock disponible", ignoreCase = true) -> {
                "La categoría con mayor stock es ${topItem.label}, con ${
                    formatValue(
                        topItem.value,
                        "",
                        ""
                    )
                } unidades. La categoría con menor stock es ${minItem.label}."
            }

            detail.title.contains("Precio promedio", ignoreCase = true) -> {
                "La categoría con precio promedio más alto es ${topItem.label}, con ${
                    formatValue(
                        topItem.value,
                        detail.valuePrefix,
                        detail.valueSuffix
                    )
                }. La categoría con precio promedio más bajo es ${minItem.label}."
            }

            detail.title.contains("Métodos de pago", ignoreCase = true) ||
                    detail.title.contains("Uso de métodos de pago", ignoreCase = true) -> {
                "El método de pago más utilizado es ${topItem.label}, con ${
                    formatValue(
                        topItem.value,
                        "",
                        ""
                    )
                } registros. Esto permite identificar la forma de pago con mayor preferencia entre los compradores."
            }

            detail.title.contains("Devoluciones", ignoreCase = true) -> {
                "El motivo o grupo con mayor presencia es ${topItem.label}. Este resultado ayuda a detectar problemas recurrentes que pueden afectar la satisfacción del comprador."
            }

            detail.title.contains("Búsquedas", ignoreCase = true) -> {
                "La búsqueda con mayor presencia es ${topItem.label}. Esto puede indicar una oportunidad comercial o una demanda no atendida dentro del catálogo."
            }

            else -> {
                "El elemento con mayor valor es ${topItem.label}, con ${
                    formatValue(
                        topItem.value,
                        detail.valuePrefix,
                        detail.valueSuffix
                    )
                }. Su participación aproximada dentro del total analizado es de ${String.format("%.1f", topPercent)}%."
            }
        }
    }

    private fun buildPdfConclusion(
        detail: KpiDetail,
        selectedItem: ChartItem?,
        topItem: ChartItem?,
        reportPeriod: String
    ): String {
        val reference = selectedItem ?: topItem

        if (reference == null) {
            return "El indicador no cuenta con datos suficientes para generar una conclusión automática."
        }

        return when {
            detail.title.contains("Ventas", ignoreCase = true) -> {
                "El análisis de ventas corresponde a: $reportPeriod. Este reporte permite identificar meses fuertes, meses débiles y periodos con caída comercial. Con base en el periodo analizado, se recomienda reforzar las acciones comerciales en los meses con menor desempeño mediante cupones, envío gratis, promociones por tiempo limitado y campañas dirigidas a categorías con alta rotación."
            }

            detail.title.contains("Valor del inventario", ignoreCase = true) -> {
                "El indicador muestra dónde se concentra el mayor valor económico del inventario. La empresa debe vigilar estas categorías para evitar acumulación de capital detenido y promover rotación mediante descuentos controlados o paquetes promocionales."
            }

            detail.title.contains("Stock disponible", ignoreCase = true) -> {
                "El análisis de stock permite detectar acumulación o baja disponibilidad. Las categorías con mucho inventario deben revisarse para aplicar campañas de rotación, mientras que las categorías con bajo stock deben evaluarse para reabastecimiento."
            }

            detail.title.contains("Precio promedio", ignoreCase = true) -> {
                "El precio promedio ayuda a identificar categorías de alto valor y categorías de bajo costo. Para productos de precio elevado conviene facilitar la compra mediante meses sin intereses, cupones o envío gratis."
            }

            detail.title.contains("Métodos de pago", ignoreCase = true) ||
                    detail.title.contains("Uso de métodos de pago", ignoreCase = true) -> {
                "El comportamiento de métodos de pago permite conocer la preferencia del comprador. Los métodos más usados deben mantenerse estables y los métodos con menor uso deben revisarse para mejorar confianza, visibilidad o facilidad de uso."
            }

            detail.title.contains("Envíos", ignoreCase = true) -> {
                "El indicador logístico permite detectar concentración en estados de envío. Si existen retrasos o acumulaciones, conviene revisar tiempos de preparación, empresa de envío y zonas con mayor incidencia."
            }

            detail.title.contains("Búsquedas", ignoreCase = true) -> {
                "Las búsquedas con pocos resultados representan oportunidades de catálogo. La empresa puede atraer vendedores, ampliar productos relacionados y crear campañas para cubrir esa demanda no atendida."
            }

            detail.title.contains("Devoluciones", ignoreCase = true) -> {
                "Las devoluciones permiten detectar causas de insatisfacción. La empresa debe revisar los motivos más frecuentes y mejorar descripción, fotografías, calidad, empaque o tiempos de entrega."
            }

            detail.title.contains("Productos peor calificados", ignoreCase = true) -> {
                "Los productos con baja calificación pueden afectar la confianza del comprador. Se recomienda revisar calidad, descripción, fotografías, atención postventa y considerar pausar publicaciones problemáticas."
            }

            else -> {
                "El indicador permite priorizar decisiones sobre los elementos con mayor peso dentro del análisis. El resultado seleccionado debe revisarse con atención porque puede representar una oportunidad, riesgo o punto de mejora."
            }
        }
    }

    private fun formatValue(
        value: Double,
        prefix: String,
        suffix: String
    ): String {
        return when {
            prefix == "$" -> "$" + String.format("%,.0f", value)
            suffix.isNotEmpty() -> String.format("%.2f", value) + suffix
            value >= 1000 -> String.format("%,.0f", value)
            else -> String.format("%.0f", value)
        }
    }
}