package com.example.ui.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExperienceItem
import com.example.data.model.JobBiodata
import com.example.ui.components.ProfilePhotoUploadCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobFormScreen(
    initialData: JobBiodata,
    onDataChanged: (JobBiodata) -> Unit,
    onPreviewClicked: () -> Unit,
    onAiImproveObjective: (original: String, targetRole: String) -> Unit,
    onAiImproveExperience: (original: String, jobTitle: String, index: Int) -> Unit,
    onBack: () -> Unit
) {
    var data by remember { mutableStateOf(initialData) }
    var nameError by remember { mutableStateOf(false) }

    fun updateAndEmit(newData: JobBiodata) {
        data = newData
        onDataChanged(newData)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Biodata & Resume Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onPreviewClicked) {
                        Text("Preview", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }

                    Button(
                        onClick = {
                            if (data.fullName.isBlank()) {
                                nameError = true
                            } else {
                                onPreviewClicked()
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Preview & Save", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Profile Photo Card
            item {
                ProfilePhotoUploadCard(
                    currentPhotoUri = data.profilePhotoUri,
                    onPhotoSelected = { uri ->
                        updateAndEmit(data.copy(profilePhotoUri = uri))
                    }
                )
            }

            // Section 1: Personal Information
            item {
                SectionHeaderCard(
                    title = "Personal Information",
                    subtitle = "Contact details and target job title"
                )
            }

            item {
                OutlinedTextField(
                    value = data.fullName,
                    onValueChange = {
                        nameError = false
                        updateAndEmit(data.copy(fullName = it))
                    },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Rahul Sharma") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Full name is required") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = data.targetRole,
                    onValueChange = { updateAndEmit(data.copy(targetRole = it)) },
                    label = { Text("Target Job Title / Designation") },
                    placeholder = { Text("e.g. Senior Software Engineer / Marketing Manager") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.mobile,
                        onValueChange = { updateAndEmit(data.copy(mobile = it)) },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("+91 9876543210") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = data.email,
                        onValueChange = { updateAndEmit(data.copy(email = it)) },
                        label = { Text("Email ID") },
                        placeholder = { Text("rahul@example.com") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = data.address,
                    onValueChange = { updateAndEmit(data.copy(address = it)) },
                    label = { Text("Current City & State") },
                    placeholder = { Text("Bengaluru, Karnataka") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = data.portfolioOrLinkedin,
                    onValueChange = { updateAndEmit(data.copy(portfolioOrLinkedin = it)) },
                    label = { Text("LinkedIn / Portfolio URL (Optional)") },
                    placeholder = { Text("linkedin.com/in/yourname") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Section 2: Career Objective with Gemini AI
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Career Objective / Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            FilledTonalButton(
                                onClick = { onAiImproveObjective(data.careerObjective, data.targetRole) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Improve", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = data.careerObjective,
                            onValueChange = { updateAndEmit(data.copy(careerObjective = it)) },
                            placeholder = { Text("Write a 2-3 line summary of your professional goals, core strengths and key achievements...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }
            }

            // Section 3: Work Experience
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionHeaderCard(
                        title = "Work Experience",
                        subtitle = "Recent employment history (Fresher? Leave blank or add internships)"
                    )
                }
            }

            // List of Work Experience items
            items(data.workExperience.size) { index ->
                val exp = data.workExperience[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Position #${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = {
                                val updated = data.workExperience.toMutableList().apply { removeAt(index) }
                                updateAndEmit(data.copy(workExperience = updated))
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        OutlinedTextField(
                            value = exp.jobTitle,
                            onValueChange = { newTitle ->
                                val updated = data.workExperience.toMutableList()
                                updated[index] = exp.copy(jobTitle = newTitle)
                                updateAndEmit(data.copy(workExperience = updated))
                            },
                            label = { Text("Job Title") },
                            placeholder = { Text("Software Engineer / Analyst") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = exp.company,
                                onValueChange = { newCompany ->
                                    val updated = data.workExperience.toMutableList()
                                    updated[index] = exp.copy(company = newCompany)
                                    updateAndEmit(data.copy(workExperience = updated))
                                },
                                label = { Text("Company Name") },
                                placeholder = { Text("Tata Consultancy Services") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = exp.duration,
                                onValueChange = { newDur ->
                                    val updated = data.workExperience.toMutableList()
                                    updated[index] = exp.copy(duration = newDur)
                                    updateAndEmit(data.copy(workExperience = updated))
                                },
                                label = { Text("Duration") },
                                placeholder = { Text("2022 - Present") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Key Responsibilities:", style = MaterialTheme.typography.labelMedium)
                            TextButton(
                                onClick = { onAiImproveExperience(exp.responsibilities, exp.jobTitle, index) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Polish Bullets", fontSize = 11.sp)
                            }
                        }

                        OutlinedTextField(
                            value = exp.responsibilities,
                            onValueChange = { newResp ->
                                val updated = data.workExperience.toMutableList()
                                updated[index] = exp.copy(responsibilities = newResp)
                                updateAndEmit(data.copy(workExperience = updated))
                            },
                            placeholder = { Text("• Spearheaded product feature delivery...\n• Collaborated with engineering teams to optimize response times.") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        val updated = data.workExperience + ExperienceItem()
                        updateAndEmit(data.copy(workExperience = updated))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Work Experience")
                }
            }

            // Section 4: Education
            item {
                SectionHeaderCard(
                    title = "Education & Qualifications",
                    subtitle = "Degrees, colleges and academic marks"
                )
            }

            item {
                OutlinedTextField(
                    value = data.graduation,
                    onValueChange = { updateAndEmit(data.copy(graduation = it)) },
                    label = { Text("Graduation / Degree *") },
                    placeholder = { Text("e.g. B.Tech Computer Science, Delhi University (2020 - 2024) - 8.4 CGPA") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = data.postGraduation,
                    onValueChange = { updateAndEmit(data.copy(postGraduation = it)) },
                    label = { Text("Post Graduation (If applicable)") },
                    placeholder = { Text("e.g. M.Tech / MBA (2024 - 2026)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.intermediate,
                        onValueChange = { updateAndEmit(data.copy(intermediate = it)) },
                        label = { Text("Intermediate (12th)") },
                        placeholder = { Text("CBSE Board - 88%") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = data.highSchool,
                        onValueChange = { updateAndEmit(data.copy(highSchool = it)) },
                        label = { Text("High School (10th)") },
                        placeholder = { Text("ICSE / State - 92%") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 5: Additional Information
            item {
                SectionHeaderCard(
                    title = "Skills, Languages & Hobbies",
                    subtitle = "Technical skills and personal proficiencies"
                )
            }

            item {
                OutlinedTextField(
                    value = data.skills,
                    onValueChange = { updateAndEmit(data.copy(skills = it)) },
                    label = { Text("Skills (Comma separated)") },
                    placeholder = { Text("Java, Python, Project Management, Agile, Communication, SQL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = data.languages,
                        onValueChange = { updateAndEmit(data.copy(languages = it)) },
                        label = { Text("Languages Known") },
                        placeholder = { Text("English, Hindi") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = data.hobbies,
                        onValueChange = { updateAndEmit(data.copy(hobbies = it)) },
                        label = { Text("Hobbies & Interests") },
                        placeholder = { Text("Chess, Reading, Football") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = data.certifications,
                    onValueChange = { updateAndEmit(data.copy(certifications = it)) },
                    label = { Text("Certifications / Achievements") },
                    placeholder = { Text("AWS Certified Solutions Architect, Scrum Master") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
