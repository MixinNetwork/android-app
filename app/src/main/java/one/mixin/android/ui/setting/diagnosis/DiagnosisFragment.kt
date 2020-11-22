package one.mixin.android.ui.setting.diagnosis

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import fairy.easy.httpmodel.HttpModelHelper
import fairy.easy.httpmodel.resource.HttpListener
import fairy.easy.httpmodel.resource.HttpType
import one.mixin.android.R
import one.mixin.android.databinding.FragmentDiagnosisBinding
import one.mixin.android.di.HostSelectionInterceptor.Companion.CURRENT_URL
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.util.viewBinding
import org.json.JSONObject
import timber.log.Timber
import java.util.Locale

@AndroidEntryPoint
class DiagnosisFragment : BaseFragment() {
    companion object {
        const val TAG = "DiagnosisFragment"
        fun newInstance(): DiagnosisFragment {
            return DiagnosisFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        layoutInflater.inflate(R.layout.fragment_diagnosis, container, false)

    private val binding by viewBinding(FragmentDiagnosisBinding::bind)

    private var isFinish = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleView.titleTv.setText(R.string.setting_diagnosis)
        binding.titleView.leftIb.setOnClickListener { activity?.onBackPressed() }
        binding.diagnosisRv.setHasFixedSize(true)
        val resultAdapter = ResultAdapter()
        binding.titleView.rightIb.setOnClickListener {
            context?.getClipboardManager()
                ?.setPrimaryClip(ClipData.newPlainText(null, resultAdapter.getResult()))
            toast(R.string.copy_success)
        }
        binding.diagnosisRv.adapter = resultAdapter
        val linearLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.diagnosisRv.layoutManager = linearLayoutManager
        binding.titleView.rightAnimator.displayedChild = 2
        HttpModelHelper.getInstance()
            .init(requireContext())
            .setChina(Locale.getDefault().country == Locale.CHINA.country)
            .setModelLoader(OkHttpUrlLoader())
            .setFactory()
            .addAll()
            .build()
            .startAsync(
                CURRENT_URL,
                object : HttpListener {
                    override fun onSuccess(httpType: HttpType, result: JSONObject) {
                        if (!isAdded) return
                        toast("${httpType.getName()} success")
                        resultAdapter.insertData(ResultBean(httpType.getName(), result.toString()))
                    }

                    override fun onFail(data: String) {
                        if (!isAdded) return
                        isFinish = true
                        binding.titleView.rightAnimator.displayedChild = 0
                        toast(data)
                    }

                    override fun onFinish(result: JSONObject) {
                        if (!isAdded) return
                        isFinish = true
                        binding.titleView.rightAnimator.displayedChild = 0

                        try {
                            toast("total time-consuming${result.getString("totalTime")}")
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
            )
    }
}
