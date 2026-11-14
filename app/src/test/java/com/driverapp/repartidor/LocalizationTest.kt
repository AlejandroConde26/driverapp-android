package com.driverapp.repartidor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Comprueba que la app está completamente traducida.
 *
 * Sin esto, una cadena nueva en `values/strings.xml` se queda sin traducir y en inglés
 * aparece en español: es justo el tipo de fallo que no salta hasta que se cambia el
 * idioma en el móvil.
 */
class LocalizationTest {

    private fun load(path: String): Map<String, String> {
        val file = File(path)
        assertTrue("No existe $path", file.exists())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val out = mutableMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as Element
            out[el.getAttribute("name")] = el.textContent ?: ""
        }
        return out
    }

    private val es by lazy { load("src/main/res/values/strings.xml") }
    private val en by lazy { load("src/main/res/values-en/strings.xml") }

    @Test
    fun `todos los idiomas tienen las mismas claves`() {
        val faltanEn = es.keys - en.keys
        val sobranEn = en.keys - es.keys
        assertEquals("Faltan en values-en: $faltanEn", emptySet<String>(), faltanEn)
        assertEquals("Sobran en values-en: $sobranEn", emptySet<String>(), sobranEn)
    }

    @Test
    fun `los argumentos de formato coinciden entre idiomas`() {
        val pattern = Regex("""%(\d+\$)?[sdf]""")
        val desalineadas = es.keys.filter { key ->
            val a = pattern.findAll(es.getValue(key)).map { it.value }.sorted().toList()
            val b = pattern.findAll(en.getValue(key)).map { it.value }.sorted().toList()
            a != b
        }
        assertEquals("Cadenas con argumentos distintos: $desalineadas", emptyList<String>(), desalineadas)
    }

    @Test
    fun `el idioma ingles no contiene texto en espanol`() {
        // Marcadores inequívocos: acentos españoles y palabras que no existen en inglés.
        // (No se incluyen "no", "al", "se": son palabras inglesas legítimas y darían
        // falsos positivos.)
        val espanol = Regex(
            """[áéíóúñ¿¡]|\b(pedido|pedidos|entrega|cliente|repartidor|ubicación|para|sin|del|más|vehículo)\b""",
            RegexOption.IGNORE_CASE
        )
        val sinTraducir = en.filter { (key, value) ->
            key != "language_es" && value.length > 6 && espanol.containsMatchIn(value)
        }.keys
        assertEquals("Sin traducir en values-en: $sinTraducir", emptySet<String>(), sinTraducir)
    }

    @Test
    fun `las cadenas vacias solo son aceptables en simbolos`() {
        val vacias = (es.filterValues { it.isBlank() }.keys + en.filterValues { it.isBlank() }.keys)
        assertEquals(emptySet<String>(), vacias)
    }
}
