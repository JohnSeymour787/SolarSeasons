package com.johnseymour.solarseasons.apiKeySetup

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
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

        viewPager.reduceDragSensitivity()
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

    companion object
    {
        fun newInstance() = APIKeyFragment()
    }
}