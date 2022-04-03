package com.android.android_github_api.ui.main.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.android_github_api.HiltTestActivity
import com.android.android_github_api.R
import com.android.android_github_api.databinding.FragmentWebViewBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class WebViewFragment :
    Fragment() {

    private var _binding: FragmentWebViewBinding? = null
    private val binding: FragmentWebViewBinding get() = _binding!!

    private val args: WebViewFragmentArgs by navArgs()

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_web_view, container, false)

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallback)


        binding.webView.webViewClient = MyWebClient()
        binding.webView.settings.setSupportZoom(true)
        binding.webView.settings.supportMultipleWindows()

        binding.webView.loadUrl(args.url)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (args.title != null) {
            findNavController().currentDestination?.label = "${args.title}"
        }
        if (activity is HiltTestActivity) {
            (activity as HiltTestActivity).setupActionBar(binding.toolbar.root)
        } else {
            (activity as MainActivity).setupActionBar(binding.toolbar.root)
        }
    }

    override fun onDestroyView() {
        onBackPressedCallback.remove()
        super.onDestroyView()
    }
}

class MyWebClient : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {

        view.loadUrl(url)
        return true

    }
}
