package com.example.data.model

enum class BiodataType {
    MARRIAGE,
    JOB
}

data class MarriageBiodata(
    val id: String = "",
    val templateId: String = "m_royal_gold_1",
    val profilePhotoUri: String? = null,
    
    // Personal Details
    val fullName: String = "",
    val gender: String = "Male",
    val dateOfBirth: String = "",
    val birthTime: String = "",
    val birthPlace: String = "",
    val rashi: String = "",
    val nakshatra: String = "",
    val gotra: String = "",
    val manglik: String = "No",
    val religion: String = "Hindu",
    val caste: String = "",
    val subCaste: String = "",
    val height: String = "",
    val weight: String = "",
    val complexion: String = "",
    val bloodGroup: String = "",
    val education: String = "",
    val occupation: String = "",
    val annualIncome: String = "",
    val aboutMe: String = "",
    
    // Family Details
    val fatherName: String = "",
    val fatherOccupation: String = "",
    val motherName: String = "",
    val motherOccupation: String = "",
    val brothers: String = "",
    val sisters: String = "",
    val familyType: String = "Nuclear",
    val familyValues: String = "Traditional",
    val nativePlace: String = "",
    val familyStatus: String = "Middle Class",
    
    // Contact Details
    val mobile: String = "",
    val alternateMobile: String = "",
    val email: String = "",
    val residentialAddress: String = "",
    val showContactOnPdf: Boolean = true,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class JobBiodata(
    val id: String = "",
    val templateId: String = "j_executive_blue_1",
    val profilePhotoUri: String? = null,
    
    // Personal Information
    val fullName: String = "",
    val targetRole: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val portfolioOrLinkedin: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val nationality: String = "Indian",
    
    // Career
    val careerObjective: String = "",
    val workExperience: List<ExperienceItem> = emptyList(),
    
    // Education
    val postGraduation: String = "",
    val graduation: String = "",
    val intermediate: String = "",
    val highSchool: String = "",
    val otherQualifications: String = "",
    
    // Additional Information
    val languages: String = "",
    val skills: String = "",
    val hobbies: String = "",
    val certifications: String = "",
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ExperienceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val duration: String = "",
    val responsibilities: String = ""
)

data class BiodataTemplate(
    val id: String,
    val type: BiodataType,
    val name: String,
    val category: String,
    val isPaid: Boolean = false,
    val price: Int = 0,
    val layoutType: String, // "CLASSIC_HEADER", "SIDEBAR_LEFT", "SIDEBAR_RIGHT", "ROYAL_BORDER", "TRADITIONAL_HERITAGE", "TWO_COLUMN", "EXECUTIVE_CORPORATE", "MINIMAL_PORTRAIT", "CREATIVE_ACCENT", "MAGAZINE", "ATS_CLEAN", "WEDDING_FLORAL", "DARK_LUXURY", "COMPACT_GRID"
    val primaryColorHex: String,
    val secondaryColorHex: String,
    val accentColorHex: String,
    val backgroundColorHex: String = "#FFFFFF",
    val hasPhoto: Boolean = true,
    val borderStyle: String = "ORNATE", // "NONE", "HAIRLINE", "DOUBLE_GOLD", "ORNATE", "MODERN_BAR"
    val headerStyle: String = "CREST", // "SIMPLE", "CREST", "GANESHA", "CORPORATE", "ACCENT_BLOCK"
    val fontStyle: String = "SERIF", // "SANS", "SERIF", "GEOMETRIC"
    val isEnabled: Boolean = true,
    val displayOrder: Int = 0
)

data class EBook(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val description: String,
    val coverColor: String = "#1E3A8A",
    val isPaid: Boolean = false,
    val price: Int = 0,
    val rating: Float = 4.8f,
    val pageCount: Int = 50,
    val downloadUrl: String = "",
    val chapters: List<String> = emptyList(),
    val isEnabled: Boolean = true
)

data class AppConfig(
    val appName: String = "Biodata Maker",
    val mainHeading: String = "Biodata Maker",
    val subtitle: String = "Create Professional Biodata in Minutes",
    val supportEmail: String = "support@biodatamaker.app",
    val razorpayKeyId: String = "rzp_test_1DP5mmOlF5G5ag",
    val announcement: String = "100+ Handcrafted Biodata Templates Available!",
    val allowAiImprovements: Boolean = true,
    val currencySymbol: String = "₹",
    val isDevMode: Boolean = true
)

data class OrderRecord(
    val orderId: String = "",
    val itemType: String = "", // "TEMPLATE" or "EBOOK"
    val itemId: String = "",
    val itemName: String = "",
    val amount: Int = 0,
    val userEmail: String = "",
    val userName: String = "",
    val paymentId: String = "",
    val status: String = "SUCCESS", // "SUCCESS", "PENDING", "FAILED"
    val timestamp: Long = System.currentTimeMillis()
)
