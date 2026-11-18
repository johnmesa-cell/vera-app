package com.example.vera_app.domain.model

/**
 * Entidad raíz (Aggregate Root) que representa la familia, su cuenta compartida,
 * sus integrantes, metas de ahorro e historial de movimientos.
 */
data class Family(
    val id: String,
    val name: String,
    val sharedAccount: SharedAccount = SharedAccount(),
    val members: List<Member> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val movements: List<Movement> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "El ID de la familia no puede estar vacío." }
        require(name.isNotBlank()) { "El nombre de la familia no puede estar vacío." }

        // 1. No existen integrantes con IDs duplicados
        require(members.map { it.id }.distinct().size == members.size) {
            "Existen integrantes con IDs duplicados en la familia."
        }

        // 2. No existen metas de ahorro con IDs duplicados
        require(savingsGoals.map { it.id }.distinct().size == savingsGoals.size) {
            "Existen metas de ahorro con IDs duplicados en la familia."
        }

        // 3. No existen movimientos con IDs duplicados
        require(movements.map { it.id }.distinct().size == movements.size) {
            "Existen movimientos con IDs duplicados en la familia."
        }

        // 4 & 5. Las referencias de las metas corresponden a cuentas válidas dentro de la familia
        savingsGoals.forEach { goal ->
            if (goal.accountRef.type == AccountType.PERSONAL) {
                val memberId = goal.accountRef.memberId
                require(!memberId.isNullOrBlank() && members.any { it.id == memberId }) {
                    "La meta de ahorro '${goal.name}' está vinculada a un integrante inexistente o inválido (ID: $memberId)."
                }
            } else {
                require(goal.accountRef.memberId == null) {
                    "La meta de ahorro familiar '${goal.name}' no debe tener un memberId asociado."
                }
            }
        }

        // 6. La suma de las reservas de las metas de la cuenta compartida coincide con sharedAccount.reservedSavings
        val sharedGoalsReserved = savingsGoals
            .filter { it.accountRef == AccountReference.SHARED }
            .fold(Money.ZERO) { acc, goal -> acc + goal.currentAmount }

        require(sharedAccount.reservedSavings == sharedGoalsReserved) {
            "El saldo reservado de la cuenta compartida (${sharedAccount.reservedSavings}) no coincide con la suma de sus metas de ahorro ($sharedGoalsReserved)."
        }

        // 6. La suma de las reservas de las metas de cada cuenta personal coincide con personalAccount.reservedSavings
        members.forEach { member ->
            val memberGoalsReserved = savingsGoals
                .filter { it.accountRef == AccountReference.personal(member.id) }
                .fold(Money.ZERO) { acc, goal -> acc + goal.currentAmount }

            require(member.personalAccount.reservedSavings == memberGoalsReserved) {
                "El saldo reservado de la cuenta personal del integrante ${member.name} (${member.personalAccount.reservedSavings}) no coincide con la suma de sus metas de ahorro ($memberGoalsReserved)."
            }
        }
    }

    // --- Métricas consolidadas ---

    /** Dinero total de la familia (cuenta compartida + cuentas personales de los integrantes) */
    val totalFamilyMoney: Money
        get() = sharedAccount.totalMoney + members.fold(Money.ZERO) { acc, m -> acc + m.personalAccount.totalMoney }

    /** Dinero disponible total de la familia para nuevos gastos o reservas */
    val totalFamilyAvailableMoney: Money
        get() = sharedAccount.availableMoney + members.fold(Money.ZERO) { acc, m -> acc + m.personalAccount.availableMoney }

    /** Dinero total reservado para ahorros (familiares y personales) */
    val totalFamilyReservedSavings: Money
        get() = sharedAccount.reservedSavings + members.fold(Money.ZERO) { acc, m -> acc + m.personalAccount.reservedSavings }

    /** Dinero total acumulado/distribuido actualmente en cuentas personales */
    val totalAllocatedToMembers: Money
        get() = members.fold(Money.ZERO) { acc, m -> acc + m.personalAccount.totalMoney }

    // --- Consultas relacionales ---

    fun getMember(memberId: String): Member? = members.find { it.id == memberId }

    fun getSavingsGoal(goalId: String): SavingsGoal? = savingsGoals.find { it.id == goalId }

    fun getMovementsForSharedAccount(): List<Movement> = movements.filter {
        (it.sourceAccountRef == AccountReference.SHARED) || (it.destinationAccountRef == AccountReference.SHARED)
    }

    fun getMovementsForMember(memberId: String): List<Movement> = movements.filter {
        (it.sourceAccountRef?.memberId == memberId) || (it.destinationAccountRef?.memberId == memberId)
    }

    // --- Operaciones de dominio ---

    /**
     * Agrega un nuevo integrante a la familia.
     */
    fun addMember(member: Member): Family {
        require(members.none { it.id == member.id }) { "Ya existe un integrante con el ID ${member.id}" }
        return copy(members = members + member)
    }

    /**
     * Registra un ingreso de dinero en la cuenta compartida familiar.
     */
    fun recordSharedIncome(movement: Movement): Family {
        validateUniqueMovementId(movement.id)
        require(movement.type == MovementType.INCOME) { "El movimiento debe ser de tipo INCOME." }
        require(movement.destinationAccountRef == AccountReference.SHARED) { "El destino debe ser la cuenta compartida." }

        val updatedAccount = sharedAccount.depositIncome(movement.amount)
        return copy(
            sharedAccount = updatedAccount,
            movements = movements + movement,
        )
    }

    /**
     * Registra un gasto en la cuenta compartida familiar.
     */
    fun recordSharedExpense(movement: Movement): Family {
        validateUniqueMovementId(movement.id)
        require(movement.type == MovementType.EXPENSE) { "El movimiento debe ser de tipo EXPENSE." }
        require(movement.sourceAccountRef == AccountReference.SHARED) { "El origen debe ser la cuenta compartida." }

        val updatedAccount = sharedAccount.withdrawExpense(movement.amount)
        return copy(
            sharedAccount = updatedAccount,
            movements = movements + movement,
        )
    }

    /**
     * Asigna (transfiere) dinero desde la cuenta compartida hacia la cuenta personal de un integrante.
     */
    fun allocateToMember(
        memberId: String,
        amount: Money,
        description: String,
        categoryId: String,
        movementId: String,
        timestamp: Long = 0L,
    ): Family {
        validateUniqueMovementId(movementId)
        val member = getMember(memberId)
            ?: throw DomainException("No se encontró el integrante con ID $memberId")

        val updatedSharedAccount = sharedAccount.transferOut(amount)
        val updatedPersonalAccount = member.personalAccount.receiveAllocation(amount)
        val updatedMember = member.copy(personalAccount = updatedPersonalAccount)

        val movement = Movement(
            id = movementId,
            type = MovementType.TRANSFER,
            amount = amount,
            description = description,
            categoryId = categoryId,
            sourceAccountRef = AccountReference.SHARED,
            destinationAccountRef = AccountReference.personal(memberId),
            timestamp = timestamp,
        )

        val updatedMembers = members.map { if (it.id == memberId) updatedMember else it }
        return copy(
            sharedAccount = updatedSharedAccount,
            members = updatedMembers,
            movements = movements + movement,
        )
    }

    /**
     * Devuelve (transfiere) dinero desde la cuenta personal de un integrante hacia la cuenta compartida familiar.
     */
    fun returnFromMember(
        memberId: String,
        amount: Money,
        description: String,
        categoryId: String,
        movementId: String,
        timestamp: Long = 0L,
    ): Family {
        validateUniqueMovementId(movementId)
        val member = getMember(memberId)
            ?: throw DomainException("No se encontró el integrante con ID $memberId")

        val updatedPersonalAccount = member.personalAccount.transferToShared(amount)
        val updatedSharedAccount = sharedAccount.transferIn(amount)
        val updatedMember = member.copy(personalAccount = updatedPersonalAccount)

        val movement = Movement(
            id = movementId,
            type = MovementType.TRANSFER,
            amount = amount,
            description = description,
            categoryId = categoryId,
            sourceAccountRef = AccountReference.personal(memberId),
            destinationAccountRef = AccountReference.SHARED,
            timestamp = timestamp,
        )

        val updatedMembers = members.map { if (it.id == memberId) updatedMember else it }
        return copy(
            sharedAccount = updatedSharedAccount,
            members = updatedMembers,
            movements = movements + movement,
        )
    }

    /**
     * Registra un ingreso personal directo de un integrante.
     */
    fun recordPersonalIncome(memberId: String, movement: Movement): Family {
        validateUniqueMovementId(movement.id)
        require(movement.type == MovementType.INCOME) { "El movimiento debe ser de tipo INCOME." }
        require(movement.destinationAccountRef == AccountReference.personal(memberId)) {
            "El destino debe ser la cuenta personal del integrante $memberId."
        }

        val member = getMember(memberId)
            ?: throw DomainException("No se encontró el integrante con ID $memberId")

        val updatedPersonalAccount = member.personalAccount.depositPersonalIncome(movement.amount)
        val updatedMember = member.copy(personalAccount = updatedPersonalAccount)
        val updatedMembers = members.map { if (it.id == memberId) updatedMember else it }

        return copy(
            members = updatedMembers,
            movements = movements + movement,
        )
    }

    /**
     * Registra un gasto personal de un integrante.
     */
    fun recordPersonalExpense(memberId: String, movement: Movement): Family {
        validateUniqueMovementId(movement.id)
        require(movement.type == MovementType.EXPENSE) { "El movimiento debe ser de tipo EXPENSE." }
        require(movement.sourceAccountRef == AccountReference.personal(memberId)) {
            "El origen debe ser la cuenta personal del integrante $memberId."
        }

        val member = getMember(memberId)
            ?: throw DomainException("No se encontró el integrante con ID $memberId")

        val updatedPersonalAccount = member.personalAccount.withdrawPersonalExpense(movement.amount)
        val updatedMember = member.copy(personalAccount = updatedPersonalAccount)
        val updatedMembers = members.map { if (it.id == memberId) updatedMember else it }

        return copy(
            members = updatedMembers,
            movements = movements + movement,
        )
    }

    /**
     * Agrega un nuevo objetivo de ahorro a la familia (familiar o personal).
     */
    fun addSavingsGoal(savingsGoal: SavingsGoal): Family {
        require(savingsGoals.none { it.id == savingsGoal.id }) { "Ya existe una meta de ahorro con ID ${savingsGoal.id}" }
        require(savingsGoal.currentAmount == Money.ZERO) {
            "Un nuevo objetivo de ahorro debe iniciarse con saldo reservado cero. Utilice depositToSavingsGoal() para reservar fondos."
        }
        if (savingsGoal.accountRef.type == AccountType.PERSONAL) {
            val memberId = savingsGoal.accountRef.memberId
            require(members.any { it.id == memberId }) { "No existe el integrante con ID $memberId para esta meta de ahorro personal." }
        }
        return copy(savingsGoals = savingsGoals + savingsGoal)
    }

    /**
     * Reserva dinero hacia un objetivo de ahorro (familiar o personal).
     */
    fun depositToSavingsGoal(
        savingsGoalId: String,
        amount: Money,
        movementId: String,
        timestamp: Long = 0L,
    ): Family {
        validateUniqueMovementId(movementId)
        val goal = getSavingsGoal(savingsGoalId)
            ?: throw DomainException("No se encontró el objetivo de ahorro con ID $savingsGoalId")

        val updatedGoal = goal.deposit(amount)
        val updatedGoals = savingsGoals.map { if (it.id == savingsGoalId) updatedGoal else it }

        val movement = Movement(
            id = movementId,
            type = MovementType.SAVINGS_DEPOSIT,
            amount = amount,
            description = "Reserva para ahorro: ${goal.name}",
            categoryId = Category.SYSTEM_SAVINGS.id,
            sourceAccountRef = goal.accountRef,
            savingsGoalId = savingsGoalId,
            timestamp = timestamp,
        )

        return if (goal.accountRef.type == AccountType.SHARED) {
            val updatedShared = sharedAccount.reserveSavings(amount)
            copy(
                sharedAccount = updatedShared,
                savingsGoals = updatedGoals,
                movements = movements + movement,
            )
        } else {
            val memberId = goal.accountRef.memberId!!
            val member = getMember(memberId)!!
            val updatedPersonal = member.personalAccount.reserveSavings(amount)
            val updatedMember = member.copy(personalAccount = updatedPersonal)
            val updatedMembers = members.map { if (it.id == memberId) updatedMember else it }
            copy(
                members = updatedMembers,
                savingsGoals = updatedGoals,
                movements = movements + movement,
            )
        }
    }

    /**
     * Libera dinero desde un objetivo de ahorro de vuelta al saldo disponible.
     */
    fun withdrawFromSavingsGoal(
        savingsGoalId: String,
        amount: Money,
        movementId: String,
        timestamp: Long = 0L,
    ): Family {
        validateUniqueMovementId(movementId)
        val goal = getSavingsGoal(savingsGoalId)
            ?: throw DomainException("No se encontró el objetivo de ahorro con ID $savingsGoalId")

        val updatedGoal = goal.withdraw(amount)
        val updatedGoals = savingsGoals.map { if (it.id == savingsGoalId) updatedGoal else it }

        val movement = Movement(
            id = movementId,
            type = MovementType.SAVINGS_WITHDRAWAL,
            amount = amount,
            description = "Liberación de ahorro: ${goal.name}",
            categoryId = Category.SYSTEM_SAVINGS.id,
            destinationAccountRef = goal.accountRef,
            savingsGoalId = savingsGoalId,
            timestamp = timestamp,
        )

        return if (goal.accountRef.type == AccountType.SHARED) {
            val updatedShared = sharedAccount.releaseSavings(amount)
            copy(
                sharedAccount = updatedShared,
                savingsGoals = updatedGoals,
                movements = movements + movement,
            )
        } else {
            val memberId = goal.accountRef.memberId!!
            val member = getMember(memberId)!!
            val updatedPersonal = member.personalAccount.releaseSavings(amount)
            val updatedMember = member.copy(personalAccount = updatedPersonal)
            val updatedMembers = members.map { if (it.id == memberId) updatedMember else it }
            copy(
                members = updatedMembers,
                savingsGoals = updatedGoals,
                movements = movements + movement,
            )
        }
    }

    private fun validateUniqueMovementId(movementId: String) {
        require(movements.none { it.id == movementId }) {
            "Ya existe un movimiento registrado con el ID '$movementId'."
        }
    }
}
