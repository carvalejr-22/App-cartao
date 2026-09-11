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

    @Test
    fun pharmacyNfceUsesAmountToPayNotPreDiscountTotal() {
        val result = ReceiptParser.parse(
            """
            DROGAM INAS VALENCA LTDA
            CNPJ 08.603.462/0001-05
            ALBENDAZOL 400MG 27,71
            ENTEROGERMINA 53,38
            Valor Total R$ 81,09
            Desconto(s) R$ -28,49
            VALOR A PAGAR R$ 52,60
            Cartao de Credito 52,60
            Troco R$ 0,00
            NFC-e 927982 Serie 001 08/09/2026 12:22
            Data de autorizacao: 08/09/2026 12:22
            """.trimIndent(),
            categories
        )
        assertEquals(5260L, result.amountCents)
        assertEquals(LocalDate.of(2026, 9, 8), result.purchaseDate)
        assertEquals("Farmácia", result.category)
        assertEquals("DROGAM INAS VALENCA LTDA", result.description)
    }

    @Test
    fun pharmacyCardSlipReadsPaymentValue() {
        val result = ReceiptParser.parse(
            """
            DROGAM INAS VALENCA LTDA
            RUA PADRE LUNA, 100 - VALENCA
            Data Emissao: 08/09/2026 12:22
            Comprovante vinculado
            Valor do Pagamento: 52,60
            VISA CREDITO
            VALOR: 52,60
            ULTRAPOPULAR PE LUNA 08 09 26-12:22
            TRANSACAO APROVADA PELO EMISSOR
            """.trimIndent(),
            categories
        )
        assertEquals(5260L, result.amountCents)
        assertEquals(LocalDate.of(2026, 9, 8), result.purchaseDate)
        assertEquals("Farmácia", result.category)
        assertEquals("DROGAM INAS VALENCA LTDA", result.description)
    }
}
