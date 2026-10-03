package com.example.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BiodataType
import com.example.ui.admin.AdminPanelScreen
import com.example.ui.components.AiImprovementDialog
import com.example.ui.components.RazorpayPaymentDialog
import com.example.ui.documents.MyDocumentsScreen
import com.example.ui.ebook.EBookStoreScreen
import com.example.ui.form.JobFormScreen
import com.example.ui.form.MarriageFormScreen
import com.example.ui.home.HomeScreen
import com.example.ui.preview.BiodataPreviewScreen
import com.example.ui.preview.PDFPreviewScreen
import com.example.ui.support.PrivacyPolicyScreen
import com.example.ui.support.SupportScreen
import com.example.ui.support.TermsScreen
import com.example.ui.template.TemplateSelectionScreen
import com.example.ui.viewmodel.BiodataViewModel
import java.io.File

enum class Screen {
    HOME,
    TEMPLATES,
    MARRIAGE_FORM,
    JOB_FORM,
    PREVIEW,
    PDF_RENDERER_PREVIEW,
    DOCUMENTS,
    EBOOK_STORE,
    ADMIN_PANEL,
    PRIVACY,
    TERMS,
    SUPPORT
}

@Composable
fun AppNavigation(viewModel: BiodataViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val screenStack = remember { mutableStateListOf(Screen.HOME) }
    var viewingPdfFile by remember { mutableStateOf<File?>(null) }

    fun navigateTo(screen: Screen) {
        screenStack.add(screen)
        currentScreen = screen
    }

    fun navigateBack() {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
            currentScreen = screenStack.last()
        }
    }

    BackHandler(enabled = screenStack.size > 1) {
        navigateBack()
    }

    // ViewModel States
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()
    val marriageData by viewModel.marriageForm.collectAsStateWithLifecycle()
    val jobData by viewModel.jobForm.collectAsStateWithLifecycle()
    val generatedPdfFile by viewModel.generatedPdfFile.collectAsStateWithLifecycle()
    val isGeneratingPdf by viewModel.isGeneratingPdf.collectAsStateWithLifecycle()
    val aiModal by viewModel.aiModal.collectAsStateWithLifecycle()
    val paymentModal by viewModel.paymentModal.collectAsStateWithLifecycle()
    val allDocs by viewModel.allDocuments.collectAsStateWithLifecycle()
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
    val unlockedTemplateIds by viewModel.unlockedTemplateIds.collectAsStateWithLifecycle()

    val isCurrentTemplateUnlocked = remember(selectedTemplate, unlockedTemplateIds) {
        viewModel.isTemplateUnlocked(selectedTemplate)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            Screen.HOME -> HomeScreen(
                appConfig = appConfig,
                onNavigateToTemplates = { type ->
                    viewModel.selectBiodataType(type)
                    navigateTo(Screen.TEMPLATES)
                },
                onNavigateToDocuments = { navigateTo(Screen.DOCUMENTS) },
                onNavigateToEBooks = { navigateTo(Screen.EBOOK_STORE) },
                onNavigateToSupport = { navigateTo(Screen.SUPPORT) },
                onNavigateToPrivacy = { navigateTo(Screen.PRIVACY) },
                onNavigateToTerms = { navigateTo(Screen.TERMS) },
                onSecretAdminTrigger = { navigateTo(Screen.ADMIN_PANEL) }
            )

            Screen.TEMPLATES -> TemplateSelectionScreen(
                initialType = selectedType,
                onTemplateChosen = { template ->
                    viewModel.selectTemplate(template)
                    if (template.type == BiodataType.MARRIAGE) {
                        navigateTo(Screen.MARRIAGE_FORM)
                    } else {
                        navigateTo(Screen.JOB_FORM)
                    }
                },
                onBack = { navigateBack() }
            )

            Screen.MARRIAGE_FORM -> MarriageFormScreen(
                initialData = marriageData,
                onDataChanged = { viewModel.updateMarriageForm(it) },
                onPreviewClicked = { navigateTo(Screen.PREVIEW) },
                onAiImproveAboutMe = { original, values ->
                    viewModel.triggerAiAboutMeEnhance(original, values)
                },
                onBack = { navigateBack() }
            )

            Screen.JOB_FORM -> JobFormScreen(
                initialData = jobData,
                onDataChanged = { viewModel.updateJobForm(it) },
                onPreviewClicked = { navigateTo(Screen.PREVIEW) },
                onAiImproveObjective = { original, role ->
                    viewModel.triggerAiObjectiveEnhance(original, role)
                },
                onAiImproveExperience = { original, title, index ->
                    viewModel.triggerAiExperienceEnhance(original, title, index)
                },
                onBack = { navigateBack() }
            )

            Screen.PREVIEW -> BiodataPreviewScreen(
                type = selectedType,
                template = selectedTemplate,
                marriageData = marriageData,
                jobData = jobData,
                isUnlocked = isCurrentTemplateUnlocked,
                generatedPdfFile = generatedPdfFile,
                isGeneratingPdf = isGeneratingPdf,
                onEditClicked = {
                    if (selectedType == BiodataType.MARRIAGE) navigateTo(Screen.MARRIAGE_FORM)
                    else navigateTo(Screen.JOB_FORM)
                },
                onChangeTemplateClicked = { navigateTo(Screen.TEMPLATES) },
                onPayClicked = { viewModel.startTemplatePayment(selectedTemplate) },
                onGeneratePdfClicked = {
                    viewModel.generatePdf(
                        onSuccess = { file ->
                            viewingPdfFile = file
                            navigateTo(Screen.PDF_RENDERER_PREVIEW)
                        },
                        onError = { err ->
                            Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onSharePdfClicked = { viewModel.shareCurrentPdf() },
                onEmailPdfClicked = { email, name ->
                    viewModel.emailCurrentPdf(email, name)
                },
                onViewPdfRendererPreview = {
                    viewingPdfFile = generatedPdfFile
                    navigateTo(Screen.PDF_RENDERER_PREVIEW)
                },
                onBack = { navigateBack() }
            )

            Screen.PDF_RENDERER_PREVIEW -> {
                val file = viewingPdfFile ?: generatedPdfFile
                if (file != null && file.exists()) {
                    val userEmail = if (selectedType == BiodataType.MARRIAGE) marriageData.email else jobData.email
                    val userName = if (selectedType == BiodataType.MARRIAGE) marriageData.fullName else jobData.fullName
                    PDFPreviewScreen(
                        pdfFile = file,
                        title = "${selectedTemplate.name} PDF Preview",
                        isPaid = selectedTemplate.isPaid,
                        isUnlocked = isCurrentTemplateUnlocked,
                        price = selectedTemplate.price,
                        userEmail = userEmail,
                        userName = userName,
                        onPayToUnlock = { viewModel.startTemplatePayment(selectedTemplate) },
                        onEditClicked = {
                            if (selectedType == BiodataType.MARRIAGE) navigateTo(Screen.MARRIAGE_FORM)
                            else navigateTo(Screen.JOB_FORM)
                        },
                        onBack = { navigateBack() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("No PDF file ready for preview", modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                    }
                }
            }

            Screen.DOCUMENTS -> MyDocumentsScreen(
                documents = allDocs,
                payments = allPayments,
                onEditMarriage = { doc ->
                    viewModel.loadMarriageBiodata(doc)
                    navigateTo(Screen.MARRIAGE_FORM)
                },
                onEditJob = { doc ->
                    viewModel.loadJobBiodata(doc)
                    navigateTo(Screen.JOB_FORM)
                },
                onViewPdf = { path ->
                    viewingPdfFile = File(path)
                    navigateTo(Screen.PDF_RENDERER_PREVIEW)
                },
                onDeleteDocument = { id ->
                    viewModel.deleteDocument(id)
                    Toast.makeText(context, "Document deleted", Toast.LENGTH_SHORT).show()
                },
                onBack = { navigateBack() }
            )

            Screen.EBOOK_STORE -> EBookStoreScreen(
                onPurchaseEBook = { ebook ->
                    viewModel.startEBookPayment(ebook)
                },
                onBack = { navigateBack() }
            )

            Screen.ADMIN_PANEL -> AdminPanelScreen(
                currentConfig = appConfig,
                onConfigUpdated = {
                    // Handled inside
                },
                onBack = { navigateBack() }
            )

            Screen.PRIVACY -> PrivacyPolicyScreen(onBack = { navigateBack() })
            Screen.TERMS -> TermsScreen(onBack = { navigateBack() })
            Screen.SUPPORT -> SupportScreen(supportEmail = appConfig.supportEmail, onBack = { navigateBack() })
        }

        // Global Gemini AI modal
        AiImprovementDialog(
            isOpen = aiModal.isOpen,
            fieldName = aiModal.fieldName,
            originalText = aiModal.originalText,
            improvedText = aiModal.improvedText,
            isLoading = aiModal.isLoading,
            onApply = { viewModel.applyAiImprovement(it) },
            onDismiss = { viewModel.closeAiModal() }
        )

        // Global Razorpay Payment Dialog
        RazorpayPaymentDialog(
            isOpen = paymentModal.isOpen,
            itemName = paymentModal.itemName,
            amount = paymentModal.amount,
            isProcessing = paymentModal.isProcessing,
            onConfirm = { email, name ->
                viewModel.confirmPayment(email, name) {
                    Toast.makeText(context, "Payment Verified! Unlocked.", Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { viewModel.closePaymentModal() }
        )
    }
}
