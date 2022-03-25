package com.johnseymour.solarseasons.apiKeySetup

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.johnseymour.solarseasons.Constants
import com.johnseymour.solarseasons.R

class APIKeyTutorialPagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment)
{
    companion object
    {
        private const val NUM_PAGES = 4
    }

    override fun getItemCount() = NUM_PAGES

    override fun createFragment(position: Int): Fragment
    {

        if (position == (NUM_PAGES - 1)) { return APIKeyEntryFragment.newInstance() }

        val bundle = bundleOf()

        when (position)
        {
            0 ->
            {
                bundle.putInt(APITutorialPageFragment.TITLE_RESOURCE_KEY, R.string.api_key_fragment_page_1_title)
                bundle.putInt(APITutorialPageFragment.EXPLANATION_RESOURCE_KEY, R.string.api_key_fragment_page_1_explanation)
            }

            1 ->
            {
                bundle.putInt(APITutorialPageFragment.TITLE_RESOURCE_KEY, R.string.api_key_fragment_page_2_3_4_title)
                bundle.putInt(APITutorialPageFragment.EXPLANATION_RESOURCE_KEY, R.string.api_key_fragment_page_2_explanation)
                bundle.putString(APITutorialPageFragment.WEB_VIEW_URL_KEY, Constants.OPEN_UV_WEBSITE_BASE_URL)
                bundle.putBoolean(APITutorialPageFragment.SHOULD_SHOW_REDIRECT_BUTTON, true)
            }

            2 ->
            {
                bundle.putInt(APITutorialPageFragment.TITLE_RESOURCE_KEY, R.string.api_key_fragment_page_2_3_4_title)
                bundle.putInt(APITutorialPageFragment.EXPLANATION_RESOURCE_KEY, R.string.api_key_fragment_page_3_explanation)
                bundle.putString(APITutorialPageFragment.WEB_VIEW_URL_KEY, Constants.OPEN_UV_WEBSITE_CONSOLE_URL)
                bundle.putBoolean(APITutorialPageFragment.SHOULD_SCROLL_WEB_VIEW_KEY, true)
                bundle.putBoolean(APITutorialPageFragment.SHOULD_SHOW_REDIRECT_BUTTON, true)
            }
        }

        return APITutorialPageFragment.newInstance(bundle)
    }
}