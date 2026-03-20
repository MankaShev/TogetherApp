package com.example.togetherapp.presentation.screens.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.togetherapp.R
import com.example.togetherapp.databinding.FragmentMapBinding
import com.example.togetherapp.domain.models.SelectedPlace
import com.example.togetherapp.presentation.viewmodel.SharedMapViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.BusinessObjectMetadata
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.runtime.Error

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private lateinit var searchManager: SearchManager
    private var mapInputListener: InputListener? = null
    private var searchSession: Session? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var sharedViewModel: SharedMapViewModel
    private var currentPlace: SelectedPlace? = null
    private lateinit var userLocationLayer: UserLocationLayer
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(requireContext())

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                moveToUserWithZoom()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = binding.mapContainer.mapview

        // Shared ViewModel
        sharedViewModel = ViewModelProvider(requireActivity())[SharedMapViewModel::class.java]

        // Наблюдение за результатом добавления в коллекцию
        observeNavigationEvents()

        // BottomSheet
        val bottomSheet = binding.mapContainer.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN

        // SearchManager
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

        // Адаптер поиска по строке
        searchAdapter = SearchAdapter { item ->
            val point = item.obj?.geometry?.firstOrNull()?.point
            if (point != null) {
                mapView.mapWindow.map.move(
                    CameraPosition(point, 17.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1f),
                    null
                )

                binding.cvSearchResults.visibility = View.GONE
                binding.etSearch.clearFocus()

                startSearch(point)
            }
        }

        binding.rvSearch.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }

        binding.etSearch.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    startTextSearch(query)
                    binding.etSearch.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    binding.cvSearchResults.visibility = View.GONE
                }
                return true
            }
        })

        setupMap()
        setupAddToCollectionButton()
    }

    private fun observeNavigationEvents() {
        sharedViewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is SharedMapViewModel.NavigationEvent.BackToMapWithResult -> {
                    Toast.makeText(
                        requireContext(),
                        "✅ Место добавлено в «${event.collectionName}»",
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.mapContainer.addToCollectionButton.text = "Добавлено ✓"
                    binding.mapContainer.addToCollectionButton.isEnabled = false

                    currentPlace = null
                    sharedViewModel.consumeNavigationEvent()
                }

                null -> {}
            }
        }
    }

    private fun setupAddToCollectionButton() {
        binding.mapContainer.addToCollectionButton.setOnClickListener {
            val place = currentPlace
            if (place != null) {
                sharedViewModel.setSelectedPlace(place)

                findNavController().navigate(
                    R.id.collectionsFragment,
                    Bundle().apply {
                        putBoolean("isAddingMode", true)
                    }
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    "Сначала выберите место на карте",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupMap() {
        val map = mapView.mapWindow.map
        val mapKit = MapKitFactory.getInstance()

        // Ставим Томск по дефолту
        map.move(
            CameraPosition(
                Point(56.471116, 84.946636),
                16.5f,
                0.0f,
                0.0f
            )
        )

        // Инициализируем слой пользователя
        userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer.isVisible = true

        // Проверяем разрешения и, если можно, двигаем карту к пользователю
        checkPermissionsAndMove()

        mapInputListener = object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                binding.mapContainer.placeNameText.text = ""
                binding.mapContainer.placeAddressText.text = ""
                binding.mapContainer.categoryText.text = ""

                binding.mapContainer.addToCollectionButton.text = "Добавить в коллекцию"
                binding.mapContainer.addToCollectionButton.isEnabled = true

                currentPlace = null
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN

                android.util.Log.d("MapStep", "ЭТАП 2: Запускаю поиск для ${point.latitude}")
                startSearch(point)
            }

            override fun onMapLongTap(map: Map, point: Point) {}
        }

        map.addInputListener(mapInputListener!!)
    }

    private fun checkPermissionsAndMove() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                requireContext(),
                "Подождите пару секунд, сейчас мы вас найдем :)",
                Toast.LENGTH_LONG
            ).show()
            moveToUserWithZoom()
        } else {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun moveToUserWithZoom() {
        mapView.postDelayed({
            userLocationLayer.cameraPosition()?.target?.let { userPoint ->
                mapView.mapWindow.map.move(
                    CameraPosition(userPoint, 16.5f, 0f, 0f),
                    Animation(Animation.Type.SMOOTH, 1.5f),
                    null
                )
            } ?: run {
                moveToUserWithZoom()
            }
        }, 2000)
    }

    private fun startSearch(point: Point) {
        val searchOptions = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 1
        }

        searchSession = searchManager.submit(
            point,
            16,
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val bizMetadata = response.collection.children.firstOrNull()?.obj
                        ?.metadataContainer
                        ?.getItem(BusinessObjectMetadata::class.java)

                    if (bizMetadata != null) {
                        val name = bizMetadata.name
                        val address = bizMetadata.address.formattedAddress
                        val category = bizMetadata.categories.firstOrNull()?.name ?: ""

                        android.util.Log.d("MapStep", "ЭТАП 2 ПРОЙДЕН: Найдена организация: $name")

                        currentPlace = SelectedPlace(
                            external_id = (point.latitude * point.longitude).toInt(),
                            title = name,
                            latitude = point.latitude,
                            longitude = point.longitude,
                            description = null,
                            address = address
                        )

                        binding.mapContainer.addToCollectionButton.text = "Добавить в коллекцию"
                        binding.mapContainer.addToCollectionButton.isEnabled = true

                        binding.mapContainer.placeNameText.text = name
                        binding.mapContainer.placeAddressText.text = address
                        binding.mapContainer.categoryText.text = category

                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        android.util.Log.d("MapStep", "В этой точке нет организаций")
                        currentPlace = null
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }

                override fun onSearchError(error: Error) {
                    android.util.Log.e("MapStep", "Ошибка поиска: $error")
                    currentPlace = null
                }
            }
        )
    }

    private fun startTextSearch(query: String) {
        val searchOptions = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 7
        }

        val visibleRegion = mapView.mapWindow.map.visibleRegion

        searchSession = searchManager.submit(
            query,
            VisibleRegionUtils.toPolygon(visibleRegion),
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val results = response.collection.children
                    android.util.Log.d("MapSearch", "Найдено объектов: ${results.size}")

                    if (results.isNotEmpty()) {
                        searchAdapter.submitList(results)
                        binding.cvSearchResults.visibility = View.VISIBLE
                    } else {
                        binding.cvSearchResults.visibility = View.GONE
                    }

                    results.forEachIndexed { index, searchResult ->
                        val obj = searchResult.obj
                        val metadata = obj?.metadataContainer
                            ?.getItem(BusinessObjectMetadata::class.java)

                        if (metadata != null) {
                            val name = metadata.name
                            val address = metadata.address.formattedAddress
                            val point = obj.geometry.firstOrNull()?.point

                            android.util.Log.d(
                                "MapSearch",
                                "#$index: $name | Адрес: $address | Координаты: ${point?.latitude}, ${point?.longitude}"
                            )
                        }
                    }

                    if (results.isEmpty()) {
                        android.util.Log.d("MapSearch", "Ничего не найдено")
                    }
                }

                override fun onSearchError(error: Error) {
                    android.util.Log.e("MapSearch", "Ошибка текстового поиска: $error")
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        currentPlace = null
    }
}