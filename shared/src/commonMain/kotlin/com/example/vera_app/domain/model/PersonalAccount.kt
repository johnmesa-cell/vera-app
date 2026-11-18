package com.example.vera_app.domain.model

/**
 * Representa la administración financiera individual de un integrante de la familia.
 */
data class PersonalAccount(
    val memberId: String,
    val totalMoney: Money = Money.ZERO,
    val reservedSavings: Money = Money.ZERO,
) {
    init {
        require(memberId.isNotBlank()) { "El memberId de la cuenta personal no puede estar vacío." }
        require(totalMoney >= reservedSavings) {
            "El dinero total personal ($totalMoney) no puede ser menor al ahorro reservado ($reservedSavings)."
        }
    }

    /**
     * Dinero disponible para gastos personales, transferencias o reservas de ahorro.
     */
    val availableMoney: Money
        get() = totalMoney - reservedSavings

    fun receiveAllocation(amount: Money): PersonalAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        return copy(totalMoney = totalMoney + amount)
    }

    fun depositPersonalIncome(amount: Money): PersonalAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        return copy(totalMoney = totalMoney + amount)
    }

    fun withdrawPersonalExpense(amount: Money): PersonalAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        if (amount > availableMoney) {
            throw InsufficientBalanceException(amount, availableMoney, "El gasto personal excede el saldo disponible.")
        }
        return copy(totalMoney = totalMoney - amount)
    }

    fun transferToShared(amount: Money): PersonalAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        if (amount > availableMoney) {
            throw InsufficientBalanceException(amount, availableMoney, "La transferencia a la cuenta compartida excede el saldo disponible.")
        }
        return copy(totalMoney = totalMoney - amount)
    }

    fun reserveSavings(amount: Money): PersonalAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        if (amount > availableMoney) {
            throw InsufficientBalanceException(amount, availableMoney, "La reserva de ahorro personal excede el saldo disponible.")
        }
        return copy(reservedSavings = reservedSavings + amount)
    }

    fun releaseSavings(amount: Money): PersonalAccount {
        if (!amount.isPositive()) throw InvalidAmountException(amount)
        if (amount > reservedSavings) {
            throw InsufficientSavingsException(amount, reservedSavings, "No hay suficiente ahorro personal reservado para liberar.")
        }
        return copy(reservedSavings = reservedSavings - amount)
    }
}
