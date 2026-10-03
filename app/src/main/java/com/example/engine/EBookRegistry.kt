package com.example.engine

import com.example.data.model.EBook

object EBookRegistry {

    private val defaultEBooks = listOf(
        EBook(
            id = "eb_marriage_guide_1",
            title = "The Art of Indian Matchmaking & Modern Biodata",
            author = "Dr. Ananya Sharma",
            category = "Marriage",
            description = "A comprehensive guide on creating impressive matrimonial biodatas, understanding horoscope compatibility, first meetings, conversation etiquette, and modern family expectations.",
            coverColor = "#881337",
            isPaid = false,
            price = 0,
            rating = 4.9f,
            pageCount = 68,
            chapters = listOf(
                "1. Foundations of a Winning Matrimonial Biodata",
                "2. Navigating Cultural Traditions & Modern Mindsets",
                "3. Crucial Astrological Terminology Explained (Gotra, Manglik, Rashi)",
                "4. Writing an Authentic & Engaging 'About Me' Section",
                "5. What Indian Families Look for: Checklist & Etiquette",
                "6. Mastering the First Virtual & In-Person Meeting"
            )
        ),
        EBook(
            id = "eb_resume_hacks_2",
            title = "Cracking the ATS: High-Impact Resume Mastery",
            author = "Vikramaditya Sen, Ex-Google HR",
            category = "Resume",
            description = "Tested strategies for getting your job biodata and resume past algorithmic ATS filters, crafting high-converting bullets, and landing interviews at top tech and MNC firms.",
            coverColor = "#1E3A8A",
            isPaid = true,
            price = 49,
            rating = 4.8f,
            pageCount = 84,
            chapters = listOf(
                "1. Demystifying Applicant Tracking Systems (ATS) in 2026",
                "2. The Google X-Y-Z Resume Formula for Bullets",
                "3. Choosing the Right Layout: Reverse-Chronological vs Hybrid",
                "4. 500+ Power Verbs and Industry Keyword Index",
                "5. Fresh Graduate vs Experienced: Custom Strategies",
                "6. Salary Negotiation & Follow-up Email Templates"
            )
        ),
        EBook(
            id = "eb_interview_playbook_3",
            title = "The 30-Day Interview Preparation Playbook",
            author = "Pooja Malhotra, Career Coach",
            category = "Career",
            description = "Step-by-step 30-day curriculum covering behavioral STAR interview questions, technical presentation techniques, salary negotiation, and confident communication.",
            coverColor = "#065F46",
            isPaid = true,
            price = 39,
            rating = 4.7f,
            pageCount = 76,
            chapters = listOf(
                "1. Week 1: Crafting Your Professional Story & Elevator Pitch",
                "2. Week 2: Mastering the STAR Method for Behavioral Questions",
                "3. Week 3: Mock Interviews, Body Language & Video Etiquette",
                "4. Week 4: Asking the Right Questions & Closing Strong",
                "5. Bonus: Tricky Scenario Answers (Gaps, Career Switches)"
            )
        ),
        EBook(
            id = "eb_communication_skills_4",
            title = "Effortless Executive Communication & Public Speaking",
            author = "Rohan Deshmukh",
            category = "Personal Development",
            description = "Practical guide to commanding authority in meetings, writing crisp professional emails, overcoming stage fright, and networking effortlessly.",
            coverColor = "#4A154B",
            isPaid = false,
            price = 0,
            rating = 4.9f,
            pageCount = 52,
            chapters = listOf(
                "1. The Core Principles of High-Trust Communication",
                "2. Speaking with Vocal Variety and Magnetic Presence",
                "3. Persuasive Business Writing: Emails and Proposals",
                "4. Handling Conflict & Difficult Questions with Composure",
                "5. The 5-Minute Daily Vocal & Mindfulness Workout"
            )
        ),
        EBook(
            id = "eb_digital_careers_5",
            title = "Navigating Careers in AI & Digital Marketing",
            author = "Siddharth Ray, Tech Entrepreneur",
            category = "Job Search",
            description = "The new rules of hiring in the age of generative AI. Building your digital portfolio, GitHub, LinkedIn branding, and winning freelance contracts worldwide.",
            coverColor = "#0F766E",
            isPaid = true,
            price = 49,
            rating = 4.8f,
            pageCount = 92,
            chapters = listOf(
                "1. The Shifting Job Landscape in 2026",
                "2. Optimizing Your LinkedIn Profile for Inbound Recruiter DMs",
                "3. Building an Unbeatable Proof-of-Work Portfolio",
                "4. High-Paying Remote Opportunities: Global Client Acquisition",
                "5. Future-Proofing Your Career with AI Productivity Workflows"
            )
        ),
        EBook(
            id = "eb_first_90_days_6",
            title = "The First 90 Days at Your New Job",
            author = "Kavita Reddy, Leadership Advisor",
            category = "Career",
            description = "Proven blueprint for making an unforgettable first impression, building stakeholder alliances, setting early wins, and getting promoted faster.",
            coverColor = "#9A3412",
            isPaid = false,
            price = 0,
            rating = 4.8f,
            pageCount = 60,
            chapters = listOf(
                "1. Days 1-30: Absorbing Culture and Building Rapport",
                "2. Days 31-60: Delivering High-Visibility Quick Wins",
                "3. Days 61-90: Establishing Long-Term Influence & Cadence",
                "4. Managing Upward: Aligning with Your Manager's Goals",
                "5. Avoiding Common Pitfalls That Derail High Performers"
            )
        )
    )

    private val eBookOverrides = mutableMapOf<String, EBook>()
    private val unlockedEBookIds = mutableSetOf<String>()

    fun getAllEBooks(): List<EBook> {
        return defaultEBooks.map { eBookOverrides[it.id] ?: it }
    }

    fun getEBookById(id: String): EBook? {
        eBookOverrides[id]?.let { return it }
        return defaultEBooks.firstOrNull { it.id == id }
    }

    fun unlockEBook(id: String) {
        unlockedEBookIds.add(id)
    }

    fun isUnlocked(eBook: EBook): Boolean {
        return !eBook.isPaid || eBook.price == 0 || unlockedEBookIds.contains(eBook.id)
    }

    fun updateEBook(eBook: EBook) {
        eBookOverrides[eBook.id] = eBook
    }

    fun getCategories(): List<String> {
        return listOf("All") + getAllEBooks().map { it.category }.distinct()
    }
}
