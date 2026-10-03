package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BiodataEntity
import com.example.data.local.PaymentEntity
import com.example.data.model.*
import com.example.data.remote.FirebaseManager
import com.example.data.remote.GeminiService
import com.example.data.repository.BiodataRepository
import com.example.engine.EBookRegistry
import com.example.engine.TemplateRegistry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class BiodataViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BiodataRepository(application)

    // Current selection
    private val _selectedType = MutableStateFlow(BiodataType.MARRIAGE)
    val selectedType: StateFlow<BiodataType> = _selectedType.asStateFlow()

    private val _selectedTemplate = MutableStateFlow(TemplateRegistry.getTemplatesByType(BiodataType.MARRIAGE).first())
    val selectedTemplate: StateFlow<BiodataTemplate> = _selectedTemplate.asStateFlow()

    // Active forms
    private val _marriageForm = MutableStateFlow(MarriageBiodata())
    val marriageForm: StateFlow<MarriageBiodata> = _marriageForm.asStateFlow()

    private val _jobForm = MutableStateFlow(JobBiodata())
    val jobForm: StateFlow<JobBiodata> = _jobForm.asStateFlow()

    // Generated PDF state
    private val _generatedPdfFile = MutableStateFlow<File?>(null)
    val generatedPdfFile: StateFlow<File?> = _generatedPdfFile.asStateFlow()

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    // AI modal state: fieldName, original, improved, isLoading
    data class AiModalState(
        val isOpen: Boolean = false,
        val fieldName: String = "",
        val originalText: String = "",
        val improvedText: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )

    private val _aiModal = MutableStateFlow(AiModalState())
    val aiModal: StateFlow<AiModalState> = _aiModal.asStateFlow()

    // Payment state
    data class PaymentModalState(
        val isOpen: Boolean = false,
        val itemType: String = "TEMPLATE", // "TEMPLATE" or "EBOOK"
        val itemId: String = "",
        val itemName: String = "",
        val amount: Int = 0,
        val isProcessing: Boolean = false,
        val isSuccess: Boolean = false,
        val error: String? = null
    )

    private val _paymentModal = MutableStateFlow(PaymentModalState())
    val paymentModal: StateFlow<PaymentModalState> = _paymentModal.asStateFlow()

    // Purchases
    private val _unlockedTemplateIds = MutableStateFlow(mutableSetOf<String>())
    val unlockedTemplateIds: StateFlow<Set<String>> = _unlockedTemplateIds.asStateFlow()

    // Documents & Payments list
    val allDocuments: StateFlow<List<BiodataEntity>> = repository.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentEntity>> = repository.getAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appConfig: StateFlow<AppConfig> = FirebaseManager.appConfig
    val isAdminLoggedIn: StateFlow<Boolean> = FirebaseManager.isAdminLoggedIn

    init {
        FirebaseManager.initialize(application)
    }

    fun selectBiodataType(type: BiodataType) {
        _selectedType.value = type
        val templates = TemplateRegistry.getTemplatesByType(type)
        if (templates.isNotEmpty()) {
            _selectedTemplate.value = templates.first()
        }
    }

    fun selectTemplate(template: BiodataTemplate) {
        _selectedTemplate.value = template
        if (template.type == BiodataType.MARRIAGE) {
            _marriageForm.value = _marriageForm.value.copy(templateId = template.id)
        } else {
            _jobForm.value = _jobForm.value.copy(templateId = template.id)
        }
    }

    // Marriage Form Updates
    fun updateMarriageForm(updated: MarriageBiodata) {
        _marriageForm.value = updated
        // Auto-save draft
        viewModelScope.launch {
            repository.saveMarriageDraft(updated)
        }
    }

    fun resetMarriageForm() {
        _marriageForm.value = MarriageBiodata(templateId = _selectedTemplate.value.id)
    }

    fun loadMarriageBiodata(biodata: MarriageBiodata) {
        _marriageForm.value = biodata
        _selectedType.value = BiodataType.MARRIAGE
        val t = TemplateRegistry.getTemplateById(biodata.templateId)
        _selectedTemplate.value = t
    }

    // Job Form Updates
    fun updateJobForm(updated: JobBiodata) {
        _jobForm.value = updated
        viewModelScope.launch {
            repository.saveJobDraft(updated)
        }
    }

    fun resetJobForm() {
        _jobForm.value = JobBiodata(templateId = _selectedTemplate.value.id)
    }

    fun loadJobBiodata(biodata: JobBiodata) {
        _jobForm.value = biodata
        _selectedType.value = BiodataType.JOB
        val t = TemplateRegistry.getTemplateById(biodata.templateId)
        _selectedTemplate.value = t
    }

    // AI Enhancements
    fun triggerAiObjectiveEnhance(original: String, targetRole: String) {
        _aiModal.value = AiModalState(
            isOpen = true,
            fieldName = "Career Objective",
            originalText = original,
            isLoading = true
        )
        viewModelScope.launch {
            val improved = GeminiService.improveCareerObjective(original, targetRole)
            _aiModal.value = _aiModal.value.copy(
                improvedText = improved,
                isLoading = false
            )
        }
    }

    fun triggerAiExperienceEnhance(original: String, jobTitle: String, expIndex: Int) {
        _aiModal.value = AiModalState(
            isOpen = true,
            fieldName = "Work Experience ($jobTitle)",
            originalText = original,
            isLoading = true
        )
        viewModelScope.launch {
            val improved = GeminiService.improveWorkExperience(original, jobTitle)
            _aiModal.value = _aiModal.value.copy(
                improvedText = improved,
                isLoading = false
            )
        }
    }

    fun triggerAiAboutMeEnhance(original: String, values: String) {
        _aiModal.value = AiModalState(
            isOpen = true,
            fieldName = "About Me & Expectations",
            originalText = original,
            isLoading = true
        )
        viewModelScope.launch {
            val improved = GeminiService.improveAboutMe(original, values)
            _aiModal.value = _aiModal.value.copy(
                improvedText = improved,
                isLoading = false
            )
        }
    }

    fun applyAiImprovement(text: String) {
        val field = _aiModal.value.fieldName
        if (field == "Career Objective") {
            _jobForm.value = _jobForm.value.copy(careerObjective = text)
            updateJobForm(_jobForm.value)
        } else if (field.startsWith("Work Experience")) {
            val list = _jobForm.value.workExperience.toMutableList()
            if (list.isNotEmpty()) {
                list[0] = list[0].copy(responsibilities = text)
                _jobForm.value = _jobForm.value.copy(workExperience = list)
                updateJobForm(_jobForm.value)
            }
        } else if (field == "About Me & Expectations") {
            _marriageForm.value = _marriageForm.value.copy(aboutMe = text)
            updateMarriageForm(_marriageForm.value)
        }
        closeAiModal()
    }

    fun closeAiModal() {
        _aiModal.value = AiModalState()
    }

    // PDF Generation
    fun generatePdf(onSuccess: (File) -> Unit, onError: (String) -> Unit) {
        _isGeneratingPdf.value = true
        viewModelScope.launch {
            try {
                val file = if (_selectedType.value == BiodataType.MARRIAGE) {
                    repository.generateAndSaveMarriagePdf(_marriageForm.value, _selectedTemplate.value)
                } else {
                    repository.generateAndSaveJobPdf(_jobForm.value, _selectedTemplate.value)
                }
                _generatedPdfFile.value = file
                _isGeneratingPdf.value = false
                onSuccess(file)
            } catch (e: Exception) {
                _isGeneratingPdf.value = false
                onError(e.message ?: "PDF generation failed")
            }
        }
    }

    fun shareCurrentPdf() {
        _generatedPdfFile.value?.let { repository.sharePdf(it) }
    }

    fun emailCurrentPdf(userEmail: String, name: String) {
        _generatedPdfFile.value?.let { repository.emailPdf(it, userEmail, name) }
    }

    // Payment Flow
    fun isTemplateUnlocked(template: BiodataTemplate): Boolean {
        if (!template.isPaid || template.price == 0) return true
        return _unlockedTemplateIds.value.contains(template.id)
    }

    fun startTemplatePayment(template: BiodataTemplate) {
        _paymentModal.value = PaymentModalState(
            isOpen = true,
            itemType = "TEMPLATE",
            itemId = template.id,
            itemName = template.name,
            amount = template.price
        )
    }

    fun startEBookPayment(eBook: EBook) {
        _paymentModal.value = PaymentModalState(
            isOpen = true,
            itemType = "EBOOK",
            itemId = eBook.id,
            itemName = eBook.title,
            amount = eBook.price
        )
    }

    fun confirmPayment(userEmail: String, userName: String, onComplete: () -> Unit) {
        val current = _paymentModal.value
        _paymentModal.value = current.copy(isProcessing = true)

        viewModelScope.launch {
            // Simulated secure Razorpay server verification callback
            kotlinx.coroutines.delay(1200)
            val orderId = "order_${System.currentTimeMillis()}"
            val paymentId = "pay_rzp_${(100000..999999).random()}"

            // Record to DB & Firestore
            repository.recordPayment(
                orderId = orderId,
                itemId = current.itemId,
                itemName = current.itemName,
                itemType = current.itemType,
                amount = current.amount,
                paymentId = paymentId,
                userEmail = userEmail
            )

            FirebaseManager.logOrder(
                OrderRecord(
                    orderId = orderId,
                    itemType = current.itemType,
                    itemId = current.itemId,
                    itemName = current.itemName,
                    amount = current.amount,
                    userEmail = userEmail,
                    userName = userName,
                    paymentId = paymentId,
                    status = "SUCCESS"
                )
            )

            if (current.itemType == "TEMPLATE") {
                _unlockedTemplateIds.value = (_unlockedTemplateIds.value + current.itemId).toMutableSet()
            } else if (current.itemType == "EBOOK") {
                EBookRegistry.unlockEBook(current.itemId)
            }

            _paymentModal.value = current.copy(
                isProcessing = false,
                isSuccess = true
            )
            onComplete()
        }
    }

    fun closePaymentModal() {
        _paymentModal.value = PaymentModalState()
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocument(id)
        }
    }
}
