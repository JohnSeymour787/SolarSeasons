package com.johnseymour.solarseasons.apiKeySetup

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.johnseymour.solarseasons.R
import kotlinx.android.synthetic.main.api_key_fragment.*

class APIKeyFragment : Fragment()
{
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.api_key_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        viewPager.adapter = APIKeyTutorialPagerAdapter(this)
    }

    companion object
    {
        fun newInstance() = APIKeyFragment()
    }
}