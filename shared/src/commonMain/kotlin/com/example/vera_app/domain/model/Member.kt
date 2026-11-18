package com.example.vera_app.domain.model

/**
 * Representa a un integrante de la familia y su cuenta personal asociada.
 */
data class Member(
    val id: String,
    val name: String,
    val role: MemberRole = MemberRole.PARENT,
    val personalAccount: PersonalAccount = PersonalAccount(memberId = id),
) {
    init {
        require(id.isNotBlank()) { "El ID del integrante no puede estar vacío." }
        require(name.isNotBlank()) { "El nombre del integrante no puede estar vacío." }
        require(personalAccount.memberId == id) {
            "La cuenta personal debe estar vinculada al ID del integrante ($id)."
        }
    }
}
