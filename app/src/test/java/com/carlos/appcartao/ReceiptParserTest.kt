package com.carlos.appcartao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {
    private val categories = listOf(
        "Mercado", "Padaria", "Lanches", "Sorvetes", "Posto de gasolina",
        "Farmácia", "Saúde", "Restaurante", "Assinaturas", "Outros"
    )

    @Test
    fun supermarketReceiptUsesFinalTotalAndDate() {
        val result = ReceiptParser.parse(
            """
            SUPERMERCADO BOM PRECO LTDA
            CNPJ 12.345.678/0001-90
            2 ARROZ 24,90
            1 FEIJAO 8,50
            SUBTOTAL 33,40
            DESCONTO 3,40
            VALOR TOTAL R$ 30,00
            Data: 10/09/2026 18:42
            """.trimIndent(),
            categories
        )
        assertEquals(3000L, result.amountCents)
        assertEquals(LocalDate.of(2026, 9, 10), result.purchaseDate)
        assertEquals("Mercado", result.category)
        assertEquals("SUPERMERCADO BOM PRECO LTDA", result.description)
    }

    @Test
    fun fuelReceiptPrefersPaidAmountOverChange() {
        val result = ReceiptParser.parse(
            """
            POSTO CENTRAL
            GASOLINA COMUM 5,79
            TOTAL A PAGAR 150,00
            VALOR PAGO 200,00
            TROCO 50,00
            EMISSAO 09/09/2026
            """.trimIndent(),
            categories
        )
        // When both TOTAL A PAGAR and VALOR PAGO exist, the charged total is the purchase amount.
        assertEquals(15000L, result.amountCents)
        assertEquals("Posto de gasolina", result.category)
    }

    @Test
    fun fallbackFindsLargestMoneyWhenNoTotalLabelExists() {
        val result = ReceiptParser.parse(
            """
            PADARIA DO BAIRRO
            PAO 8,50
            CAFE 6,00
            14,50
            08/09/2026
            """.trimIndent(),
            categories
        )
        assertEquals(1450L, result.amountCents)
        assertEquals("Padaria", result.category)
        assertNotNull(result.purchaseDate)
    }
}
