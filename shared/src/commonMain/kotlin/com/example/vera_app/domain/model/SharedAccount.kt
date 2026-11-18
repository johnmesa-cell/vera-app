package com.example.vera_app.domain.model

/**
 * Representa la administración financiera compartida de la familia.
 */
data class SharedAccount(
    val totalMoney: Money = Money.ZERO,
    val reservedSavings: Money = Money.ZERO,
) {
    init {
        require(totalMoney >= reservedSavings) {
            "El saldo total ($totalMoney) no puede ser inferior a las reservas de ahorro ($reservedSavings)."
        }
    }

    /**
     * Saldo disponible para gastos, transferencias o nuevas reservas de ahorro.
     */
    val availableMoney: Money
        get() = totalMoney - reservedSavings

    fun depositIncome(amount: Money): SharedAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        return copy(totalMoney = totalMoney + amount)
    }

    fun withdrawExpense(amount: Money): SharedAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        if (amount > availableMoney) {
            throw InsufficientBalanceException(amount, availableMoney, "Gasto familiar excede el saldo disponible.")
        }
        return copy(totalMoney = totalMoney - amount)
    }

    fun transferOut(amount: Money): SharedAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        if (amount > availableMoney) {
            throw InsufficientBalanceException(amount, availableMoney, "La asignación excede el saldo disponible compartido.")
        }
        return copy(totalMoney = totalMoney - amount)
    }

    fun transferIn(amount: Money): SharedAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        return copy(totalMoney = totalMoney + amount)
    }

    fun reserveSavings(amount: Money): SharedAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        if (amount > availableMoney) {
            throw InsufficientBalanceException(amount, availableMoney, "Reserva de ahorro familiar excede el saldo disponible.")
        }
        return copy(reservedSavings = reservedSavings + amount)
    }

    fun releaseSavings(amount: Money): SharedAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        if (amount > reservedSavings) {
            throw InsufficientSavingsException(amount, reservedSavings, "No hay suficiente dinero reservado para liberar.")
        }
        return copy(reservedSavings = reservedSavings - amount)
    }
}
