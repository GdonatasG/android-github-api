package com.android.android_github_api.ui.main.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.android.android_github_api.HiltTestActivity
import com.android.android_github_api.R
import com.android.android_github_api.databinding.FragmentHomeBinding
import com.android.android_github_api.ui.main.viewmodel.HomeViewModel
import com.android.android_github_api.utils.Constants
import com.android.android_github_api.utils.Status
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class HomeFragment @Inject constructor(
    val glide: RequestManager,
    private val viewModelFactory: ViewModelProvider.Factory
) :
    Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding get() = _binding!!

    val viewModel: HomeViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HomeFragment", "onCreate: HomeFragment")

        viewModel.apply {
            setName(Constants.GITHUB_NAME)
            loadGithubUser()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        if (activity is HiltTestActivity) {
            (activity as HiltTestActivity).setupActionBar(binding.toolbar.root)
        } else {
            (activity as MainActivity).setupActionBar(binding.toolbar.root)
        }
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.fragment = this
        setupObservers()
        binding.srLoaded.setOnRefreshListener {
            viewModel.loadGithubUser(isRefresh = true)
        }
        return binding.root
    }

    private fun setupObservers() {
        viewModel.getGithubUser.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        if (binding.srLoaded.isRefreshing) {
                            requireActivity().runOnUiThread {
                                binding.srLoaded.isRefreshing = false
                            }
                        }
                    }
                    Status.ERROR -> {
                        if (binding.srLoaded.isRefreshing) {
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    resource.message,
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                                binding.srLoaded.isRefreshing = false
                            }
                        }
                    }
                    Status.LOADING -> {
                        /* NO-OPERATION */
                    }
                    else -> {
                        /* NO-OPERATION */
                    }
                }
            }
        })
        // to active SwipeRefreshLayout indicator when recreating view
        // if viewModel is still refreshing data
        viewModel.isRefreshing.observe(viewLifecycleOwner, { isRefreshing ->
            if (isRefreshing) {
                if (!binding.srLoaded.isRefreshing) {
                    requireActivity().runOnUiThread {
                        binding.srLoaded.isRefreshing = true
                    }
                }
            }
        })
    }

    fun goToSearchPage(view: View) {
        viewModel.getName.apply {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToSearchFragment(
                    githubName = this
                )
            )
        }
    }

    fun goToWebViewPage(view: View) {
        viewModel.getName.apply {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToWebViewFragment(
                    url = "https://github.com/${this}/",
                    title = "$this profile"
                )
            )
        }
    }

    fun mainRetry(view: View) {
        viewModel.loadGithubUser()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}