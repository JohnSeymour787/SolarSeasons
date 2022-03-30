package com.johnseymour.solarseasons.apiKeySetup

import androidx.lifecycle.ViewModel
import com.johnseymour.solarseasons.R

class APIKeyEntryViewModel : ViewModel()
{
    var apiKey = ""
    var keySaved = false

    fun validateAPIKey(): Int?
    {
        return when
        {
            apiKey.isEmpty() -> R.string.api_key_fragment_page_4_empty_key_error

            apiKey.contains(" ", true) -> R.string.api_key_fragment_page_4_spaces_in_key_error

            else -> null
        }
    }
}