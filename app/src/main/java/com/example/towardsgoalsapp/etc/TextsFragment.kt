package com.example.towardsgoalsapp.etc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.towardsgoalsapp.Constants
import com.example.towardsgoalsapp.Constants.Companion.EMPTY_STRING
import com.example.towardsgoalsapp.R
import java.util.concurrent.atomic.AtomicBoolean

abstract class TextsViewModel : ViewModel() {

    val nameOfData: MutableLiveData<String> = MutableLiveData(Constants.EMPTY_STRING)

    val descriptionOfData: MutableLiveData<String> = MutableLiveData(Constants.EMPTY_STRING)

    val addMainDataDenier = BooleanWorkDenier()
    var addedAnyData: Boolean = false
        protected set

    abstract fun putTexts(
        newName: String?,
        newDescription: String?
    )

    abstract suspend fun saveMainData() : Boolean
}

class TextsFragment : Fragment() {

    companion object {
        const val LOG_TAG = "TXF"
        const val EDITABLE = "tfeditable"
        const val INHERIT_FROM_CLASS_NUMBER = "tfifcn"

        fun newInstance(editable: Boolean, classNum: Int): TextsFragment {
            return TextsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(EDITABLE, editable)
                    putInt(INHERIT_FROM_CLASS_NUMBER, classNum)
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
            inflater.inflate(R.layout.fragment_edit_texts, container, false)
        }
        else {
            inflater.inflate(R.layout.fragment_view_texts, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.run {
            val cn = getInt(
                INHERIT_FROM_CLASS_NUMBER,
                Constants.viewModelClassToNumber[TextsViewModel::class.java]!!
            )
            val c = Constants.numberOfClassToViewModelClass[cn]
            c?.run {
                viewModel = ViewModelProvider(requireActivity())[c] as TextsViewModel
            }
        }

        viewModel.nameOfData.observe(viewLifecycleOwner) {

            if (viewModel.nameOfData.value == null || viewModel.descriptionOfData.value == null
                || ! isVisible)
                return@observe

            if (isEdit) {
                view.findViewById<EditText>(R.id.nameEditText).setText(viewModel.nameOfData.value)
            }
            else {
                view.findViewById<TextView>(R.id.nameTextView)
                    .apply { text = viewModel.nameOfData.value }
            }
        }

        viewModel.descriptionOfData.observe(viewLifecycleOwner) {
            if (viewModel.nameOfData.value == null || viewModel.descriptionOfData.value == null
                || ! isVisible)
                return@observe

            if (isEdit) {
                view.findViewById<EditText>(R.id.descriptionEditText)
                    .setText(viewModel.descriptionOfData.value)
            }
            else {
                view.findViewById<TextView>(R.id.descriptionTextView)
                    .apply { text = viewModel.descriptionOfData.value }
            }
        }

    }

    override fun onPause() {
        if (isEdit) {
            val nameText =
                view?.findViewById<EditText>(R.id.nameEditText)?.text.toString()
            val dEditText = view?.findViewById<EditText>(R.id.descriptionEditText)?.text.toString()
            if ( (nameText != EMPTY_STRING) || (dEditText != EMPTY_STRING) ) {
                viewModel.putTexts(nameText, dEditText)
            }
        }
        super.onPause()
    }
}

