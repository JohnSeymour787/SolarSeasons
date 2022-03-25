package com.johnseymour.solarseasons.apiKeySetup

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.johnseymour.solarseasons.R

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

        // TODO handle done button clicked and also same thing from the keyboard done button
    }

    companion object
    {
        fun newInstance() = APIKeyEntryFragment()
    }
}