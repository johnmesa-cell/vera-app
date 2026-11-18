package com.example.vera_app.domain.model

/**
 * Categoría para clasificar los movimientos financieros.
 */
data class Category(
    val id: String,
    val name: String,
    val icon: String? = null,
    val isSystemCategory: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "El ID de la categoría no puede estar vacío." }
        require(name.isNotBlank()) { "El nombre de la categoría no puede estar vacío." }
    }

    companion object {
        val SYSTEM_INCOME = Category("cat_income", "Ingreso General", isSystemCategory = true)
        val SYSTEM_ALLOCATION = Category("cat_allocation", "Asignación Familiar", isSystemCategory = true)
        val SYSTEM_EXPENSE = Category("cat_expense", "Gasto General", isSystemCategory = true)
        val SYSTEM_SAVINGS = Category("cat_savings", "Reserva de Ahorro", isSystemCategory = true)
    }
}
