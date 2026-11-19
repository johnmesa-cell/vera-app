package com.example.vera_app.domain.model

/**
 * Excepción base para violaciones de reglas de negocio en el dominio financiero Vera.
 */
open class DomainException(message: String) : Exception(message)

/**
 * Excepción lanzada cuando el saldo disponible es insuficiente para realizar una operación.
 */
class InsufficientBalanceException(
    val requested: Money,
    val available: Money,
    message: String = "Saldo disponible insuficiente. Requerido: $requested, Disponible: $available",
) : DomainException(message)

/**
 * Excepción lanzada cuando se intenta registrar un monto inválido.
 */
class InvalidAmountException(
    val amount: Money,
    message: String = "El monto debe ser estrictamente mayor a cero. Valor ingresado: $amount",
) : DomainException(message)

/**
 * Excepción lanzada cuando se intenta retirar más ahorro del reservado.
 */
class InsufficientSavingsException(
    val requested: Money,
    val currentReserved: Money,
    message: String = "No se puede retirar más ahorro del reservado. Solicitado: $requested, Reservado: $currentReserved",
) : DomainException(message)

/**
 * Excepción lanzada cuando no se encuentra una familia solicitada.
 */
class FamilyNotFoundException(
    val familyId: String,
    message: String = "No se encontró la familia con ID: $familyId",
) : DomainException(message)

/**
 * Excepción lanzada cuando se intenta crear una familia que ya existe.
 */
class FamilyAlreadyExistsException(
    val familyId: String,
    message: String = "Ya existe una familia registrada con el ID: $familyId",
) : DomainException(message)
