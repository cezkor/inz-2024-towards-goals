package com.example.towardsgoalsapp.etc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.towardsgoalsapp.R

class OneTextFragment(): Fragment() {

    companion object {
        const val LOG_TAG = "OTF"
        const val TEXT_FOR_FRAG = "tff"

        fun newInstance(text: String): OneTextFragment {
            return OneTextFragment().apply {
                arguments = Bundle().apply {
                    putString(TEXT_FOR_FRAG, text)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.run {
            val text = this.getString(TEXT_FOR_FRAG)
            text?.run { view.findViewById<TextView>(R.id.oneTextView).text = this }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_one_text, container, false)
    }

}
