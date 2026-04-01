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
import com.example.togetherapp.domain.models.CollectionModel
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class CollectionInsertDto(
    val title: String,
    val description: String? = null,
    val user_id: Int,
    val access_type: String
)

class CreateCollectionFragment : Fragment() {

    private var _binding: CreateCollectionScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "CreateCollectionFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CreateCollectionScreenBinding.inflate(inflater, container, false)
        sessionManager = SessionManager.getInstance(requireContext())

        Log.i(TAG, "SessionManager initialized, userId=${sessionManager.getUserId()}")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.radioPrivate.isChecked = true

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.saveCollectionButton.setOnClickListener {
            binding.nameTextInputLayout.error = null

            val name = binding.nameEditText.text?.toString().orEmpty().trim()
            val description = binding.descriptionEditText.text?.toString()?.trim().orEmpty()
            val accessType = getSelectedAccessType()

            if (!validateInput(name, accessType)) return@setOnClickListener

            val userId = sessionManager.getUserId()
            if (userId == -1) {
                Toast.makeText(
                    requireContext(),
                    "Пользователь не найден",
                    Toast.LENGTH_SHORT
                ).show()
                Log.w(TAG, "User not found in SessionManager")
                return@setOnClickListener
            }

            saveCollection(
                name = name,
                description = description.ifBlank { null },
                userId = userId,
                accessType = accessType
            )
        }

        binding.addPlaceButton.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Сначала сохраните подборку, затем добавьте места",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getSelectedAccessType(): String {
        return when (binding.accessTypeRadioGroup.checkedRadioButtonId) {
            binding.radioPrivate.id -> "private"
            binding.radioPublic.id -> "public"
            binding.radioFriends.id -> "friends"
            else -> ""
        }
    }

    private fun validateInput(name: String, accessType: String): Boolean {
        if (name.isBlank()) {
            binding.nameTextInputLayout.error = "Название не может быть пустым"
            return false
        }

        if (accessType.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Выберите тип подборки",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun saveCollection(
        name: String,
        description: String?,
        userId: Int,
        accessType: String
    ) {
        lifecycleScope.launch {
            setLoading(true)

            try {
                val normalizedTitle = name.trim().lowercase()

                val existingCollections = SupabaseClient.supabase
                    .from("collections")
                    .select()
                    .decodeList<CollectionModel>()

                val alreadyExists = existingCollections.any { collection ->
                    collection.user_id == userId &&
                            collection.title.trim().lowercase() == normalizedTitle
                }

                if (alreadyExists) {
                    binding.nameTextInputLayout.error =
                        "Подборка с таким названием уже существует"
                    return@launch
                }

                val collectionDto = CollectionInsertDto(
                    title = name.trim(),
                    description = description,
                    user_id = userId,
                    access_type = accessType
                )

                Log.i(TAG, "Saving collection: $collectionDto")

                SupabaseClient.supabase
                    .from("collections")
                    .insert(collectionDto)

                Log.i(TAG, "Collection saved successfully")

                Toast.makeText(
                    requireContext(),
                    "Подборка сохранена",
                    Toast.LENGTH_SHORT
                ).show()

                findNavController().previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("refresh_collections", true)

                findNavController().popBackStack()

            } catch (e: Exception) {
                Log.e(TAG, "Error saving collection", e)

                Toast.makeText(
                    requireContext(),
                    "Ошибка при сохранении: ${e.message ?: "неизвестная ошибка"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                if (_binding != null) {
                    setLoading(false)
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        val enabled = !isLoading

        binding.saveCollectionButton.isEnabled = enabled
        binding.addPlaceButton.isEnabled = enabled
        binding.nameEditText.isEnabled = enabled
        binding.descriptionEditText.isEnabled = enabled
        binding.radioPrivate.isEnabled = enabled
        binding.radioPublic.isEnabled = enabled
        binding.radioFriends.isEnabled = enabled

        // кнопка назад
        binding.btnBack.isEnabled = enabled

        // можно ещё визуально затемнять кнопку
        binding.saveCollectionButton.alpha = if (enabled) 1f else 0.5f
        binding.addPlaceButton.alpha = if (enabled) 1f else 0.5f
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.i(TAG, "Binding cleared")
    }
}