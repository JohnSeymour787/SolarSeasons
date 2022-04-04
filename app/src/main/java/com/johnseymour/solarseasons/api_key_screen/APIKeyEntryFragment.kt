package com.johnseymour.solarseasons.api_key_screen

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.R
import kotlinx.android.synthetic.main.fragment_api_key_entry.*

class APIKeyEntryFragment : Fragment()
{
    private lateinit var viewModel: APIKeyEntryViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_api_key_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(APIKeyEntryViewModel::class.java)

        apiKeyEntry.setOnEditorActionListener()
        { _, actionID, _ ->
            if (actionID == EditorInfo.IME_ACTION_DONE)
            {
                apiKeySubmitted()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        apiKeyEntry.doOnTextChanged()
        { text, _, _, _ ->
            viewModel.apiKey = text?.toString() ?: ""
        }

        doneButton.setOnClickListener()
        {
            apiKeySubmitted()
        }

        launchAppButton.setOnClickListener()
        {
            setFragmentResult(LAUNCH_MAIN_APP_FRAGMENT_KEY, bundleOf())
        }

        if (viewModel.keySaved)
        {
            launchAppButton.visibility = View.VISIBLE
            apiKeySuccessMessage.visibility = View.VISIBLE
        }
    }

    private fun apiKeySubmitted()
    {
        val errorMessageID = viewModel.validateAPIKey()

        if (errorMessageID == null)
        {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit().putString(Constants.SharedPreferences.API_KEY, viewModel.apiKey)
                .apply()

            Toast.makeText(requireContext(), R.string.api_key_fragment_page_4_key_added_success_toast, Toast.LENGTH_SHORT).show()

            // Hide keyboard
            (activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(apiKeyEntry.windowToken, 0)

            launchAppButton.visibility = View.VISIBLE
            apiKeySuccessMessage.visibility = View.VISIBLE

            viewModel.keySaved = true
        }
        else
        {
            apiKeyEntry.error = resources.getText(errorMessageID)
            viewModel.keySaved = false
        }
    }

    companion object
    {
        fun newInstance() = APIKeyEntryFragment()

        const val LAUNCH_MAIN_APP_FRAGMENT_KEY = "launch_main_app_fragment_key"
    }
}