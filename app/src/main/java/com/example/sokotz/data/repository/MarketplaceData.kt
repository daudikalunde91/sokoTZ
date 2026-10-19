package com.example.sokotz.data.repository

import com.example.sokotz.data.model.Category

object MarketplaceData {

    val regions = listOf(
        "Tanzania Nzima",
        "Dar es Salaam",
        "Arusha",
        "Mwanza",
        "Dodoma",
        "Zanzibar",
        "Mbeya",
        "Morogoro",
        "Tanga",
        "Kilimanjaro"
    )

    val categories = listOf(
        Category(
            id = "cat_1",
            slug = "kilimo",
            nameSw = "Kilimo & Mazao",
            nameEn = "Agriculture & Crops",
            iconName = "agriculture",
            listingCount = 0
        ),
        Category(
            id = "cat_2",
            slug = "elektroniki",
            nameSw = "Elektroniki & Vifaa",
            nameEn = "Electronics & Gadgets",
            iconName = "devices",
            listingCount = 0
        ),
        Category(
            id = "cat_3",
            slug = "mafundi",
            nameSw = "Mafundi & Ujenzi",
            nameEn = "Contractors & Builders",
            iconName = "build",
            listingCount = 0
        ),
        Category(
            id = "cat_4",
            slug = "mitindo",
            nameSw = "Mitindo & Nguo",
            nameEn = "Fashion & Apparel",
            iconName = "checkroom",
            listingCount = 0
        ),
        Category(
            id = "cat_5",
            slug = "vyakula",
            nameSw = "Vyakula & Mikahawa",
            nameEn = "Food & Restaurants",
            iconName = "restaurant",
            listingCount = 0
        ),
        Category(
            id = "cat_6",
            slug = "usafiri",
            nameSw = "Usafiri & Magari",
            nameEn = "Transport & Automotive",
            iconName = "directions_car",
            listingCount = 0
        ),
        Category(
            id = "cat_7",
            slug = "huduma",
            nameSw = "Huduma za Kitaalamu",
            nameEn = "Professional Services",
            iconName = "business_center",
            listingCount = 0
        )
    )
}
