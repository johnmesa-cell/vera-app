package com.example.vera_app.domain.model

import kotlin.jvm.JvmInline

/**
 * Representa un valor monetario inmutable expresado en la unidad mínima entera (centavos o sub-unidades).
 *
 * Convención de unidades:
 * - 1 Unidad Principal (ej. Pesos/Dólares) = 100 Centavos (Unidades Mínimas).
 * - Todas las propiedades y comparaciones internas operan sobre centavos (`cents`).
 * - Evita el uso de tipos de punto flotante (`Float` o `Double`) para garantizar precisión exacta.
 */
@JvmInline
value class Money(val cents: Long = 0L) : Comparable<Money> {

    init {
        require(cents >= 0L) { "El monto monetario en centavos no puede ser negativo: $cents" }
    }

    /** Retorna `true` si la cantidad de centavos es strictly mayor a 0. */
    fun isPositive(): Boolean = cents > 0L

    /** Retorna `true` si la cantidad de centavos es exactamente 0. */
    fun isZero(): Boolean = cents == 0L

    /** Devuelve la representación entera equivalente en unidades principales. */
    fun toMajorUnits(): Long = cents / CENTS_PER_MAJOR_UNIT

    /** Devuelve el valor exacto en centavos (unidades mínimas). */
    fun toCents(): Long = cents

    /** Suma dos montos comprobando desbordamiento aritmético. */
    operator fun plus(other: Money): Money {
        val sum = safeAdd(this.cents, other.cents)
        return Money(sum)
    }

    /** Resta dos montos comprobando que el resultado sea no negativo. */
    operator fun minus(other: Money): Money {
        require(cents >= other.cents) { "No se puede restar un monto mayor ($other) al saldo actual ($cents)." }
        return Money(cents - other.cents)
    }

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    override fun toString(): String = "$cents centavos"

    companion object {
        private const val CENTS_PER_MAJOR_UNIT: Long = 100L

        /** Instancia constante que representa un saldo de cero centavos. */
        val ZERO = Money(0L)

        /**
         * Crea un valor [Money] a partir de unidades principales (ej. pesos enteros).
         * Realiza la conversión explícita multiplicando por 100 centavos por unidad.
         *
         * @param units Cantidad en unidades principales (debe ser >= 0).
         */
        fun fromMajorUnits(units: Long): Money {
            require(units >= 0L) { "Las unidades principales no pueden ser negativas: $units" }
            val cents = safeMultiply(units, CENTS_PER_MAJOR_UNIT)
            return Money(cents)
        }

        /**
         * Crea un valor [Money] especificando directamente la cantidad en centavos.
         *
         * @param cents Cantidad exacta en unidades mínimas (debe ser >= 0).
         */
        fun of(cents: Long): Money = Money(cents)

        private fun safeAdd(a: Long, b: Long): Long {
            val result = a + b
            if ((a xor result) and (b xor result) < 0L) {
                throw ArithmeticException("Desbordamiento aritmético al sumar montos monetarios ($a + $b).")
            }
            return result
        }

        private fun safeMultiply(a: Long, b: Long): Long {
            if (a == 0L || b == 0L) return 0L
            val result = a * b
            if (a != result / b) {
                throw ArithmeticException("Desbordamiento aritmético al multiplicar montos monetarios ($a * $b).")
            }
            return result
        }
    }
}
