package com.johnseymour.solarseasons.api_key_screen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.johnseymour.solarseasons.databinding.ApiKeyFragmentBinding

class APIKeyFragment : Fragment()
{
    private var _binding: ApiKeyFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        _binding = ApiKeyFragmentBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.viewPager.adapter = APIKeyTutorialPagerAdapter(this)

        binding.viewPager.reduceDragSensitivity()

        childFragmentManager.setFragmentResultListener(APIKeyEntryFragment.LAUNCH_MAIN_APP_FRAGMENT_KEY, this)
        { requestKey, bundle ->
            parentFragmentManager.setFragmentResult(requestKey, bundle)
        }
    }

    /**
     * Increases the "resistance" of the horizontal scrolling of the viewpager in order to allow
     *  easier use of the WebView on some pages without accidentally scrolling to other pages.
     *
     *  @author https://gist.github.com/AlShevelev/ea43096e8f66b0ec45a0ec0dd1e8cacc
     */
    private fun ViewPager2.reduceDragSensitivity()
    {
        val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recyclerViewField.isAccessible = true
        val recyclerView = recyclerViewField.get(this) as RecyclerView

        val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
        touchSlopField.isAccessible = true
        val touchSlop = touchSlopField.get(recyclerView) as Int
        touchSlopField.set(recyclerView, touchSlop*2)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        _binding = null
    }

    companion object
    {
        fun newInstance() = APIKeyFragment()
    }
}