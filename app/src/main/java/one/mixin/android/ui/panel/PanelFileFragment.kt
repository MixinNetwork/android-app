package one.mixin.android.ui.panel

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_panel_file.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.ui.common.recyclerview.HeaderAdapter
import one.mixin.android.ui.panel.adapter.PanelFileAdapter
import java.io.File

class PanelFileFragment : Fragment() {

    private val fileContext = Job()

    private val adapter = PanelFileAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_panel_file, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        files_rv.layoutManager = LinearLayoutManager(requireContext())
        adapter.headerView = layoutInflater.inflate(R.layout.view_panel_file_header, files_rv, false)
        adapter.headerView?.setOnClickListener {
            onPanelFileCallback?.onOpenFileSelector()
        }
        files_rv.adapter = adapter
        adapter.onItemListener = object : HeaderAdapter.OnItemListener {
            override fun <T> onNormalItemClick(item: T) {
                onPanelFileCallback?.onFileClick(item as File)
            }
        }

        GlobalScope.launch(fileContext) {
            val dir = File(Environment.getExternalStorageDirectory().absolutePath)
            val files = dir.listFiles().filter { !it.isDirectory }.sortedBy { it.name }
            launch(Dispatchers.Main) {
                adapter.data = files
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fileContext.cancel()
    }

    var onPanelFileCallback: OnPanelFileCallback? = null

    interface OnPanelFileCallback {
        fun onFileClick(file: File)
        fun onOpenFileSelector()
    }

    companion object {
        const val TAG = "PanelFileFragment"

        fun newInstance() = PanelFileFragment()
    }
}