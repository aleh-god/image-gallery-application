package by.godevelopment.imagegalleryapplication.presentation.tableview

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import by.godevelopment.imagegalleryapplication.R
import by.godevelopment.imagegalleryapplication.commons.TAG
import by.godevelopment.imagegalleryapplication.databinding.FragmentTableViewBinding
import by.godevelopment.imagegalleryapplication.presentation.fullview.FullViewFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class TableViewFragment : Fragment() {

    companion object {
        fun newInstance() = TableViewFragment()
        private const val KEY_BUNDLE = "pass-src"
    }

    private var _binding: FragmentTableViewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TableViewViewModel by viewModels()
    private val onClick: (String) -> Any = {
        navigateToFullView(it)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTableViewBinding.inflate(inflater, container, false)
        setupUi()
        setupEvent()
        return binding.root
    }

    private fun setupUi() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect { uiState ->
                Log.i(TAG, "setupUi: launchWhenStarted isFetchingData = ${uiState.isFetchingData}")
                val swipeContainer = binding.swipeContainer
                if (!uiState.isFetchingData) {
                    Log.i(TAG, "setupUi: uiState.isFetchingData = false")
                    binding.progress.visibility = View.GONE
                    swipeContainer?.isRefreshing = false
                }
                    else binding.progress.visibility = View.VISIBLE
                val adapter = TableAdapter(onClick).apply {
                    Log.i(TAG, "setupUi: TableAdapter = ${uiState.imagesList.size}")
                    imagesList = uiState.imagesList
                }
                binding.rv.adapter = adapter
                val layoutManager =
                    if (isTablet()) GridLayoutManager(requireContext(), 3)
                        else GridLayoutManager(requireContext(), 2)
                binding.rv.layoutManager = layoutManager

                binding.swipeContainer?.apply {
                    Log.i(TAG, "setupUi: binding.swipeContainer?.apply")
                    setOnRefreshListener {
                        Log.i(TAG, "setupUi: binding.swipeContainer emptyList() and fetchImagesList()")
                        (binding.rv.adapter as TableAdapter).imagesList = emptyList()
                        viewModel.fetchImagesList()
                    }
                    setColorSchemeResources(
                        android.R.color.holo_blue_bright,
                        android.R.color.holo_green_light,
                        android.R.color.holo_orange_light,
                        android.R.color.holo_red_light
                    )
                }
            }
        }
    }

    private fun setupEvent() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiEvent.collect {
                Log.i(TAG, "setupEvent: viewModel.uiEvent.collect")
                Snackbar.make(binding.root,
                    R.string.alert_error_loading,
                    Snackbar.LENGTH_LONG
                )
                    .setAction(getString(R.string.snackbar_btn_reload))
                    { viewModel.fetchImagesList() }
                    .show()
            }
        }
    }

    private fun navigateToFullView(src: String) {
        val bundle = bundleOf(KEY_BUNDLE to src)
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            setCustomAnimations(
                R.anim.card_flip_right_in,
                R.anim.card_flip_right_out,
                R.anim.card_flip_left_in,
                R.anim.card_flip_left_out
            )
            replace<FullViewFragment>(R.id.container_view, args = bundle)
            addToBackStack(null)
        }
    }

    private fun isTablet(): Boolean {
        val screen = requireContext().resources.configuration.screenLayout
        val constMask = Configuration.SCREENLAYOUT_SIZE_MASK
        val constLarge = Configuration.SCREENLAYOUT_SIZE_LARGE
        val check = screen and constMask
        return check >= constLarge
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}