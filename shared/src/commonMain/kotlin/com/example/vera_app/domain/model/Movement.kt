package com.example.vera_app.domain.model

/**
 * Representa un movimiento financiero inmutable en Vera.
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
        require(description.isNotBlank()) { "La descripción del movimiento no puede estar vacía." }
        require(categoryId.isNotBlank()) { "El ID de categoría del movimiento no puede estar vacío." }

        if (!amount.isPositive()) {
            throw InvalidAmountException(amount, "El importe del movimiento debe ser estrictamente mayor a cero.")
        }

        when (type) {
            MovementType.INCOME -> {
                require(destinationAccountRef != null) { "Un movimiento de ingreso debe especificar la cuenta de destino." }
                require(sourceAccountRef == null) { "Un movimiento de ingreso no debe especificar cuenta de origen." }
                require(savingsGoalId == null) { "Un movimiento de ingreso no debe asociarse a una meta de ahorro." }
            }
            MovementType.EXPENSE -> {
                require(sourceAccountRef != null) { "Un movimiento de gasto debe especificar la cuenta de origen." }
                require(destinationAccountRef == null) { "Un movimiento de gasto no debe especificar cuenta de destino." }
                require(savingsGoalId == null) { "Un movimiento de gasto no debe asociarse a una meta de ahorro." }
            }
            MovementType.TRANSFER -> {
                require((sourceAccountRef != null) && (destinationAccountRef != null)) {
                    "Una transferencia debe especificar tanto la cuenta de origen como la de destino."
                }
                require(sourceAccountRef != destinationAccountRef) {
                    "La cuenta de origen y destino en una transferencia deben ser diferentes."
                }
                require(savingsGoalId == null) { "Una transferencia no debe asociarse a una meta de ahorro." }
            }
            MovementType.SAVINGS_DEPOSIT -> {
                require(!savingsGoalId.isNullOrBlank()) {
                    "Un depósito de ahorro debe estar vinculado a un ID de objetivo de ahorro."
                }
                require(sourceAccountRef != null) { "Un depósito de ahorro debe especificar la cuenta de origen." }
                require(destinationAccountRef == null) { "Un depósito de ahorro no debe especificar cuenta de destino." }
            }
            MovementType.SAVINGS_WITHDRAWAL -> {
                require(!savingsGoalId.isNullOrBlank()) {
                    "Un retiro de ahorro debe estar vinculado a un ID de objetivo de ahorro."
                }
                require(destinationAccountRef != null) { "Un retiro de ahorro debe especificar la cuenta de destino." }
                require(sourceAccountRef == null) { "Un retiro de ahorro no debe especificar cuenta de origen." }
            }
        }
    }
}
