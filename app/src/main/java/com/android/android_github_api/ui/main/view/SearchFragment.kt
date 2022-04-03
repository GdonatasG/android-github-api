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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.android_github_api.HiltTestActivity
import com.android.android_github_api.R
import com.android.android_github_api.databinding.FragmentSearchBinding
import com.android.android_github_api.ui.main.adapter.GithubReposAdapter
import com.android.android_github_api.ui.main.viewmodel.SearchViewModel
import com.android.android_github_api.utils.Constants
import com.android.android_github_api.utils.EditTextUtils.textChanges
import com.android.android_github_api.utils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject


@AndroidEntryPoint
class SearchFragment @Inject constructor(
    private val githubReposAdapter: GithubReposAdapter,
    private val viewModelFactory: SearchViewModel.AssistedSearchViewModelFactory
) :
    Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding: FragmentSearchBinding get() = _binding!!

    private val args: SearchFragmentArgs by navArgs()

    private val viewModel: SearchViewModel by viewModels {
        SearchViewModel.provideFactory(viewModelFactory, args.githubName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SearchFragment", "onCreate: SearchFragment")
        viewModel.getRepos(loadingMore = false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false)
        if (activity is HiltTestActivity) {
            (activity as HiltTestActivity).setupActionBar(binding.toolbar.root)
        } else {
            (activity as MainActivity).setupActionBar(binding.toolbar.root)
        }

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.fragment = this

        binding.etGithubUser.setText(viewModel.getName)

        binding.btnGithubUserSubmit.setOnClickListener {
            viewModel.setName(binding.etGithubUser.text.toString())
        }

        setupObservers()
        setUpRvAndAdapter()

        binding.etQuery.textChanges().debounce(Constants.DEBOUNCE_TIMEOUT_MS).onEach {
            if (it != null) {
                viewModel.setQuery(it.toString())
            }
        }.launchIn(lifecycleScope)

        return binding.root
    }

    private fun setUpRvAndAdapter() {
        githubReposAdapter.setOnItemClickListener { repo ->
            findNavController().navigate(
                SearchFragmentDirections.actionSearchFragmentToWebViewFragment(
                    url = repo.html_url,
                    title = repo.name
                ),
            )
        }

        binding.rvRepos.apply {
            adapter = githubReposAdapter
            layoutManager = LinearLayoutManager(requireContext())
            val dividerItemDecoration = DividerItemDecoration(
                binding.rvRepos.context,
                (layoutManager as LinearLayoutManager).orientation
            )
            addItemDecoration(dividerItemDecoration)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if ((layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() == githubReposAdapter.itemCount - 1) {
                        if (!viewModel.isLoadingMore && viewModel.canPaginate()) {
                            viewModel.captureLoadingMore()
                            // adding loading indicator in the end of the list
                            binding.rvRepos.post {
                                githubReposAdapter.addRepo(null)
                            }
                            viewModel.getRepos(loadingMore = true)
                        }
                    }
                }
            })
        }
    }

    private fun setupObservers() {
        viewModel.githubSearchData.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        if (resource.data != null) {
                            githubReposAdapter.submitRepos(resource.data.items) {
                                // scroll to the start when resetting the pagination
                                if (viewModel.currentPageCounter == 1) {
                                    binding.rvRepos.post {
                                        binding.rvRepos.scrollToPosition(0)
                                    }
                                }
                            }
                        }
                        if (viewModel.isLoadingMore) {
                            viewModel.releaseLoadingMore()
                        }
                    }
                    Status.ERROR -> {
                        if (resource.data != null) {
                            githubReposAdapter.submitRepos(resource.data.items) {
                                // show error message when pagination was not successful
                                // also offset recycler view vertically to avoid immediate pagination after error
                                if (viewModel.isLoadingMore) {
                                    binding.rvRepos.post {
                                        binding.rvRepos.layoutManager?.offsetChildrenVertical(10)
                                        Toast.makeText(
                                            requireContext(),
                                            resource.message,
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                        // releasing isLoadingMore only after recycler view offset
                                        viewModel.releaseLoadingMore()
                                    }
                                }
                            }
                        }
                    }
                    Status.EMPTY -> {
                        if (viewModel.isLoadingMore) {
                            viewModel.releaseLoadingMore()
                        }
                    }
                    else -> {
                        /* NO-OPERATION */
                    }
                }
            }
        })
    }

    fun mainRetry(view: View) {
        viewModel.getRepos(loadingMore = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}