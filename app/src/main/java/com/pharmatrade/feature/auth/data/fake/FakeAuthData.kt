package com.pharmatrade.feature.auth.data.fake

import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType

object FakeAuthData {
    private val users = mutableListOf(
        User(
            id = "admin_001",
            name = "Admin",
            email = "admin@pharma.com",
            phone = "01000000000",
            userType = UserType.ADMIN,
            businessName = "PharmaTrade Admin"
        ),
        User(
            id = "seller_001",
            name = "Ahmed Hassan",
            email = "seller@pharma.com",
            phone = "01012345678",
            userType = UserType.SELLER,
            businessName = "MedPharm Distribution",
            sellerId = "s001"
        ),
        User(
            id = "buyer_001",
            name = "Dr. Sara Mahmoud",
            email = "pharmacy@pharma.com",
            phone = "01098765432",
            userType = UserType.BUYER,
            businessName = "Al-Shifaa Pharmacy"
        ),
        User(
            id = "buyer_pending_001",
            name = "Pending Pharmacy",
            email = "pending@pharma.com",
            phone = "01011111111",
            userType = UserType.BUYER,
            status = "pending",
            businessName = "New Pharmacy"
        )
    )

    fun findByEmail(email: String): User? = users.find { it.email == email }
    fun findByPhone(phone: String): User? = users.find { it.phone == phone }

    fun addUser(user: User) = users.add(user)

    fun generateUserId() = "user_${System.currentTimeMillis()}"
}
