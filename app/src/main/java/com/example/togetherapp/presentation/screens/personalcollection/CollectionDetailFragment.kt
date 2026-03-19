package com.example.togetherapp.presentation.screens.personalcollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.togetherapp.databinding.FragmentCollectionDetailBinding

class CollectionDetailFragment : Fragment() {

    private var _binding: FragmentCollectionDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получить ID подборки из аргументов
        val collectionId = arguments?.getInt("collectionId") ?: 0

        // Загрузить данные подборки по ID
        loadCollectionDetails(collectionId)
    }

    private fun loadCollectionDetails(collectionId: Int) {
        // TODO: Загрузить данные подборки из БД
        binding.tvCollectionTitle.text = "Название подборки"
        binding.tvCollectionDescription.text = "Описание подборки"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}