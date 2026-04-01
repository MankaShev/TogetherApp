package com.example.togetherapp.presentation.screens.personalcollection

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.togetherapp.R
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.data.repository.CollectionRepositoryImpl
import com.example.togetherapp.data.repository.FriendRepositoryImpl
import com.example.togetherapp.data.repository.PlaceRepositoryImpl
import com.example.togetherapp.databinding.FragmentCollectionDetailBinding
import com.example.togetherapp.domain.models.CollectionPlaceWithPlace
import com.example.togetherapp.domain.models.SelectedPlace
import com.example.togetherapp.presentation.viewmodel.SharedMapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CollectionDetailFragment : Fragment() {

    private var _binding: FragmentCollectionDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CollectionDetailViewModel
    private lateinit var adapter: CollectionPlacesAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var sharedMapViewModel: SharedMapViewModel

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private var pendingCheckInItem: CollectionPlaceWithPlace? = null

    private var collectionId: Int = -1
    private var collectionTitle: String = ""
    private var collectionDescription: String = ""
    private var collectionAccessType: String = "private"
    private var collectionDeadlineAt: String? = null
    private var isFromExplore: Boolean = false
    private var collectionAuthorId: Int = -1
    private var collectionAuthorLogin: String = "Неизвестный автор"
    private var forceOwnerMode: Boolean = false

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineGranted || coarseGranted) {
                pendingCheckInItem?.let { item ->
                    startCheckInFlow(item)
                }
            } else {
                showToast("Без доступа к геолокации нельзя подтвердить посещение.")
            }
        }

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

        sessionManager = SessionManager.getInstance(requireContext().applicationContext)
        sharedMapViewModel = ViewModelProvider(requireActivity())[SharedMapViewModel::class.java]

        readArguments()
        bindStaticInfo()
        initViewModel()
        setupRecyclerView()
        setupObservers()
        setupCloseButton()
        setupActionButtons()
        setupRouteButton()
        setupActionButtonsVisibility()

        if (collectionId != -1) {
            viewModel.loadCollectionData(collectionId)
        } else {
            showToast("Ошибка: не найден id подборки")
        }
    }

    private fun readArguments() {
        collectionId = arguments?.getInt("collectionId", -1) ?: -1
        collectionTitle = arguments?.getString("collectionTitle").orEmpty()
        collectionDescription = arguments?.getString("collectionDescription").orEmpty()
        collectionAuthorLogin = arguments?.getString("collectionAuthor") ?: "Неизвестный автор"
        collectionAuthorId = arguments?.getInt("collectionAuthorId", -1) ?: -1
        collectionAccessType = arguments?.getString("collectionAccessType") ?: "private"
        collectionDeadlineAt = arguments?.getString("collectionDeadlineAt")
        isFromExplore = arguments?.getBoolean("isFromExplore", false) ?: false
        forceOwnerMode = arguments?.getBoolean("forceOwnerMode", false) ?: false
    }

    private fun bindStaticInfo() {
        binding.tvCollectionTitle.text = collectionTitle
        binding.tvCollectionDescription.text = collectionDescription
        binding.tvCollectionAuthor.text = collectionAuthorLogin
        binding.tvCollectionPrivacy.text = mapAccessType(collectionAccessType)
        binding.tvCollectionDeadline.text = formatDeadline(collectionDeadlineAt)
    }

    private fun initViewModel() {
        val placeRepository = PlaceRepositoryImpl()
        val friendRepository = FriendRepositoryImpl()
        val repository = CollectionRepositoryImpl(
            sessionManager = sessionManager,
            placeRepository = placeRepository,
            friendRepository = friendRepository
        )
        val factory = CollectionDetailViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CollectionDetailViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = CollectionPlacesAdapter(
            onCheckInClicked = { item ->
                onCheckInClicked(item)
            },
            onRemoveClicked = { linkId ->
                if (isOwnCollection() && collectionId != -1) {
                    viewModel.removePlace(linkId, collectionId)
                } else {
                    showToast("Удалять места можно только в своей подборке")
                }
            },
            onRouteClicked = { item ->
                val place = item.place

                val selectedPlace = SelectedPlace(
                    external_id = place.id,
                    title = place.title,
                    latitude = place.latitude,
                    longitude = place.longitude,
                    description = place.description,
                    address = place.address
                )

                buildRouteToSinglePlace(selectedPlace)
            }
        )

        binding.rvPlaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaces.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.shareLink.observe(viewLifecycleOwner) { link ->
            link?.let {
                val privacyText = when (collectionAccessType.lowercase()) {
                    "public" -> "Публичная подборка"
                    "friends" -> "Подборка для друзей"
                    else -> "Подборка"
                }

                val shareText = buildString {
                    append("$privacyText в TogetherApp\n\n")
                    append("Название: $collectionTitle\n")

                    if (collectionDescription.isNotBlank()) {
                        append("Описание: $collectionDescription\n")
                    }

                    append("Автор: $collectionAuthorLogin\n")

                    if (!collectionDeadlineAt.isNullOrBlank()) {
                        append("Дедлайн: ${formatDeadline(collectionDeadlineAt)}\n")
                    }

                    if (collectionAccessType.lowercase() == "friends") {
                        append("\nДоступно только друзьям внутри приложения.\n")
                    }

                    append("\nОткрыть подборку:\n$link")
                }

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, collectionTitle)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

                startActivity(Intent.createChooser(sendIntent, "Поделиться подборкой"))
                viewModel.clearShareLink()
            }
        }

        viewModel.places.observe(viewLifecycleOwner) { places ->
            adapter.submitList(places)

            if (places.isEmpty()) {
                binding.rvPlaces.visibility = View.GONE
                binding.tvPlacesTitle.visibility = View.GONE
            } else {
                binding.rvPlaces.visibility = View.VISIBLE
                binding.tvPlacesTitle.visibility = View.VISIBLE
            }
        }

        viewModel.progress.observe(viewLifecycleOwner) { progress ->
            binding.tvProgressText.text = "Посещено ${progress.visitedCount} из ${progress.totalCount}"
            binding.progressCollection.progress = progress.progressPercent
        }

        viewModel.collectionUpdated.observe(viewLifecycleOwner) { updated ->
            if (updated == true) {
                binding.tvCollectionTitle.text = collectionTitle
                binding.tvCollectionDescription.text = collectionDescription
                binding.tvCollectionPrivacy.text = mapAccessType(collectionAccessType)
                binding.tvCollectionDeadline.text = formatDeadline(collectionDeadlineAt)

                showToast("Подборка обновлена")
                setupActionButtonsVisibility()
                viewModel.clearUpdateState()
            }
        }

        viewModel.collectionDeleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted == true) {
                showToast("Подборка удалена")

                val navController = findNavController()
                val wasPopped = navController.popBackStack()

                if (!wasPopped) {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }

                viewModel.clearDeleteState()
            }
        }

        viewModel.collectionCopied.observe(viewLifecycleOwner) { copied ->
            if (copied == true) {
                showToast("Подборка скопирована в ваши подборки")
                viewModel.clearCopyState()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                showToast(it)
                viewModel.clearError()
            }
        }
    }

    private fun setupActionButtons() {
        binding.btnEditCollection.setOnClickListener {
            showEditCollectionDialog()
        }

        binding.btnChangePrivacy.setOnClickListener {
            showChangePrivacyDialog()
        }

        binding.btnDeleteCollection.setOnClickListener {
            showDeleteCollectionDialog()
        }

        binding.btnCopyCollection.setOnClickListener {
            copyForeignCollection()
        }

        binding.btnShareCollection.setOnClickListener {
            shareCollection()
        }
    }

    private fun setupRouteButton() {
        binding.btnBuildRoute.setOnClickListener {
            buildRouteForCollection()
        }
    }

    private fun isOwnCollection(): Boolean {
        if (forceOwnerMode) return true

        val currentUserId = sessionManager.getUserId()
        if (currentUserId == -1) return false

        if (collectionAuthorId != -1) {
            return currentUserId == collectionAuthorId
        }

        return !isFromExplore
    }

    private fun setupActionButtonsVisibility() {
        val isOwn = isOwnCollection()
        val canCopyForeignCollection =
            !isOwn && (collectionAccessType == "public" || collectionAccessType == "friends")

        val canShareOwnCollection =
            isOwn && (collectionAccessType == "public" || collectionAccessType == "friends")

        when {
            isOwn -> {
                binding.btnBuildRoute.visibility = View.VISIBLE
                binding.btnCopyCollection.visibility = View.GONE
                binding.btnShareCollection.visibility =
                    if (canShareOwnCollection) View.VISIBLE else View.GONE
                binding.btnEditCollection.visibility = View.VISIBLE
                binding.btnChangePrivacy.visibility = View.VISIBLE
                binding.btnDeleteCollection.visibility = View.VISIBLE
            }

            canCopyForeignCollection -> {
                binding.btnBuildRoute.visibility = View.VISIBLE
                binding.btnCopyCollection.visibility = View.VISIBLE
                binding.btnShareCollection.visibility = View.GONE
                binding.btnEditCollection.visibility = View.GONE
                binding.btnChangePrivacy.visibility = View.GONE
                binding.btnDeleteCollection.visibility = View.GONE
            }

            else -> {
                binding.btnBuildRoute.visibility = View.VISIBLE
                binding.btnCopyCollection.visibility = View.GONE
                binding.btnShareCollection.visibility = View.GONE
                binding.btnEditCollection.visibility = View.GONE
                binding.btnChangePrivacy.visibility = View.GONE
                binding.btnDeleteCollection.visibility = View.GONE
            }
        }
    }

    private fun onCheckInClicked(item: CollectionPlaceWithPlace) {
        if (!isOwnCollection()) {
            showToast("Отмечать посещение можно только в своей подборке")
            return
        }

        if (collectionId == -1) {
            showToast("Ошибка: не найдена подборка")
            return
        }

        if (item.isVisited) {
            showToast("Это место уже отмечено как посещённое")
            return
        }

        val place = item.place
        if (place.latitude == null || place.longitude == null) {
            showToast("У этого места нет координат")
            return
        }

        pendingCheckInItem = item

        if (hasLocationPermission()) {
            startCheckInFlow(item)
        } else {
            requestLocationPermission()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startCheckInFlow(item: CollectionPlaceWithPlace) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.tryCheckIn(
                        item = item,
                        collectionId = collectionId,
                        userLatitude = location.latitude,
                        userLongitude = location.longitude
                    )
                } else {
                    requestSingleCurrentLocation(item)
                }
            }
            .addOnFailureListener {
                showToast("Не удалось получить текущее местоположение")
            }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleCurrentLocation(item: CollectionPlaceWithPlace) {
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                viewModel.tryCheckIn(
                    item = item,
                    collectionId = collectionId,
                    userLatitude = location.latitude,
                    userLongitude = location.longitude
                )
            } else {
                showToast("Не удалось определить ваше местоположение")
            }
        }.addOnFailureListener {
            showToast("Ошибка при получении местоположения")
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun copyForeignCollection() {
        if (collectionId == -1) {
            showToast("Ошибка: не найдена подборка")
            return
        }

        viewModel.copyCollection(
            sourceCollectionId = collectionId,
            newTitle = "$collectionTitle (копия)",
            newDescription = collectionDescription,
            accessType = "private",
            deadlineAt = collectionDeadlineAt
        )
    }

    private fun shareCollection() {
        if (!isOwnCollection()) {
            showToast("Можно делиться только своими подборками")
            return
        }

        when (collectionAccessType.lowercase()) {
            "private" -> {
                showToast(
                    "Приватной подборкой нельзя поделиться. Сначала сделайте её публичной или доступной для друзей."
                )
            }

            "public", "friends" -> {
                viewModel.generateShareLink(collectionId)
            }

            else -> {
                showToast("Неизвестный тип доступа подборки")
            }
        }
    }

    private fun buildRouteForCollection() {
        val items = viewModel.places.value.orEmpty()

        if (items.isEmpty()) {
            showToast("В подборке нет мест для маршрута")
            return
        }

        val routePlaces = items.mapNotNull { item ->
            val place = item.place
            val lat = place.latitude
            val lon = place.longitude

            if (lat == null || lon == null) {
                null
            } else {
                SelectedPlace(
                    external_id = place.id,
                    title = place.title,
                    latitude = lat,
                    longitude = lon,
                    description = place.description,
                    address = place.address
                )
            }
        }

        if (routePlaces.isEmpty()) {
            showToast("У мест в подборке нет координат для маршрута")
            return
        }

        sharedMapViewModel.setRoutePlaces(routePlaces)

        showToast("Строю маршрут по подборке")
        findNavController().navigate(R.id.mapFragment)
    }

    private fun buildRouteToSinglePlace(place: SelectedPlace) {
        if (place.latitude == null || place.longitude == null) {
            showToast("У этого места нет координат для маршрута")
            return
        }

        sharedMapViewModel.setRoutePlaces(listOf(place))

        showToast("Строю маршрут до: ${place.title}")
        findNavController().navigate(R.id.mapFragment)
    }

    private fun showEditCollectionDialog() {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val titleInput = EditText(context).apply {
            hint = "Название подборки"
            setText(collectionTitle)
        }

        val descriptionInput = EditText(context).apply {
            hint = "Описание"
            setText(collectionDescription)
        }

        val deadlineInput = EditText(context).apply {
            hint = "Дедлайн"
            isFocusable = false
            isClickable = true
            setText(formatDeadline(collectionDeadlineAt))
        }

        deadlineInput.setOnClickListener {
            showDatePicker { isoDate, displayDate ->
                collectionDeadlineAt = isoDate
                deadlineInput.setText(displayDate)
            }
        }

        container.addView(titleInput)
        container.addView(descriptionInput)
        container.addView(deadlineInput)

        AlertDialog.Builder(context)
            .setTitle("Редактировать подборку")
            .setView(container)
            .setPositiveButton("Сохранить") { _, _ ->
                val newTitle = titleInput.text.toString().trim()
                val newDescription = descriptionInput.text.toString().trim()

                if (newTitle.isEmpty()) {
                    showToast("Название не может быть пустым")
                    return@setPositiveButton
                }

                collectionTitle = newTitle
                collectionDescription = newDescription

                viewModel.updateCollection(
                    collectionId = collectionId,
                    title = collectionTitle,
                    description = collectionDescription,
                    accessType = collectionAccessType,
                    deadlineAt = collectionDeadlineAt
                )
            }
            .setNeutralButton("Сбросить дедлайн") { _, _ ->
                collectionDeadlineAt = null
                viewModel.updateCollection(
                    collectionId = collectionId,
                    title = collectionTitle,
                    description = collectionDescription,
                    accessType = collectionAccessType,
                    deadlineAt = null
                )
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showChangePrivacyDialog() {
        val privacyOptions = arrayOf("Приватная", "Публичная", "Для друзей")
        val privacyValues = arrayOf("private", "public", "friends")

        val checkedItem = privacyValues.indexOf(collectionAccessType).let {
            if (it == -1) 0 else it
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Изменить приватность")
            .setSingleChoiceItems(privacyOptions, checkedItem) { dialog, which ->
                collectionAccessType = privacyValues[which]
                viewModel.updateCollectionAccessType(collectionId, collectionAccessType)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteCollectionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить подборку")
            .setMessage("Вы уверены, что хотите удалить подборку?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteCollection(collectionId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (isoDate: String, displayDate: String) -> Unit) {
        val calendar = Calendar.getInstance()

        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

                onDateSelected(
                    isoFormat.format(selectedCalendar.time),
                    displayFormat.format(selectedCalendar.time)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.datePicker.minDate = System.currentTimeMillis() - 1000
        dialog.show()
    }

    private fun setupCloseButton() {
        binding.btnClose.setOnClickListener {
            val navController = findNavController()
            val wasPopped = navController.popBackStack()

            if (!wasPopped) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun mapAccessType(accessType: String): String {
        return when (accessType.lowercase()) {
            "private" -> "Приватная"
            "public" -> "Публичная"
            "friends" -> "Для друзей"
            else -> "Не указано"
        }
    }

    private fun formatDeadline(deadline: String?): String {
        if (deadline.isNullOrBlank()) return "Не задан"

        return try {
            val parsed = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            ).parse(deadline)

            if (parsed != null) {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(parsed)
            } else {
                deadline
            }
        } catch (e: Exception) {
            deadline
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}