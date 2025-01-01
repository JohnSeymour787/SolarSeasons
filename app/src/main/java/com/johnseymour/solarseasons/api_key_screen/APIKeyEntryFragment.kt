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
import com.johnseymour.solarseasons.databinding.FragmentApiKeyEntryBinding

class APIKeyEntryFragment : Fragment()
{
    private lateinit var viewModel: APIKeyEntryViewModel
    private var _binding: FragmentApiKeyEntryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        _binding = FragmentApiKeyEntryBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[APIKeyEntryViewModel::class.java]

        binding.apiKeyEntry.setOnEditorActionListener()
        { _, actionID, _ ->
            if (actionID == EditorInfo.IME_ACTION_DONE)
            {
                apiKeySubmitted()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        binding.apiKeyEntry.doOnTextChanged()
        { text, _, _, _ ->
            viewModel.apiKey = text?.toString() ?: ""
        }

        binding.doneButton.setOnClickListener()
        {
            apiKeySubmitted()
        }

        binding.launchAppButton.setOnClickListener()
        {
            setFragmentResult(LAUNCH_MAIN_APP_FRAGMENT_KEY, bundleOf())
        }

        if (viewModel.keySaved)
        {
            binding.launchAppButton.visibility = View.VISIBLE
            binding.apiKeySuccessMessage.visibility = View.VISIBLE
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
                ?.hideSoftInputFromWindow(binding.apiKeyEntry.windowToken, 0)

            binding.launchAppButton.visibility = View.VISIBLE
            binding.apiKeySuccessMessage.visibility = View.VISIBLE

            viewModel.keySaved = true
        }
        else
        {
            binding.apiKeyEntry.error = resources.getText(errorMessageID)
            viewModel.keySaved = false
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }

    companion object
    {
        fun newInstance() = APIKeyEntryFragment()

        const val LAUNCH_MAIN_APP_FRAGMENT_KEY = "launch_main_app_fragment_key"
    }
}