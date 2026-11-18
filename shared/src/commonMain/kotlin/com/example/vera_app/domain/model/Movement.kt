package com.example.vera_app.domain.model

/**
 * Representa un movimiento financiero en Vera.
 */
data class Movement(
    val id: String,
    val type: MovementType,
    val amount: Money,
    val description: String,
    val categoryId: String,
    val sourceAccountRef: AccountReference? = null,
    val destinationAccountRef: AccountReference? = null,
    val savingsGoalId: String? = null,
    val timestamp: Long = 0L,
) {
    init {
        require(id.isNotBlank()) { "El ID del movimiento no puede estar vacío." }
        if (!amount.isPositive()) {
            throw InvalidAmountException(amount, "El importe del movimiento debe ser estrictamente mayor a cero.")
        }

        when (type) {
            MovementType.INCOME -> {
                require(destinationAccountRef != null) { "Un movimiento de ingreso debe especificar la cuenta de destino." }
            }
            MovementType.EXPENSE -> {
                require(sourceAccountRef != null) { "Un movimiento de gasto debe especificar la cuenta de origen." }
            }
            MovementType.TRANSFER -> {
                require((sourceAccountRef != null) && (destinationAccountRef != null)) {
                    "Una transferencia debe especificar tanto la cuenta de origen como la de destino."
                }
                require(sourceAccountRef != destinationAccountRef) {
                    "La cuenta de origen y destino en una transferencia deben ser diferentes."
                }
            }
            MovementType.SAVINGS_DEPOSIT, MovementType.SAVINGS_WITHDRAWAL -> {
                require(!savingsGoalId.isNullOrBlank()) {
                    "Un movimiento de ahorro debe estar vinculado a un ID de objetivo de ahorro."
                }
                require((sourceAccountRef != null) || (destinationAccountRef != null)) {
                    "Un movimiento de ahorro debe especificar la cuenta asociada."
                }
            }
        }
    }
}
