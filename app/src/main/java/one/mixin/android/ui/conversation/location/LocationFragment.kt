package one.mixin.android.ui.conversation.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_location.*
import one.mixin.android.R
import one.mixin.android.ui.common.BaseFragment

class LocationFragment : BaseFragment() {

    companion object {
        const val TAG = "LocationFragment"

        fun newInstance(): LocationFragment {
            return LocationFragment()
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val locationViewModel: LocationViewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(LocationViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_location, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        ic_back.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }
}
