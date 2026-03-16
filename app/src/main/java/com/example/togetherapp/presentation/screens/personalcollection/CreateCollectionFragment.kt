package com.example.togetherapp.presentation.screens.personalcollection

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.remote.SupabaseClient
import com.example.togetherapp.databinding.CreateCollectionScreenBinding
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import io.github.jan.supabase.postgrest.from

@Serializable
data class CollectionInsertDto(
    val title: String,
    val description: String? = null,
    val user_id: Int
)

class CreateCollectionFragment : Fragment() {

    private var _binding: CreateCollectionScreenBinding? = null
    private val binding get() = _binding!!

    // SessionManager singleton
    private lateinit var sessionManager: SessionManager
    private val TAG = "CreateCollectionFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CreateCollectionScreenBinding.inflate(inflater, container, false)

        // Инициализация singleton SessionManager
        sessionManager = SessionManager.getInstance(requireContext())
        Log.i(TAG, "onCreateView: SessionManager initialized")
        Log.i(TAG, "onCreateView: current userId = ${sessionManager.getUserId()}")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveCollectionButton.setOnClickListener {
            val name = binding.nameEditText.text?.toString().orEmpty()
            val description = binding.descriptionEditText.text?.toString()

            if (name.isBlank()) {
                binding.nameTextInputLayout.error = "Название не может быть пустым"
                return@setOnClickListener
            } else {
                binding.nameTextInputLayout.error = null
            }

            val userId = sessionManager.getUserId()
            Log.i(TAG, "Attempt to save collection for userId=$userId")

            if (userId == -1) {
                Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "User not found in SessionManager!")
                return@setOnClickListener
            }

            saveCollection(name, description, userId)
        }
    }

    private fun saveCollection(name: String, description: String?, userId: Int) {
        lifecycleScope.launch {
            try {
                val collectionDto = CollectionInsertDto(
                    title = name,
                    description = description,
                    user_id = userId
                )

                Log.i(TAG, "Saving collection: $collectionDto")

                val result = SupabaseClient.supabase
                    .from("collections")
                    .insert(collectionDto) // передаем DTO вместо mapOf

                Log.i(TAG, "Collection saved successfully: $result")
                Toast.makeText(requireContext(), "Коллекция сохранена!", Toast.LENGTH_SHORT).show()

                // Обновляем предыдущий фрагмент
                findNavController().previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("refresh_collections", true)

                findNavController().popBackStack()

            } catch (e: Exception) {
                Log.e(TAG, "Error saving collection", e)
                Toast.makeText(requireContext(), "Ошибка при сохранении: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.i(TAG, "onDestroyView: binding cleared")
    }
}