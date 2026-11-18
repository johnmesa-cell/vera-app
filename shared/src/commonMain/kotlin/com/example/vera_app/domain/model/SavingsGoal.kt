package com.example.vera_app.domain.model

/**
 * Representa un objetivo y reserva de ahorro familiar o personal.
 */
data class SavingsGoal(
    val id: String,
    val name: String,
    val targetAmount: Money,
    val currentAmount: Money = Money.ZERO,
    val accountRef: AccountReference,
) {
    init {
        require(id.isNotBlank()) { "El ID del objetivo de ahorro no puede estar vacío." }
        require(name.isNotBlank()) { "El nombre del objetivo de ahorro no puede estar vacío." }
        require(targetAmount.isPositive()) { "El monto objetivo del ahorro debe ser estrictamente mayor a cero." }
        require(currentAmount >= Money.ZERO) { "El monto actualmente reservado no puede ser negativo." }
        require(currentAmount <= targetAmount) {
            "El monto reservado ($currentAmount) no puede superar el monto objetivo ($targetAmount)."
        }
    }

    /**
     * Deposita una cantidad reservada en esta meta de ahorro.
     */
    fun deposit(amount: Money): SavingsGoal {
        if (!amount.isPositive()) {
            throw InvalidAmountException(amount)
        }
        val newAmount = currentAmount + amount
        if (newAmount > targetAmount) {
            throw DomainException("El depósito excede el monto objetivo fijado ($targetAmount) para la meta '$name'.")
        }
        return copy(currentAmount = newAmount)
    }

    /**
     * Libera una cantidad reservada en esta meta de ahorro.
     */
    fun withdraw(amount: Money): SavingsGoal {
        if (!amount.isPositive()) {
            throw InvalidAmountException(amount)
        }
        if (amount > currentAmount) {
            throw InsufficientSavingsException(amount, currentAmount)
        }
        return copy(currentAmount = currentAmount - amount)
    }
}
