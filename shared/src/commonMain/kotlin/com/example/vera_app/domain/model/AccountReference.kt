package com.example.vera_app.domain.model

/**
 * Tipo de cuenta en la administración financiera Vera.
 */
enum class AccountType {
    SHARED,
    PERSONAL,
}

/**
 * Referencia inmutable a una cuenta financiera (compartida o personal).
 */
data class AccountReference(
    val type: AccountType,
    val memberId: String? = null,
) {
    init {
        if (type == AccountType.PERSONAL) {
            require(!memberId.isNullOrBlank()) { "Una cuenta personal debe estar vinculada al ID de un integrante." }
        } else {
            require(memberId == null) { "La cuenta compartida no debe tener un memberId asociado." }
        }
    }

    companion object {
        val SHARED = AccountReference(AccountType.SHARED)

        fun personal(memberId: String): AccountReference = AccountReference(AccountType.PERSONAL, memberId)
    }
}
