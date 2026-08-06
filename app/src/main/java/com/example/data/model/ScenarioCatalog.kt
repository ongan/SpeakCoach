package com.example.data.model

object ScenarioCatalog {

    val quickPracticeScenarios = listOf(
        ScenarioDefinition(
            id = "quick_coffee_shop",
            title = "Coffee Shop Order (Dinamik Kafe Siparişi)",
            description = "İçeceğinizin boyunu, süt tipini ve paket/masa tercihini belirleyin. Sürpriz stok durumları veya kart problemleri ile pratik yapın.",
            cefrLevel = "CEFR A1-A2",
            location = "Bustling Downtown Cafe",
            aiRole = "Friendly Barista",
            userRole = "Customer ordering coffee & pastry",
            mainGoal = "Order coffee and handle payment/preferences smoothly",
            subGoals = listOf("Choose size and drink", "Specify milk/sugar preference", "Handle complication/payment", "Confirm order"),
            targetVocabulary = listOf("Espresso", "Oat milk", "Takeaway", "Receipt", "Contactless"),
            grammarFocus = "Polite requests (I would like..., Can I have...)",
            variableOptions = mapOf(
                "drinkType" to listOf("Iced Latte", "Cappuccino", "Caramel Macchiato", "Americano"),
                "milkChoice" to listOf("Whole milk", "Oat milk", "Almond milk", "Lactose-free"),
                "complication" to listOf("Oat milk is sold out", "Card reader is slow", "Pastry discount offer", "Size upgrade promotion")
            ),
            starterPrompt = "Hi welcome to Starlight Coffee! What can I get started for you today?"
        ),
        ScenarioDefinition(
            id = "quick_job_interview",
            title = "Job Interview (Dinamik İş Mülakatı)",
            description = "Her oturumda farklı şirket ve pozisyon için mülakata girin. Deneyimleriniz, güçlü yönleriniz ve maaş beklentiniz sorulacak.",
            cefrLevel = "CEFR B2-C1",
            location = "Corporate Office / Online Video Call",
            aiRole = "Senior Hiring Manager",
            userRole = "Job Applicant",
            mainGoal = "Convince the manager of your skills and fit for the role",
            subGoals = listOf("Introduce experience", "Provide problem-solving example", "Ask questions about team culture", "Discuss expectations"),
            targetVocabulary = listOf("Leadership", "Problem-solving", "Impact", "Collaboration", "Strategy"),
            grammarFocus = "Present perfect & past simple for accomplishments",
            variableOptions = mapOf(
                "companyType" to listOf("Tech Startup", "Global Marketing Agency", "Financial Institution", "Design Studio"),
                "jobRole" to listOf("Software Engineer", "Marketing Specialist", "Project Manager", "UX Designer"),
                "focusQuestion" to listOf("Handling tight deadlines", "Resolving team conflicts", "Managing complex projects")
            ),
            starterPrompt = "Welcome! Thank you for joining us today. Could you start by telling me a bit about yourself and your background?"
        ),
        ScenarioDefinition(
            id = "quick_hotel_checkin",
            title = "Hotel Check-in & Special Requests",
            description = "Otel resepsiyonunda kayıt yaptırın. Rezervasyon bulamama, oda değişikliği veya Wi-Fi sorgusu gibi durumları çözün.",
            cefrLevel = "CEFR A2-B1",
            location = "Grand Plaza Hotel Reception",
            aiRole = "Front Desk Agent",
            userRole = "Hotel Guest",
            mainGoal = "Successfully check in and resolve room preferences or booking queries",
            subGoals = listOf("Confirm booking details", "Request high floor/quiet room", "Ask about breakfast & Wi-Fi", "Get room key"),
            targetVocabulary = listOf("Reservation", "High floor", "Key card", "Complimentary", "Luggage"),
            grammarFocus = "Indirect questions & polite requests",
            variableOptions = mapOf(
                "bookingStatus" to listOf("Found under user name", "Minor spelling mismatch in name", "Room upgrade available"),
                "specialRequest" to listOf("Quiet room away from elevator", "Late check-out option", "Extra towels & pillows")
            ),
            starterPrompt = "Good afternoon! Welcome to Grand Plaza Hotel. How may I assist you with your reservation today?"
        ),
        ScenarioDefinition(
            id = "quick_airport_customs",
            title = "Airport Border Control & Customs",
            description = "Pasaport kontrolünde seyahat amacınız, konaklama adresiniz ve bagajınız hakkında sorulan soruları yanıtlayın.",
            cefrLevel = "CEFR B1-B2",
            location = "International Airport Customs Officer Counter",
            aiRole = "Immigration & Customs Officer",
            userRole = "International Traveler",
            mainGoal = "Pass border inspection smoothly by answering security questions clearly",
            subGoals = listOf("State purpose of visit", "Declare duration of stay", "Provide hotel/stay address", "Confirm travel insurance/return ticket"),
            targetVocabulary = listOf("Passport", "Sightseeing", "Duration", "Customs declaration", "Return ticket"),
            grammarFocus = "Future intentions (going to, planning to) & past travel details",
            variableOptions = mapOf(
                "tripPurpose" to listOf("Tourism and sightseeing", "Attending an international conference", "Visiting relatives"),
                "officerTone" to listOf("Strict & precise", "Polite & efficient", "Curious about travel itinerary")
            ),
            starterPrompt = "Next please. Passport and landing card. What is the main purpose of your visit to the country?"
        ),
        ScenarioDefinition(
            id = "quick_weekend_chat",
            title = "Casual Weekend Chat & Movies",
            description = "Hafta sonu planları, izlenen filmler, hobiler ve günlük sohbet üzerine doğal, arkadaşça diyalog kurun.",
            cefrLevel = "CEFR B1-B2",
            location = "Cozy Park Bench / Cafe Chat",
            aiRole = "Friendly Acquaintance",
            userRole = "Friend / Colleague",
            mainGoal = "Maintain an engaging, mutual casual conversation",
            subGoals = listOf("Share weekend activities", "Recommend a movie or show", "Ask about friend's hobbies", "Plan future meetup"),
            targetVocabulary = listOf("Binge-watch", "Recommendation", "Catch up", "Unwind", "Weekend vibe"),
            grammarFocus = "Conversational connectors & opinion phrases (In my opinion, I reckon, Personally)",
            variableOptions = mapOf(
                "topicFocus" to listOf("Latest sci-fi streaming series", "Outdoor hiking trip", "New restaurant opening", "Cooking at home"),
                "chatVibe" to listOf("Enthusiastic & relaxed", "Thoughtful & analytical")
            ),
            starterPrompt = "Hey there! It's so good to see you. How has your week been, and any exciting plans for this weekend?"
        )
    )

    fun getStoryChapters(userName: String): List<StoryChapter> {
        val name = if (userName.isNotBlank()) userName else "Deniz"
        return listOf(
            StoryChapter(
                id = "ch_1",
                chapterNumber = 1,
                rawTitle = "Bölüm 1 – {name}’in Amerika’daki İlk Günleri",
                description = "{name}'in Amerika'ya inişi, ilk otobüs yolculuğu, adresi bulması ve akrabalarıyla buluşması.",
                cefrLevel = "CEFR A1-A2",
                scenes = listOf(
                    StoryScene("s_1_1", "ch_1", 1, "Amerika’ya Uçuşta Tanışma", "Uçakta yan koltuktaki yolcu ile ilk tanışma sohbeti.", "CEFR A1", "Hello! Is this seat taken? Are you traveling to New York too?", "Flight Passenger"),
                    StoryScene("s_1_2", "ch_1", 2, "İlk Defa Otobüse Binme", "Havalimanı otobüs şoförüne bilet sorma ve rotayı teyit etme.", "CEFR A1", "Welcome aboard the Airport Express! Where are you heading today?", "Bus Driver"),
                    StoryScene("s_1_3", "ch_1", 3, "Gideceği Adresi Bulma", "Sokaktaki birine harita göstererek amcasının evinin adresini sorma.", "CEFR A1", "Excuse me! You look a bit lost. Do you need help finding an address?", "Friendly Pedestrian"),
                    StoryScene("s_1_4", "ch_1", 4, "İngilizce Konuşan Yengesiyle Tanışma", "Eve varışta yengesi Sarah ile sıcak selamlama ve ev turu.", "CEFR A2", "Oh {name}! Welcome to our home! Come on in, how was your flight?", "Aunt Sarah"),
                    StoryScene("s_1_5", "ch_1", 5, "Amerika’daki Akrabalarıyla İlk Görüşme", "Akşam yemeğinde aile üyeleriyle sohbet etme ve seyahat izlenimlerini paylaşma.", "CEFR A2", "We are so happy to have you here, {name}. What was your first impression of the city?", "Uncle Sam"),
                    StoryScene("s_1_6", "ch_1", 6, "İlk Sabah ve Kahvaltı", "Sabah kahvaltısında Amerikan tarzı krep ve kahve siparişi hakkında konuşma.", "CEFR A1", "Good morning {name}! Did you sleep well? Would you like pancakes or toast for breakfast?", "Aunt Sarah"),
                    StoryScene("s_1_7", "ch_1", 7, "Bir Amerika Kafesindeki İlk Deneyim", "Mahalle kafesinde tek başına ilk defa sipariş verme.", "CEFR A2", "Good morning! Welcome to Corner Bakery. What can I get for you?", "Cafe Cashier"),
                    StoryScene("s_1_8", "ch_1", 8, "Eğlence ve Film Seçimi", "Kuzen ile sinemada hangi filme gidileceğine karar verme.", "CEFR A2", "Hey {name}! There are two good movies playing tonight: action or comedy. Which do you prefer?", "Cousin Eric"),
                    StoryScene("s_1_9", "ch_1", 9, "İngilizce Konuşulan Masadaki Akşam Yemeği", "Ailenin yabancı misafirleriyle akşam yemeğinde kendini tanıtma.", "CEFR A2", "Hello {name}, I'm David, a family neighbor. What are your plans while staying in America?", "Neighbor David"),
                    StoryScene("s_1_10", "ch_1", 10, "İlk Yalnızlık/Can Sıkıntısını Giderme", "Parkta yürürken evcil hayvan gezdiren bir insanla sohbet başlatma.", "CEFR A2", "Hi! Your dog is so friendly. What breed is he?", "Dog Owner")
                )
            ),
            StoryChapter(
                id = "ch_2",
                chapterNumber = 2,
                rawTitle = "Bölüm 2 – {name}’in İş Arama Macerası",
                description = "{name}'in iş ilanlarını incelemesi, mülakatlara girmesi ve ilk işini bulması.",
                cefrLevel = "CEFR A2-B1",
                scenes = listOf(
                    StoryScene("s_2_1", "ch_2", 1, "İlk İş Arayışı", "Kariyer merkezindeki danışmana uygun part-time işler sorma.", "CEFR A2", "Hello {name}! Let's look at available job openings for university students.", "Career Advisor"),
                    StoryScene("s_2_2", "ch_2", 2, "İlk İş Görüşmesi", "Bir kafede komi/garson pozisyonu için mülakat.", "CEFR B1", "Thanks for coming, {name}. Do you have any previous experience in customer service?", "Cafe Manager"),
                    StoryScene("s_2_3", "ch_2", 3, "Amcasından İş Tavsiyesi Alma", "Tavsiyeler üzerine çalışma izni ve özgeçmiş üzerine sohbet.", "CEFR A2", "{name}, don't feel discouraged! Let's polish your resume together.", "Uncle Sam"),
                    StoryScene("s_2_4", "ch_2", 4, "Gelecekteki Patronuyla Görüşme", "Downtown restoranda müdürle mülakata girme ve çalışma saatlerini konuşma.", "CEFR B1", "Hi {name}! We need someone who is punctual and friendly. What hours are you available?", "Restaurant Owner Mr. Miller"),
                    StoryScene("s_2_5", "ch_2", 5, "“Artık Bir İşim Var” Sevinci", "İşe kabul edildiğini amcasına telefonla muştulama.", "CEFR A2", "Uncle Sam! Great news! I got the job at Miller's Restaurant!", "Uncle Sam"),
                    StoryScene("s_2_6", "ch_2", 6, "Restorandaki İlk Müşteri", "İlk gününde restoranda müşteriden sipariş alma heyecanı.", "CEFR A2", "Hi! Excuse me, we are ready to order. What soup do you recommend today?", "Restaurant Customer"),
                    StoryScene("s_2_7", "ch_2", 7, "İkinci Gün Farklı Bir Müşteri", "Alerjisi olan zorlu bir müşterinin sorularını yanıtlama.", "CEFR B1", "Does this dish contain any peanuts or dairy? I am allergic.", "Customer with Allergy"),
                    StoryScene("s_2_8", "ch_2", 8, "İlk Kez Borç Para İsteme", "Amcasından ilk ayki ulaşım kartı için borç isteme ve mahcubiyet.", "CEFR B1", "Uncle Sam, can I talk to you about my bus pass expenses for this month?", "Uncle Sam"),
                    StoryScene("s_2_9", "ch_2", 9, "Bilgisayar Mağazasında Alışveriş", "Teknoloji mağazasından çalışma için ders laptopu alma.", "CEFR B1", "Welcome to TechWorld! Are you looking for a laptop for studies or work?", "Tech Sales Clerk"),
                    StoryScene("s_2_10", "ch_2", 10, "Amerika’daki İlk Sinema Deneyimi", "Tek başına sinemadan bilet ve mısır alma.", "CEFR A2", "One ticket for the 7 PM show please. Would you like a large popcorn combo?", "Cinema Cashier")
                )
            ),
            StoryChapter(
                id = "ch_3",
                chapterNumber = 3,
                rawTitle = "Bölüm 3 – {name} Yeni İşine Alışıyor",
                description = "{name}'in müşteri ilişkilerini geliştirmesi, sosyal çevresini büyütmesi ve ilk maaş sevinci.",
                cefrLevel = "CEFR B1",
                scenes = listOf(
                    StoryScene("s_3_1", "ch_3", 1, "Amerika Yemek Kültürünü Öğrenme", "İş arkadaşıyla Amerikan porsiyonları ve bahşiş (tip) sistemi hakkında sohbet.", "CEFR B1", "In America, tipping 18-20% is standard! Let me explain how servers divide tips.", "Coworker Mark"),
                    StoryScene("s_3_2", "ch_3", 2, "Müşteriye Menü Tavsiyesi Verme", "En çok satan tatlı ve ana yemekleri hevesle müşteriye tanıtma.", "CEFR B1", "Everything smells amazing! What is your personal favorite dish here, {name}?", "Dinner Guest"),
                    StoryScene("s_3_3", "ch_3", 3, "Yaşlı Bir Müşteriyle Sipariş Konuşması", "Düzenli gelen tatlı yaşlı bir müşteriyle samimi konuşma.", "CEFR B1", "Hello young man! You must be new here. How are you liking New York?", "Regular Customer Mrs. Gable"),
                    StoryScene("s_3_4", "ch_3", 4, "Jessica’yı Sosyal Medyada Bulma", "İş yerindeki müşteri Jessica ile takipleşme sohbeti.", "CEFR B1", "Hey {name}! I noticed your restaurant profile! Are you on Instagram?", "Jessica"),
                    StoryScene("s_3_5", "ch_3", 5, "İşe Geç Kalma", "Trafik yüzünden müdüre durumu açıklayıp özür dileme.", "CEFR B1", "{name}, you're 15 minutes late today. Is everything okay?", "Restaurant Owner Mr. Miller"),
                    StoryScene("s_3_6", "ch_3", 6, "Taksi Şoförüyle Konuşma", "Aceleyle taksiye binip kestirme yoldan gitmesini rica etme.", "CEFR B1", "Where to, buddy? Traffic on 5th Avenue is jammed right now!", "Taxi Driver"),
                    StoryScene("s_3_7", "ch_3", 7, "İlk Maaşla Alışveriş", "İlk kazandığı maaş çekini yatırıp kendine yeni bir mont alma.", "CEFR B1", "Congrats on your first paycheck! Are you going to save it or celebrate?", "Coworker Mark"),
                    StoryScene("s_3_8", "ch_3", 8, "İlk Borcunu Ödeme", "Amcasına aldığı borcu geri verip teşekkür etme.", "CEFR B1", "Uncle Sam, here is the money I borrowed for my transit pass. Thank you so much!", "Uncle Sam"),
                    StoryScene("s_3_9", "ch_3", 9, "Akrabasından Hediye Alma", "Yengesinin sürpriz hediyesine içten teşekkür etme.", "CEFR B1", "{name}, we bought you a nice winter coat for the New York weather!", "Aunt Sarah")
                )
            ),
            StoryChapter(
                id = "ch_4",
                chapterNumber = 4,
                rawTitle = "Bölüm 4 – {name}’in Yeni Eve Taşınma Macerası",
                description = "{name}'in kendi kiralık dairesini araması, eşyaları taşıması ve komşularıyla kaynaşması.",
                cefrLevel = "CEFR B1-B2",
                scenes = listOf(
                    StoryScene("s_4_1", "ch_4", 1, "Kiralık Ev Arama", "Emlakçı ile bütçeye uygun 1+1 stüdyo daireler hakkında görüşme.", "CEFR B1", "Hi {name}! I have two studio apartments available near your workplace.", "Real Estate Agent"),
                    StoryScene("s_4_2", "ch_4", 2, "Yeni Eve Taşınma", "Ev sahibi ile kira sözleşmesi ve depo imzalaması.", "CEFR B2", "Here is the lease agreement for 12 months. Please review the security deposit clause.", "Landlord Mr. Henderson"),
                    StoryScene("s_4_3", "ch_4", 3, "Taksi Konuşması", "Büyük kutuları taksiye yüklerken şoförden yardım isteme.", "CEFR B1", "Need a hand loading those big boxes into the trunk?", "Taxi Driver"),
                    StoryScene("s_4_4", "ch_4", 4, "Mahallede İlk Komşuyla Tanışma", "Kapı komşusu Alex ile koridorda selamlaşma.", "CEFR B1", "Hi! I see someone moving in next door. I'm Alex from apartment 4B!", "Neighbor Alex"),
                    StoryScene("s_4_5", "ch_4", 5, "Market Alışverişi", "Süpermarkette temizlik malzemeleri ve mutfak eşyaları arama.", "CEFR B1", "Excuse me, where can I find paper towels and dishwashing detergent?", "Store Clerk"),
                    StoryScene("s_4_6", "ch_4", 6, "Yeni Komşuyla Karşılaşma", "Alex'ten en yakın çamaşırhaneyi öğrenme.", "CEFR B1", "Hey {name}! Do you know where the nearest laundromat is around here?", "Neighbor Alex"),
                    StoryScene("s_4_7", "ch_4", 7, "Jessica’yı Yemeğe Çağırma", "Yeni evinde kutlama yemeği için Jessica'ya davet mesajı ve konuşma.", "CEFR B2", "Hey {name}! I'd love to visit your new place! Should I bring dessert?", "Jessica"),
                    StoryScene("s_4_8", "ch_4", 8, "Jessica’nın Ailesinin Ziyareti", "Jessica'nın annesiyle tanışıp kibarca sohbet etme.", "CEFR B2", "Hello {name}, Jessica told us so much about your courage moving to America!", "Jessica's Mother"),
                    StoryScene("s_4_9", "ch_4", 9, "İş Arkadaşına Hediye Seçimi", "Mark'ın doğum günü için hediye dükkanında fikir alışverişi.", "CEFR B1", "What do you think Mark would prefer: a coffee mug or a leather notebook?", "Jessica"),
                    StoryScene("s_4_10", "ch_4", 10, "İngilizcesindeki Gelişimi Fark Etme", "Evde aynada kendisiyle konuşarak ne kadar akıcılaştığını tebessümle fark etme.", "CEFR B2", "Wow {name}, I noticed you didn't hesitate at all during dinner tonight!", "Aunt Sarah")
                )
            ),
            StoryChapter(
                id = "ch_5",
                chapterNumber = 5,
                rawTitle = "Bölüm 5 – {name}’in Yeni Evindeki Maceralar",
                description = "{name}'in mahalle yaşamı, spor rutini, aile aramaları ve piknik planları.",
                cefrLevel = "CEFR B2",
                scenes = listOf(
                    StoryScene("s_5_1", "ch_5", 1, "Kapı Komşusuyla Arkadaş Olma", "Alex ile kahve içip hafta sonu maçını izleme planı.", "CEFR B2", "Hey {name}! Are you a soccer fan? The Champions League final is on Saturday!", "Neighbor Alex"),
                    StoryScene("s_5_2", "ch_5", 2, "Spor Mağazasının Yerini Öğrenme", "Koşu ayakkabısı almak için mağaza danışmanına danışma.", "CEFR B1", "Hi! Looking for road running shoes or gym training sneakers?", "Sports Store Associate"),
                    StoryScene("s_5_3", "ch_5", 3, "Spor Malzemesi Alırken Satıcıyla Konuşma", "Beden ve esneklik denerken satıcıyla sohbet etme.", "CEFR B2", "How do those feel on your feet? Need half a size bigger?", "Sales Associate"),
                    StoryScene("s_5_4", "ch_5", 4, "Yengesiyle Telefonda Konuşma", "Yengesiyle telefonda hal hatır sorma ve ev durumu.", "CEFR B1", "Hello {name}! How is living on your own in the new apartment?", "Aunt Sarah"),
                    StoryScene("s_5_5", "ch_5", 5, "Kuzeniyle Telefonda Akıcı İngilizce", "Kuzeni Eric ile takılırken hiç duraksamadan İngilizce şakalaşma.", "CEFR B2", "Dude! You sound like a native New Yorker now! That's impressive!", "Cousin Eric"),
                    StoryScene("s_5_6", "ch_5", 6, "Akrabalarıyla Piknik Planlama", "Central Park'ta açık hava pikniği için menü kararlaştırma.", "CEFR B2", "Let's organize a Sunday picnic! Who is bringing sandwiches and drinks?", "Uncle Sam"),
                    StoryScene("s_5_7", "ch_5", 7, "Aile Pikniği", "Parkta frisbee oynayıp Barbekü üzerine sohbet etme.", "CEFR B2", "Passing the frisbee to you, {name}! Catch it!", "Cousin Eric"),
                    StoryScene("s_5_8", "ch_5", 8, "Komşuyla Buluşma Planı", "Alex ile akşam yemeği pişirme etkinliği düzenleme.", "CEFR B2", "I'll bring the pasta, you bring the salad! Deal?", "Neighbor Alex")
                )
            ),
            StoryChapter(
                id = "ch_6",
                chapterNumber = 6,
                rawTitle = "Bölüm 6 – {name}’in Arkadaşlarıyla Aktiviteleri",
                description = "{name}'in arkadaş çevresiyle derin sohbetleri, spor aktiviteleri ve iş yerinde beklenmedik haberler.",
                cefrLevel = "CEFR B2",
                scenes = listOf(
                    StoryScene("s_6_1", "ch_6", 1, "Arkadaşıyla Telefonda Sohbet", "Türkiye'deki arkadaşına Amerika'daki hayatını anlatma.", "CEFR B2", "Hey {name}! I miss you man! Tell me everything about your life in America!", "Old Friend Can"),
                    StoryScene("s_6_2", "ch_6", 2, "Mike ile Kafede Konuşma", "İş arkadaşı Mike ile kariyer hedefleri hakkında konuşma.", "CEFR B2", "Do you see yourself staying in the restaurant business or starting a tech project?", "Mike"),
                    StoryScene("s_6_3", "ch_6", 3, "Seyahatten Dönən Jessica’yı Karşılama", "Jessica'nın tatil fotoğrafları ve hediyeleri hakkında konuşma.", "CEFR B2", "{name}! I brought you a souvenir from California! Look at these photos!", "Jessica"),
                    StoryScene("s_6_4", "ch_6", 4, "Jessica ile Tenis Oynama", "Kortta skor tutma ve spor dostluğu.", "CEFR B2", "Nice serve, {name}! That point goes to you!", "Jessica"),
                    StoryScene("s_6_5", "ch_6", 5, "Jessica ile Restoranda Konuşma", "İtalyan restoranında gelecek planları üzerine derin sohbet.", "CEFR B2", "What is your biggest dream for the next five years, {name}?", "Jessica"),
                    StoryScene("s_6_6", "ch_6", 6, "İş Gezisi Hakkında Sohbet", "Boston'daki kısa restoran eğitimi hakkında bilgi alma.", "CEFR B2", "Mr. Miller selected you for the hospitality seminar in Boston next week!", "Mike"),
                    StoryScene("s_6_7", "ch_6", 7, "İş Yeri Hakkında Kötü Haber Alma", "Restoranın satılacağı dedikodularını öğrenme ve endişelenme.", "CEFR B2", "{name}, did you hear? The landlord might sell the building where the restaurant is!", "Mike")
                )
            ),
            StoryChapter(
                id = "ch_7",
                chapterNumber = 7,
                rawTitle = "Bölüm 7 – {name}’in Yeni İş Macerası",
                description = "{name}'in kendi girişimcilik fikrini geliştirmesi, yatırım araması ve dönüş yolculuğu.",
                cefrLevel = "CEFR B2-C1",
                scenes = listOf(
                    StoryScene("s_7_1", "ch_7", 1, "İş Fikrini Eski Patronuna Anlatma", "Kendi online Türk lezzetleri / kahve girişim fikrini Miller'a sunma.", "CEFR B2", "That sounds ambitious, {name}! Tell me more about your distribution plan.", "Mr. Miller"),
                    StoryScene("s_7_2", "ch_7", 2, "Yeni İş Planı Hazırlama", "Finansal bütçe ve dijital pazarlama planını dosyalama.", "CEFR C1", "Let's review the projected operating costs and initial marketing push.", "Business Mentor"),
                    StoryScene("s_7_3", "ch_7", 3, "Jessica’yı Kahve İçmeye Davet Etme", "Yeni projesinin heyecanını Jessica ile paylaşma.", "CEFR B2", "I am so proud of your initiative, {name}! Tell me how I can help!", "Jessica"),
                    StoryScene("s_7_4", "ch_7", 4, "Patronla İş Görüşmesi", "Yatırım ortaklığı ve ilk sermaye üzerine konuşma.", "CEFR C1", "If I invest 20% in your venture, what equity share are you offering?", "Mr. Miller"),
                    StoryScene("s_7_5", "ch_7", 5, "Borç Para Arama", "Banka kredisi ve melek yatırımcı seçeneklerini değerlendirme.", "CEFR C1", "Good day {name}! Let's examine your credit history for a small business loan.", "Bank Officer"),
                    StoryScene("s_7_6", "ch_7", 6, "Jessica ile Telefon Görüşmesi", "Yoğun geçen günün ardından Jessica ile dertleşme.", "CEFR B2", "You sound tired today, {name}. Take a breath, you're doing amazing!", "Jessica"),
                    StoryScene("s_7_7", "ch_7", 7, "Jessica’nın Türkiye’ye Gideceğini Öğrenme", "Jessica'nın İstanbul seyahati müjdesi.", "CEFR B2", "Guess what {name}? My university exchange program is sending me to Istanbul!", "Jessica"),
                    StoryScene("s_7_8", "ch_7", 8, "Uçak Bileti Problemi", "Aynı uçuşa bilet bulmak için müşteri hizmetlerini arama.", "CEFR B2", "Airline desk, how may I assist with your flight reservation change?", "Airline Agent"),
                    StoryScene("s_7_9", "ch_7", 9, "Amerika’ya Dönüş", "Kısa ziyaretin ardından Amerika'daki yeni işinin başına geçme.", "CEFR B2", "Welcome back to New York, {name}! Ready to launch your venture?", "Uncle Sam")
                )
            ),
            StoryChapter(
                id = "ch_8",
                chapterNumber = 8,
                rawTitle = "Bölüm 8 – {name}’in Hayatında Dönüm Noktası",
                description = "{name}'in işini büyütmesi, stadyum maçı heyecanı ve Jessica'ya doğum günü partisi.",
                cefrLevel = "CEFR B2-C1",
                scenes = listOf(
                    StoryScene("s_8_1", "ch_8", 1, "Jessica ile Sinema", "Romantik komedi filmi sonrası kahve sohbeti.", "CEFR B2", "That plot twist in the ending was hilarious, wasn't it {name}?", "Jessica"),
                    StoryScene("s_8_2", "ch_8", 2, "Komşuyla Arkadaşlığı İlerletme", "Alex ile evde pizza yapıp sohbet etme.", "CEFR B2", "Your homemade pizza sauce beats any takeaway in Brooklyn, {name}!", "Neighbor Alex"),
                    StoryScene("s_8_3", "ch_8", 3, "Komşuyla Maça Gitme", "Yankee Stadyumu'nda beysbol maçı izleyip tezahürat öğrenme.", "CEFR B2", "Here come the hot dogs! Get ready to cheer for the home team!", "Neighbor Alex"),
                    StoryScene("s_8_4", "ch_8", 4, "Girişimi İçin İlk Kargoların Gelmesi", "İlk 500 paket ürünün depoya inmesi sevinci.", "CEFR B2", "Sign here for 10 boxes of imported coffee products, sir!", "Delivery Driver"),
                    StoryScene("s_8_5", "ch_8", 5, "İşini Büyütme", "Web sitesinde ilk 100 siparişe ulaşma kutlaması.", "CEFR C1", "Look at the analytics board! 100 sales in 48 hours! Outstanding execution!", "Mike"),
                    StoryScene("s_8_6", "ch_8", 6, "Duygularını Sorgulama", "Alex ile dertleşirken Jessica'ya olan hislerini itiraf etme.", "CEFR B2", "{name}, be honest with yourself... you really like Jessica, don't you?", "Neighbor Alex"),
                    StoryScene("s_8_7", "ch_8", 7, "Jessica’ya Doğum Günü Partisi", "Sürpriz doğum günü organizasyonunda konukları karşılama.", "CEFR B2", "Surprise! Happy Birthday Jessica! {name} organized everything for you!", "Party Guests")
                )
            ),
            StoryChapter(
                id = "ch_9",
                chapterNumber = 9,
                rawTitle = "Bölüm 9 – {name}’in Hastane Maceraları",
                description = "{name}'in acil hastane durumları, doktor randevusu ve özel müşteriler.",
                cefrLevel = "CEFR B2-C1",
                scenes = listOf(
                    StoryScene("s_9_1", "ch_9", 1, "Doğum Günü Sürprizi Teşekkürü", "Jessica'nın baş başa içten teşekkür konuşması.", "CEFR B2", "{name}, that was the most memorable birthday of my life. Thank you so much!", "Jessica"),
                    StoryScene("s_9_2", "ch_9", 2, "Amcasına Borcunu Ödeme", "Tüm yatırılan parayı amcasına tam olarak teslim etme.", "CEFR C1", "{name}, seeing you succeed with your own earnings makes me so proud.", "Uncle Sam"),
                    StoryScene("s_9_3", "ch_9", 3, "Kaza Geçiren Kuzen İçin Taksi", "Eric'in hafif bileğini burkması üzerine aceleyle hastaneye yetişme.", "CEFR B2", "Driver, please take us to Mount Sinai Emergency entrance as quickly as possible!", "Taxi Driver"),
                    StoryScene("s_9_4", "ch_9", 4, "Hastanedeki Kuzeni Ziyaret Etme", "Acil serviste doktorla konuşup kuzenin durumunu öğrenme.", "CEFR B2", "He has a minor sprain. No fracture detected on the X-ray.", "Emergency Doctor"),
                    StoryScene("s_9_5", "ch_9", 5, "Ünlü Müşteri Sürprizi", "İş yerine ünlü bir Broadway oyuncusunun gelmesi.", "CEFR C1", "Hello! I heard wonderful things about your specialty coffee blend!", "Broadway Actor"),
                    StoryScene("s_9_6", "ch_9", 6, "Hastalanma ve Doktor Görüşmesi", "{name}'in gribal enfeksiyon nedeniyle kliniğe gitmesi.", "CEFR B2", "Let me check your throat and temperature. Have you had any chills?", "Clinic Doctor"),
                    StoryScene("s_9_7", "ch_9", 7, "Tedavi Sonrası Kontrol", "İlaçlar sonrası iyileşme muayenesi.", "CEFR B2", "Your fever is completely gone. You can return to work tomorrow!", "Clinic Doctor"),
                    StoryScene("s_9_8", "ch_9", 8, "Jessica’nın Diş Hekimi Randevusu", "Diş randevusuna refakat etme.", "CEFR B2", "Thank you for holding my hand in the waiting room, {name}!", "Jessica")
                )
            ),
            StoryChapter(
                id = "ch_10",
                chapterNumber = 10,
                rawTitle = "Bölüm 10 – {name}’in Yayın Heyecanı",
                description = "{name}'in TV programına çıkması, mezuniyet balosu ve unutulmaz anlar.",
                cefrLevel = "CEFR C1",
                scenes = listOf(
                    StoryScene("s_10_1", "ch_10", 1, "Evde Hasta Ziyareti", "Jessica'nın {name}'e evde çorba getirmesi.", "CEFR B2", "I made hot chicken soup for you, {name}! Drink this and rest up!", "Jessica"),
                    StoryScene("s_10_2", "ch_10", 2, "Döviz Bozdurma", "Ailesine para göndermek için bankada işlem yaptırma.", "CEFR B2", "What is today's exchange rate for international wire transfers?", "Bank Teller"),
                    StoryScene("s_10_3", "ch_10", 3, "Kargo Gönderme", "Postahanede paket tartarıp hediye kargolama.", "CEFR B2", "Is this express or standard international shipping?", "Postal Worker"),
                    StoryScene("s_10_4", "ch_10", 4, "Talk-Show Sunucusu İle Görüşme", "TV sunucusunun canlı yayın programı daveti.", "CEFR C1", "{name}! Your immigrant success story is inspiring! We want you on our morning show!", "Talk Show Producer"),
                    StoryScene("s_10_5", "ch_10", 5, "Kaybolan Eşya Arayışı", "Stüdyoda kaybolan şanslı saatini arama.", "CEFR B2", "Has anyone seen a gold wristwatch in Green Room B?", "Studio Assistant"),
                    StoryScene("s_10_6", "ch_10", 6, "Eski Arkadaştan Tebrik Telefonu", "TV'de kendisini gören çocukluk arkadaşının tebrik araması.", "CEFR B2", "Bro!! I saw you on American television! You were amazing!!", "Old Friend Can"),
                    StoryScene("s_10_7", "ch_10", 7, "Gelecek Hakkında TV Sohbeti", "Canlı yayında ilham verici İngilizce konuşma.", "CEFR C1", "Welcome {name}! Tell our 5 million viewers: what is the secret to learning English fast?", "TV Host"),
                    StoryScene("s_10_8", "ch_10", 8, "Çekim Ekibinin Gelmesi", "Dükkanda tanıtım belgeseli çekilmesi.", "CEFR C1", "Rolling camera in 3, 2, 1... speak naturally about your products!", "Documentary Director"),
                    StoryScene("s_10_9", "ch_10", 9, "Mezuniyet Balosu", "Jessica ile şık baloda dans etme ve şık kıyafetler.", "CEFR C1", "You look incredible in that tuxedo, {name}. Ready for our slow dance?", "Jessica"),
                    StoryScene("s_10_10", "ch_10", 10, "Çok Özel Bir Gün", "{name}'in Amerika'daki serüveninin büyük kutlaması.", "CEFR C1", "To {name}! For turning dreams into reality with courage and hard work!", "Uncle Sam & Friends")
                )
            ),
            StoryChapter(
                id = "ch_11",
                chapterNumber = 11,
                rawTitle = "Bölüm 11 – Genel Tekrar & Ustalık",
                description = "Tamamlanan tüm bölümlerden öğrenilen zayıf kelimeler, gramer konuları ve pratik özet diyaloglar.",
                cefrLevel = "CEFR B1-C1",
                scenes = listOf(
                    StoryScene("s_11_1", "ch_11", 1, "Gelişmiş İş & Sosyal Tekrar", "Önceki bölümlerde zorlandığınız kelimelerle karma diyalog.", "CEFR B2", "Welcome to your personal review session! Let's test your vocabulary in context.", "Coach"),
                    StoryScene("s_11_2", "ch_11", 2, "Bütünleşik Gramer Ustalaşma", "Geçmiş zaman, şartlı cümleler ve kibar rica yapılarının tekrarı.", "CEFR B2", "Great! Let's practice using past perfect and polite request structures dynamically.", "Coach")
                )
            )
        )
    }

    val extraStudyModules = listOf(
        ExtraStudyModule(
            id = "mod_daily_words",
            title = "Günlük Hayatta En Çok Kullanılan Kelimeler",
            subtitle = "Sık karşılaşılan 100 temel ifadenin diyalog icinde kullanımı",
            iconEmoji = "🔤",
            cefrLevel = "CEFR A1-A2",
            description = "Günlük hayatta en sık kullanılan kelimeleri pratik yaparak pekiştirin.",
            targetVocabulary = listOf("Available", "Convenient", "Recommend", "Appreciate", "Mind"),
            starterPrompt = "Let's practice everyday English vocabulary! I will use common words, and you respond naturally."
        ),
        ExtraStudyModule(
            id = "mod_top_250_words",
            title = "En Sık Kullanılan 250 İngilizce Kelime",
            subtitle = "Akıcılığın kilit noktası olan 250 kelime ile diyalog",
            iconEmoji = "📚",
            cefrLevel = "CEFR A2-B1",
            description = "İngilizce konuşurken en çok ihtiyacınız olacak 250 kelimelik altın liste.",
            targetVocabulary = listOf("Essential", "Frequent", "Opportunity", "Suggest", "Consider"),
            starterPrompt = "Welcome to the 250 High-Frequency Words Challenge! Tell me about your week using new expressions."
        ),
        ExtraStudyModule(
            id = "mod_visa_interview",
            title = "Vize Başvuru Mülakatı (Visa Interview)",
            subtitle = "Konsolosluk mülakatında sorulan sorular ve özgüvenli cevaplar",
            iconEmoji = "🛂",
            cefrLevel = "CEFR B1-B2",
            description = "Vize memurunun seyahat amacı, finansal durum ve geri dönüş garantisi sorularını yanıtlayın.",
            targetVocabulary = listOf("Sponsorship", "Ties to home country", "Itinerary", "Proof of funds", "Intent"),
            starterPrompt = "Good day. I am the Consular Officer. State your full name and the purpose of your visa application."
        ),
        ExtraStudyModule(
            id = "mod_job_patterns",
            title = "İş Mülakatı Kalıpları (Job Patterns)",
            subtitle = "Profesyonel mülakat kalıpları ve kendinizi ifade etme",
            iconEmoji = "👔",
            cefrLevel = "CEFR B2-C1",
            description = "Mülakatlarda fark yaratan profesyonel ifade kalıpları.",
            targetVocabulary = listOf("Spearhead", "Optimize", "Cross-functional", "Key achievement", "Leverage"),
            starterPrompt = "Let's refine your professional interview phrasing. How do you describe your key achievements?"
        ),
        ExtraStudyModule(
            id = "mod_daily_patterns",
            title = "Günlük Konuşma Kalıpları (Daily Idioms)",
            subtitle = "Anadili İngilizce olanlar gibi konuşmanızı sağlayacak deyimler",
            iconEmoji = "💬",
            cefrLevel = "CEFR B1-B2",
            description = "Sık kullanılan deyimler, phrasal verb'ler ve slang ifadeler.",
            targetVocabulary = listOf("Break the ice", "Call it a day", "Hit the spot", "Out of the blue", "Piece of cake"),
            starterPrompt = "Let's practice natural daily idioms! Have you ever had a situation happen 'out of the blue'?"
        ),
        ExtraStudyModule(
            id = "mod_essay_writing",
            title = "Essay & Yazılı Anlatım Çalışmaları",
            subtitle = "Akademik ve profesyonel yazım dili, bağlaçlar ve paragraf yapısı",
            iconEmoji = "✍️",
            cefrLevel = "CEFR B2-C1",
            description = "Görüş belirtme, fikir savunma ve akıcı paragraf oluşturma pratiği.",
            targetVocabulary = listOf("Furthermore", "On the contrary", "Consequently", "In conclusion", "Arguments"),
            starterPrompt = "Welcome to Written & Essay Practice! Share your perspective on a topic, and I will coach your structure and connectors."
        )
    )
}
