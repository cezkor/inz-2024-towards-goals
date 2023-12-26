package com.example.towardsgoalsapp.etc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.Constants.Companion.EMPTY_STRING
import com.example.towardsgoalsapp.R

abstract class TextsViewModel : ViewModel() {

    val nameOfData: MutableLiveData<String> = MutableLiveData(Constants.EMPTY_STRING)

    val descriptionOfData: MutableLiveData<String> = MutableLiveData(Constants.EMPTY_STRING)

    abstract fun updateTexts(
        newName: String?,
        newDescription: String?
    )
}

class TextsFragment : Fragment() {

    companion object {
        const val LOG_TAG = "TXF"
        const val EDITABLE = "tfeditable"

        fun newInstance(editable: Boolean): TextsFragment {
            return TextsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(EDITABLE, editable)
                }
            }
        }
    }

    private var isEdit: Boolean = false

    private lateinit var viewModel: TextsViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.run {
            isEdit = getBoolean(EDITABLE)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (isEdit) {
            inflater.inflate(R.layout.fragment_edit_texts, container, true)
        }
        else {
            inflater.inflate(R.layout.fragment_view_texts, container, true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.nameOfData.observe(viewLifecycleOwner) {
            if (isEdit) {
                val nameEdit =
                    view.findViewById<EditText>(R.id.nameEditText)
                        .apply { text.replace(0, 0, viewModel.nameOfData.value) }
            }
            else {
                val nameTV = view.findViewById<TextView>(R.id.nameTextView)
                    .apply { text = viewModel.nameOfData.value }
            }
        }

        viewModel.descriptionOfData.observe(viewLifecycleOwner) {
            if (isEdit) {
                val nameEdit =
                    view.findViewById<EditText>(R.id.nameEditText)
                        .apply { text.replace(0, 0, viewModel.nameOfData.value) }
            }
            else {
                val nameTV = view.findViewById<TextView>(R.id.nameTextView)
                    .apply { text = viewModel.nameOfData.value }
            }
        }

        if (isEdit) {

            val dEdit = view.findViewById<EditText>(R.id.descriptionEditText)
                .apply { text.replace(0, 0, viewModel.descriptionOfData.value) }
        }
        else {

            val descTV = view.findViewById<TextView>(R.id.descriptionEditText)
                .apply { text = viewModel.descriptionOfData.value }
        }
    }

    override fun onStop() {
        if (isEdit) {
            val nameText =
                view?.findViewById<EditText>(R.id.nameEditText)?.text.toString()
            val dEditText = view?.findViewById<EditText>(R.id.descriptionEditText)?.text.toString()
            if ( (nameText != EMPTY_STRING) && (dEditText != EMPTY_STRING) ) {
                viewModel.updateTexts(nameText, dEditText)
            }
        }
        super.onStop()
    }

}

