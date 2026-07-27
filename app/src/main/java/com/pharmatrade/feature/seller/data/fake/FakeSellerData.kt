package com.pharmatrade.feature.seller.data.fake

import com.pharmatrade.core.common.model.*

object FakeSellerData {

    val sellers = mutableListOf(
        Seller(
            id = "s001",
            userId = "seller_001",
            businessName = "MedPharm Distribution",
            ownerName = "Ahmed Hassan",
            phone = "01012345678",
            location = "Cairo, Nasr City",
            rating = 4.8f,
            minimumOrderAmount = 500.0,
            totalSales = 1240,
            isVerified = true,
            licenseNumber = "LIC-2024-001"
        ),
        Seller(
            id = "s002",
            userId = "seller_002",
            businessName = "Cairo Drug Supplies",
            ownerName = "Mohamed Ali",
            phone = "01123456789",
            location = "Cairo, Heliopolis",
            rating = 4.5f,
            minimumOrderAmount = 300.0,
            totalSales = 876,
            isVerified = true,
            licenseNumber = "LIC-2024-002"
        ),
        Seller(
            id = "s003",
            userId = "seller_003",
            businessName = "Delta Pharma Agency",
            ownerName = "Khaled Ibrahim",
            phone = "01234567890",
            location = "Giza, Mohandessin",
            rating = 4.2f,
            minimumOrderAmount = 400.0,
            totalSales = 534,
            isVerified = true,
            licenseNumber = "LIC-2024-003"
        ),
        Seller(
            id = "s004",
            userId = "seller_004",
            businessName = "Nile Medical Corp",
            ownerName = "Amr Saad",
            phone = "01098765432",
            location = "Alexandria, Sporting",
            rating = 4.6f,
            minimumOrderAmount = 600.0,
            totalSales = 2100,
            isVerified = true,
            licenseNumber = "LIC-2024-004"
        ),
        Seller(
            id = "s005",
            userId = "seller_005",
            businessName = "Alexandria Drugs Ltd",
            ownerName = "Tarek Mahmoud",
            phone = "01187654321",
            location = "Alexandria, Miami",
            rating = 3.9f,
            minimumOrderAmount = 250.0,
            totalSales = 312,
            isVerified = false,
            licenseNumber = "LIC-2024-005"
        )
    )

    val drugs = listOf(
        Drug("d001", "Paracetamol 500mg", "Acetaminophen", DrugCategory.ANALGESIC, "EVA Pharma", "Pain reliever and fever reducer", "Tablet", "500mg"),
        Drug("d002", "Amoxicillin 500mg", "Amoxicillin Trihydrate", DrugCategory.ANTIBIOTIC, "Pfizer Egypt", "Broad-spectrum penicillin antibiotic", "Capsule", "500mg"),
        Drug("d003", "Ibuprofen 400mg", "Ibuprofen", DrugCategory.ANALGESIC, "Kahira Pharm", "NSAID for pain, fever, and inflammation", "Tablet", "400mg"),
        Drug("d004", "Omeprazole 20mg", "Omeprazole", DrugCategory.DIGESTIVE, "EIPICO", "Proton pump inhibitor for stomach acid", "Capsule", "20mg"),
        Drug("d005", "Metformin 500mg", "Metformin HCl", DrugCategory.DIABETES, "Hikma", "First-line oral diabetes medication", "Tablet", "500mg"),
        Drug("d006", "Atorvastatin 10mg", "Atorvastatin Calcium", DrugCategory.CARDIOVASCULAR, "Lipitor Egypt", "Statin for cholesterol management", "Tablet", "10mg"),
        Drug("d007", "Aspirin 100mg", "Acetylsalicylic Acid", DrugCategory.CARDIOVASCULAR, "Bayer Egypt", "Antiplatelet agent for cardiac protection", "Tablet", "100mg"),
        Drug("d008", "Cetirizine 10mg", "Cetirizine HCl", DrugCategory.RESPIRATORY, "UCB Pharma", "Antihistamine for allergies", "Tablet", "10mg"),
        Drug("d009", "Losartan 50mg", "Losartan Potassium", DrugCategory.CARDIOVASCULAR, "MSD Egypt", "ARB for hypertension", "Tablet", "50mg"),
        Drug("d010", "Pantoprazole 40mg", "Pantoprazole Sodium", DrugCategory.DIGESTIVE, "Nycomed", "PPI for GERD and peptic ulcers", "Tablet", "40mg"),
        Drug("d011", "Azithromycin 500mg", "Azithromycin", DrugCategory.ANTIBIOTIC, "Pfizer Egypt", "Macrolide antibiotic", "Tablet", "500mg"),
        Drug("d012", "Vitamin D3 1000IU", "Cholecalciferol", DrugCategory.VITAMINS, "Pharco", "Vitamin D supplementation", "Capsule", "1000IU")
    )

    val listings = mutableListOf(
        // s001 - MedPharm listings
        SellerListing("l001", drugs[0], sellers[0], 25.0, 10.0, 500, "Box/24tabs", "2026-12-31"),
        SellerListing("l002", drugs[1], sellers[0], 85.0, 15.0, 200, "Box/12caps", "2026-08-31"),
        SellerListing("l003", drugs[3], sellers[0], 45.0, 5.0, 350, "Box/28caps", "2027-01-31"),
        SellerListing("l004", drugs[5], sellers[0], 120.0, 20.0, 150, "Box/30tabs", "2026-10-31"),
        SellerListing("l005", drugs[11], sellers[0], 60.0, 0.0, 300, "Box/30caps", "2026-11-30"),

        // s002 - Cairo Drug Supplies listings
        SellerListing("l006", drugs[2], sellers[1], 30.0, 8.0, 400, "Box/30tabs", "2026-09-30"),
        SellerListing("l007", drugs[4], sellers[1], 18.0, 5.0, 600, "Box/30tabs", "2027-03-31"),
        SellerListing("l008", drugs[6], sellers[1], 22.0, 0.0, 250, "Box/30tabs", "2026-12-31"),
        SellerListing("l009", drugs[7], sellers[1], 35.0, 12.0, 180, "Box/20tabs", "2026-07-31"),
        SellerListing("l010", drugs[8], sellers[1], 75.0, 10.0, 120, "Box/28tabs", "2026-11-30"),

        // s003 - Delta Pharma listings
        SellerListing("l011", drugs[0], sellers[2], 22.0, 5.0, 800, "Box/24tabs", "2026-12-31"),
        SellerListing("l012", drugs[9], sellers[2], 55.0, 15.0, 200, "Box/28tabs", "2027-02-28"),
        SellerListing("l013", drugs[10], sellers[2], 95.0, 10.0, 100, "Box/3tabs", "2026-06-30"),
        SellerListing("l014", drugs[5], sellers[2], 115.0, 18.0, 90, "Box/30tabs", "2026-10-31"),

        // s004 - Nile Medical listings
        SellerListing("l015", drugs[1], sellers[3], 90.0, 20.0, 300, "Box/12caps", "2027-01-31"),
        SellerListing("l016", drugs[3], sellers[3], 48.0, 8.0, 250, "Box/28caps", "2026-09-30"),
        SellerListing("l017", drugs[4], sellers[3], 20.0, 10.0, 500, "Box/30tabs", "2027-03-31"),
        SellerListing("l018", drugs[11], sellers[3], 65.0, 5.0, 400, "Box/30caps", "2026-12-31"),

        // s005 - Alexandria Drugs listings
        SellerListing("l019", drugs[2], sellers[4], 28.0, 0.0, 300, "Box/30tabs", "2026-08-31"),
        SellerListing("l020", drugs[6], sellers[4], 20.0, 5.0, 200, "Box/30tabs", "2026-11-30"),
        SellerListing("l021", drugs[7], sellers[4], 32.0, 8.0, 150, "Box/20tabs", "2026-10-31")
    )

    fun getSellerById(id: String) = sellers.find { it.id == id }
    fun getListingsBySellerIdl(sellerId: String) = listings.filter { it.seller.id == sellerId }
    fun getListingById(id: String) = listings.find { it.id == id }
    fun generateListingId() = "l_${System.currentTimeMillis()}"
}
