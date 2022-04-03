package com.android.android_github_api.ui.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.android_github_api.data.model.github.GithubRepo
import com.android.android_github_api.databinding.GithubRepoLayoutBinding
import com.android.android_github_api.databinding.ItemLoadingBinding
import javax.inject.Inject


class GithubReposAdapter @Inject constructor() :

    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<GithubRepo?>() {
        override fun areItemsTheSame(oldItem: GithubRepo, newItem: GithubRepo): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: GithubRepo, newItem: GithubRepo): Boolean {
            return oldItem.description == newItem.description
        }

    }

    private val differ = AsyncListDiffer(this, diffCallback)

    var repos: List<GithubRepo?>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    private val VIEW_TYPE_ITEM = 0
    private val VIEW_TYPE_LOADING = 1

    override fun getItemViewType(position: Int): Int {
        val item = repos[position]
        return if (item == null) {
            VIEW_TYPE_LOADING
        } else {
            VIEW_TYPE_ITEM
        }
    }

    inner class ViewHolder(val binding: GithubRepoLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class LoadingViewHolder(val binding: ItemLoadingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_ITEM) {
            val binding =
                GithubRepoLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)

            return ViewHolder(binding)
        } else {
            val binding =
                ItemLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)

            return LoadingViewHolder(binding)
        }
    }

    private var onItemClickListener: ((GithubRepo) -> Unit)? = null

    fun setOnItemClickListener(listener: (GithubRepo) -> Unit) {
        onItemClickListener = listener
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            with(repos[position]) {
                holder.binding.tvName.text = this?.name
                holder.binding.tvLanguage.text = this?.language
                holder.binding.root.setOnClickListener {
                    this?.let { repo ->
                        onItemClickListener?.let { click ->
                            click(repo)
                        }
                    }

                }
            }
        } else if (holder is LoadingViewHolder) {
            showLoadingView(holder, position)
        }
    }

    private fun showLoadingView(viewHolder: LoadingViewHolder, position: Int) {
        //ProgressBar would be displayed
    }

    override fun getItemCount(): Int {
        return repos.size
    }

    fun submitRepos(newRepos: List<GithubRepo?>, commitCallback: Runnable? = null) {
        differ.submitList(newRepos, commitCallback)
    }

    fun addRepo(repo: GithubRepo?) {
        val newList: MutableList<GithubRepo?> = differ.currentList.toMutableList()
        newList.add(repo)
        submitRepos(newList)
    }

}