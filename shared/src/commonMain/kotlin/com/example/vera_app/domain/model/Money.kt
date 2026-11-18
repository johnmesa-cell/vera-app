package com.example.vera_app.domain.model

import kotlin.jvm.JvmInline

/**
 * Representa un valor monetario expresado en la unidad mínima entera.
 * Evita el uso de Float o Double para garantizar precisión exacta en operaciones financieras.
 */
@JvmInline
value class Money(val cents: Long = 0L) : Comparable<Money> {

    init {
        require(cents >= 0L) { "El monto monetario no puede ser negativo: $cents" }
    }

    fun isPositive(): Boolean = cents > 0L

    fun isZero(): Boolean = cents == 0L

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun minus(other: Money): Money {
        require(cents >= other.cents) { "No se puede restar un monto mayor al saldo actual: $other > $this" }
        return Money(cents - other.cents)
    }

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    override fun toString(): String = "$cents"

    companion object {
        val ZERO = Money(0L)

        fun fromMajorUnits(units: Long): Money = Money(units)

        fun of(cents: Long): Money = Money(cents)
    }
}
