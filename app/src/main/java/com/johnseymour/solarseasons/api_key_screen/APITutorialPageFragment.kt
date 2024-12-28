package com.johnseymour.solarseasons.api_key_screen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import android.widget.RelativeLayout
import com.johnseymour.solarseasons.R
import com.johnseymour.solarseasons.databinding.FragmentApiTutorialPageBinding

class APITutorialPageFragment : Fragment()
{
    private var _binding: FragmentApiTutorialPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        _binding = FragmentApiTutorialPageBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        (arguments?.getSerializable(TITLE_RESOURCE_KEY) as? Int)?.let { binding.titleTextView.setText(it) }
        (arguments?.getSerializable(EXPLANATION_RESOURCE_KEY) as? Int)?.let { binding.explanationTextView.setText(it) }
        arguments?.getString(WEB_VIEW_URL_KEY)?.let()
        { urlString ->

            // When showing the WebView, make it be centered and the explanation text immediately below the title
            val newExplainTextParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply()
            {
                addRule(RelativeLayout.BELOW, R.id.titleTextView)
                marginStart = resources.getDimensionPixelOffset(R.dimen.margin_small) / 2
                marginEnd = resources.getDimensionPixelOffset(R.dimen.margin_small) / 2
                topMargin = resources.getDimensionPixelOffset(R.dimen.margin_small)
            }
            binding.explanationTextView.layoutParams = newExplainTextParams

            val originalWebViewClient = binding.webView.webViewClient
            binding.webView.webViewClient = object : WebViewClient()
            {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean
                {
                    return false // Preventing automatic opening of browser app when loading the web page
                }

                override fun onPageFinished(view: WebView?, url: String?)
                {
                    super.onPageFinished(view, url)

                    if (_binding?.webView != null) // Can be null if configuration change occurs
                    {
                        _binding?.webView?.webViewClient = originalWebViewClient // Will open the browser app when a button on the web page is clicked
                    }
                }
            }

            binding.webView.loadUrl(urlString)

            binding.webView.visibility = View.VISIBLE

            if (arguments?.getBoolean(SHOULD_SHOW_REDIRECT_BUTTON, false) == true)
            {
                binding.apiKeyRedirectButton.visibility = View.VISIBLE

                binding.apiKeyRedirectButton.setOnClickListener()
                {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString)) // Opens the browser app
                    startActivity(intent)
                }
            }

            if (arguments?.getBoolean(SHOULD_SCROLL_WEB_VIEW_KEY, false) == true)
            {
                binding.webView.scrollY = resources.getDimensionPixelOffset(R.dimen.api_fragment_web_view_default_scroll_position)
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
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