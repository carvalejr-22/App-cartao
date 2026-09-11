package com.carlos.appcartao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurringPurchaseTest {
    private val card = ModernCardProfile(id = "card-test", name = "Teste", closingDay = 8, dueDay = 15)

    @Test
    fun recurringPurchaseCreatesFutureMonthlyOccurrences() {
        val start = LocalDate.now()
        val base = ModernPurchase(
            cardId = card.id,
            amountCents = 5290,
            purchaseDate = start,
            category = "Assinaturas",
            note = "Internet",
            isRecurring = true
        )
        val result = modernAddRecurringSeries(
            ModernAppData(cards = listOf(card), activeCardId = card.id),
            base,
            card
        )
        val rule = result.recurringRules.single()
        val occurrences = result.purchases.filter { it.recurringRuleId == rule.id }
        assertTrue(rule.active)
        assertTrue(occurrences.size >= 12)
        assertTrue(occurrences.any { it.purchaseDate == start })
        assertTrue(occurrences.all { it.amountCents == 5290L })
    }

    @Test
    fun deletingOneRecurringOccurrenceDoesNotRecreateIt() {
        val start = LocalDate.now()
        val base = ModernPurchase(
            cardId = card.id,
            amountCents = 3000,
            purchaseDate = start,
            category = "Assinaturas",
            note = "Streaming",
            isRecurring = true
        )
        val created = modernAddRecurringSeries(
            ModernAppData(cards = listOf(card), activeCardId = card.id),
            base,
            card
        )
        val target = created.purchases
            .filter { it.recurringRuleId != null }
            .sortedBy { it.purchaseDate }
            .drop(1)
            .first()
        val skipped = modernSkipRecurringOccurrence(created, target)
        val expandedAgain = modernEnsureRecurringSchedule(skipped)
        assertFalse(expandedAgain.purchases.any { it.id == target.id })
        assertFalse(expandedAgain.purchases.any {
            it.recurringRuleId == target.recurringRuleId && it.purchaseDate == target.purchaseDate
        })
        assertTrue(expandedAgain.recurringRules.single().skippedEpochDays.contains(target.purchaseDate.toEpochDay()))
    }

    @Test
    fun cancelRecurringKeepsHistoryAndRemovesSelectedAndFuture() {
        val start = LocalDate.now()
        val base = ModernPurchase(
            cardId = card.id,
            amountCents = 4500,
            purchaseDate = start,
            category = "Assinaturas",
            note = "Academia",
            isRecurring = true
        )
        val created = modernAddRecurringSeries(
            ModernAppData(cards = listOf(card), activeCardId = card.id),
            base,
            card
        )
        val ordered = created.purchases
            .filter { it.recurringRuleId != null }
            .sortedBy { it.purchaseDate }
        val target = ordered[2]
        val cancelled = modernCancelRecurringFrom(created, target)
        assertFalse(cancelled.recurringRules.single().active)
        assertTrue(cancelled.purchases.any {
            it.recurringRuleId == target.recurringRuleId && it.purchaseDate.isBefore(target.purchaseDate)
        })
        assertFalse(cancelled.purchases.any {
            it.recurringRuleId == target.recurringRuleId && !it.purchaseDate.isBefore(target.purchaseDate)
        })
        assertEquals(cancelled, modernEnsureRecurringSchedule(cancelled))
    }
}
