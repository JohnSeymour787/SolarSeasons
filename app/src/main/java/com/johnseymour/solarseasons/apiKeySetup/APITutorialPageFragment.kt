package com.johnseymour.solarseasons.apiKeySetup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.RelativeLayout
import com.johnseymour.solarseasons.R
import kotlinx.android.synthetic.main.fragment_api_tutorial_page.*

class APITutorialPageFragment : Fragment()
{
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        return inflater.inflate(R.layout.fragment_api_tutorial_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        (arguments?.getSerializable(TITLE_RESOURCE_KEY) as? Int)?.let { titleTextView.setText(it) }
        (arguments?.getSerializable(EXPLANATION_RESOURCE_KEY) as? Int)?.let { explanationTextView.setText(it) }
        arguments?.getString(WEB_VIEW_URL_KEY)?.let()
        { urlString ->
            webView.loadUrl(urlString)

            // When showing the WebView, make it be centered and the explanation text immediately below the title
            val newExplainTextParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply()
            {
                addRule(RelativeLayout.BELOW, R.id.titleTextView)
                marginStart = resources.getDimensionPixelOffset(R.dimen.margin_small) / 2
                marginEnd = resources.getDimensionPixelOffset(R.dimen.margin_small) / 2
                topMargin = resources.getDimensionPixelOffset(R.dimen.margin_small)
            }
            explanationTextView.layoutParams = newExplainTextParams

            webView.visibility = View.VISIBLE

            if (arguments?.getBoolean(SHOULD_SHOW_REDIRECT_BUTTON, false) == true)
            {
                apiKeyRedirectButton.visibility = View.VISIBLE

                apiKeyRedirectButton.setOnClickListener()
                {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString)) // Opens the browser app
                    startActivity(intent)
                }
            }

            if (arguments?.getBoolean(SHOULD_SCROLL_WEB_VIEW_KEY, false) == true)
            {
                webView.scrollY = resources.getDimensionPixelOffset(R.dimen.api_fragment_web_view_default_scroll_position)
            }
        }
    }

    companion object
    {
        const val TITLE_RESOURCE_KEY = "title_resource_key"
        const val EXPLANATION_RESOURCE_KEY = "explanation_resource_key"
        const val WEB_VIEW_URL_KEY = "web_view_url_key"
        const val SHOULD_SCROLL_WEB_VIEW_KEY = "should_scroll_web_view_key"
        const val SHOULD_SHOW_REDIRECT_BUTTON = "should_show_redirect_button"

        fun newInstance(bundle: Bundle): APITutorialPageFragment
        {
            return APITutorialPageFragment().apply { arguments = bundle }
        }
    }
}