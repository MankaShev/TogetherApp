package com.example.togetherapp.presentation.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.togetherapp.R
import com.example.togetherapp.data.local.SessionManager
import com.example.togetherapp.databinding.FragmentMapBinding
import com.example.togetherapp.domain.models.Interest
import com.example.togetherapp.domain.models.SelectedPlace
import com.example.togetherapp.presentation.viewmodel.MapViewModel
import com.example.togetherapp.presentation.viewmodel.SharedMapViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.yandex.mapkit.Animation
import com.yandex.mapkit.GeoObjectCollection
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.map.VisibleRegion
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.BusinessObjectMetadata
import com.yandex.mapkit.search.BusinessPhotoObjectMetadata
import com.yandex.mapkit.search.BusinessRating1xObjectMetadata
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.search.Snippet
import com.yandex.runtime.image.ImageProvider
import java.util.Calendar
import java.util.Locale

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private lateinit var searchManager: SearchManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var drivingRouter: DrivingRouter
    private lateinit var popularPlacesLoader: PopularPlacesLoader

    private var mapInputListener: InputListener? = null
    private var searchSession: Session? = null
    private var photoSession: Session? = null
    private var drivingSession: DrivingSession? = null

    private var polylineMapObject: PolylineMapObject? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var popularPlacesCollection: MapObjectCollection? = null
    private var routeMarkersCollection: MapObjectCollection? = null
    private var searchResultsCollection: MapObjectCollection? = null

    private lateinit var searchAdapter: SearchAdapter
    private lateinit var sharedViewModel: SharedMapViewModel
    private lateinit var mapViewModel: MapViewModel
    private lateinit var sessionManager: SessionManager

    private var currentPlace: SelectedPlace? = null
    private var currentPlaceWorkingHoursText: String? = null
    private var userInterests: List<Interest> = emptyList()

    private var interestSearchRequestId: Int = 0
    private var isInterestSearchInProgress: Boolean = false
    private var isRouteBuildInProgress: Boolean = false
    private var arePopularPlacesHiddenByRoute: Boolean = false

    private val tomskFallbackPoint = Point(56.484645, 84.947649)

    private val russiaMinLat = 41.0
    private val russiaMaxLat = 82.0
    private val russiaMinLon = 19.0
    private val russiaMaxLon = 191.0
    private val emulatorLat = 37.4219983
    private val emulatorLon = -122.084

    private val popularPlaceTapListener =
        MapObjectTapListener { mapObject, _ ->
            val place = mapObject.userData as? SelectedPlace
            if (place != null) {
                openPopularPlace(place)
                true
            } else {
                false
            }
        }

    private val searchResultTapListener =
        MapObjectTapListener { mapObject, _ ->
            val point = mapObject.userData as? Point
            if (point != null) {
                mapView.mapWindow.map.move(
                    CameraPosition(point, 17.0f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1f),
                    null
                )
                binding.cvSearchResults.visibility = View.GONE
                startSearch(point)
                true
            } else {
                false
            }
        }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                moveToUserLocation()
                retryPendingRouteBuild()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Разрешение на геолокацию не выдано",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun isPointInRussia(point: Point): Boolean {
        return point.latitude in russiaMinLat..russiaMaxLat &&
                point.longitude in russiaMinLon..russiaMaxLon
    }

    private fun isDefaultEmulatorPoint(point: Point): Boolean {
        return kotlin.math.abs(point.latitude - emulatorLat) < 0.02 &&
                kotlin.math.abs(point.longitude - emulatorLon) < 0.02
    }

    private fun resolveStartPoint(actualPoint: Point): Point {
        val isEmulatorPoint = isDefaultEmulatorPoint(actualPoint)
        val inRussia = isPointInRussia(actualPoint)
        return if (isEmulatorPoint || !inRussia) tomskFallbackPoint else actualPoint
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = binding.mapContainer.mapview
        sharedViewModel = ViewModelProvider(requireActivity())[SharedMapViewModel::class.java]
        mapViewModel = ViewModelProvider(this)[MapViewModel::class.java]
        sessionManager = SessionManager.getInstance(requireContext())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        drivingRouter = DirectionsFactory.getInstance()
            .createDrivingRouter(DrivingRouterType.COMBINED)

        searchManager = SearchFactory.getInstance()
            .createSearchManager(SearchManagerType.COMBINED)

        popularPlacesLoader = PopularPlacesLoader(searchManager)

        observeNavigationEvents()
        observeMapState()
        observeRoutePlaces()
        observeRoutePlace()
        loadUserInterests()

        val bottomSheet = binding.mapContainer.bottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheet.post {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }

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

        binding.etSearch.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
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
                        clearSearchResultMarkers()
                    }
                    return true
                }
            }
        )

        binding.btnMyInterests.setOnClickListener {
            startSearchByUserInterests()
        }

        binding.btnNearMe.setOnClickListener {
            moveToUserLocation()
        }

        binding.mapContainer.btnClearRoute.setOnClickListener {
            clearCurrentRoute()
        }

        setupMap()
        setupAddToCollectionButton()
        setupBuildRouteButtonForCurrentPlace()
        hideRouteInfo()
        loadImageIntoView(null)
        loadPopularPlacesOnMap()
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

                null -> Unit
            }
        }
    }

    private fun observeMapState() {
        mapViewModel.mapState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MapViewModel.MapState.Idle -> Unit
                is MapViewModel.MapState.LoadingInterests -> Unit
                is MapViewModel.MapState.InterestsLoaded -> {
                    userInterests = state.interests
                }
                is MapViewModel.MapState.Error -> Unit
            }
        }
    }

    private fun observeRoutePlaces() {
        sharedViewModel.routePlaces.observe(viewLifecycleOwner) { places ->
            if (places.isNullOrEmpty()) return@observe
            buildRouteFromCurrentLocation(places)
        }
    }

    private fun observeRoutePlace() {
        sharedViewModel.routePlace.observe(viewLifecycleOwner) { place ->
            if (place == null) return@observe
            buildRouteToSinglePlace(place)
        }
    }

    private fun loadUserInterests() {
        val userId = sessionManager.getUserId()
        if (userId != -1) {
            mapViewModel.loadUserInterests(userId)
        }
    }

    private fun setupAddToCollectionButton() {
        binding.mapContainer.addToCollectionButton.setOnClickListener {
            val place = currentPlace
            if (place != null) {
                sharedViewModel.setSelectedPlace(place)
                findNavController().navigate(
                    R.id.collectionsFragment,
                    Bundle().apply { putBoolean("isAddingMode", true) }
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

    private fun setupBuildRouteButtonForCurrentPlace() {
        binding.mapContainer.btnBuildRouteToPlace.setOnClickListener {
            val place = currentPlace
            if (place != null) {
                sharedViewModel.setRoutePlace(place)
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

        mapView.post {
            map.move(
                CameraPosition(
                    Point(56.471116, 84.946636),
                    16.5f,
                    0.0f,
                    0.0f
                )
            )
        }

        mapInputListener = object : InputListener {
            override fun onMapTap(map: Map, point: Point) {
                binding.mapContainer.placeNameText.text = ""
                binding.mapContainer.placeAddressText.text = ""
                binding.mapContainer.categoryText.text = ""
                binding.mapContainer.placeRatingText.text = "Рейтинг: —"
                binding.mapContainer.placeWorkingHoursText.text = "Часы работы: —"
                binding.mapContainer.placeDescriptionText.text = "Описание: —"

                binding.mapContainer.addToCollectionButton.text = "Добавить в коллекцию"
                binding.mapContainer.addToCollectionButton.isEnabled = true

                currentPlace = null
                currentPlaceWorkingHoursText = null
                bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                loadImageIntoView(null)
                clearSearchResultMarkers()

                startSearch(point)
            }

            override fun onMapLongTap(map: Map, point: Point) = Unit
        }

        map.addInputListener(mapInputListener!!)
    }

    private fun hideRouteInfo() {
        binding.mapContainer.routeInfoContainer.visibility = View.GONE
        binding.mapContainer.btnClearRoute.visibility = View.GONE
        binding.mapContainer.tvRouteTitle.text = "Информация о маршруте"
        binding.mapContainer.tvRouteFrom.text = "Откуда: —"
        binding.mapContainer.tvRouteTo.text = "Куда: —"
        binding.mapContainer.tvRouteDuration.text = "Время: —"
        binding.mapContainer.tvRouteDistance.text = "Расстояние: —"
        binding.mapContainer.tvRouteWarning.text = ""
        binding.mapContainer.tvRouteWarning.visibility = View.GONE
    }

    private fun showRouteInfo(
        fromText: String,
        toText: String,
        durationText: String,
        distanceText: String,
        warningText: String? = null
    ) {
        binding.mapContainer.routeInfoContainer.visibility = View.VISIBLE
        binding.mapContainer.btnClearRoute.visibility = View.VISIBLE
        binding.mapContainer.tvRouteTitle.text = "Информация о маршруте"
        binding.mapContainer.tvRouteFrom.text = "Откуда: $fromText"
        binding.mapContainer.tvRouteTo.text = "Куда: $toText"
        binding.mapContainer.tvRouteDuration.text = "Время: $durationText"
        binding.mapContainer.tvRouteDistance.text = "Расстояние: $distanceText"

        if (warningText.isNullOrBlank()) {
            binding.mapContainer.tvRouteWarning.text = ""
            binding.mapContainer.tvRouteWarning.visibility = View.GONE
        } else {
            binding.mapContainer.tvRouteWarning.text = warningText
            binding.mapContainer.tvRouteWarning.visibility = View.VISIBLE
        }

        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun formatDistance(distanceMeters: Int): String {
        return if (distanceMeters < 1000) {
            "$distanceMeters м"
        } else {
            String.format(Locale.getDefault(), "%.1f км", distanceMeters / 1000.0)
        }
    }

    private fun formatDuration(durationSeconds: Int): String {
        val totalMinutes = durationSeconds / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (hours > 0) {
            "${hours} ч ${minutes} мин"
        } else {
            "${minutes} мин"
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun getStartPointLabel(startPoint: Point): String {
        return if (
            kotlin.math.abs(startPoint.latitude - tomskFallbackPoint.latitude) < 0.001 &&
            kotlin.math.abs(startPoint.longitude - tomskFallbackPoint.longitude) < 0.001
        ) {
            "Томск, Ленина 36"
        } else {
            "Текущее местоположение"
        }
    }

    private fun updateRouteInfo(
        route: DrivingRoute,
        startPoint: Point,
        destinationTitle: String
    ) {
        try {
            val metadata = route.metadata
            val distanceMeters = metadata.weight.distance.value.toInt()
            val durationSeconds = metadata.weight.time.value.toInt()
            val warningText = checkArrivalAgainstWorkingHours(
                workingHoursText = currentPlaceWorkingHoursText,
                routeDurationSeconds = durationSeconds
            )

            showRouteInfo(
                fromText = getStartPointLabel(startPoint),
                toText = destinationTitle,
                durationText = formatDuration(durationSeconds),
                distanceText = formatDistance(distanceMeters),
                warningText = warningText
            )

            if (warningText != null) {
                Toast.makeText(requireContext(), warningText, Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
            showRouteInfo(
                fromText = getStartPointLabel(startPoint),
                toText = destinationTitle,
                durationText = "не удалось определить",
                distanceText = "не удалось определить"
            )
        }
    }

    private fun updateCollectionRouteInfo(
        route: DrivingRoute,
        startPoint: Point,
        placesCount: Int
    ) {
        try {
            val metadata = route.metadata
            val distanceMeters = metadata.weight.distance.value.toInt()
            val durationSeconds = metadata.weight.time.value.toInt()

            showRouteInfo(
                fromText = getStartPointLabel(startPoint),
                toText = "Подборка ($placesCount мест)",
                durationText = formatDuration(durationSeconds),
                distanceText = formatDistance(distanceMeters)
            )
        } catch (_: Exception) {
            showRouteInfo(
                fromText = getStartPointLabel(startPoint),
                toText = "Подборка ($placesCount мест)",
                durationText = "не удалось определить",
                distanceText = "не удалось определить"
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun moveToUserLocation() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                val finalPoint = if (location != null) {
                    resolveStartPoint(Point(location.latitude, location.longitude))
                } else {
                    tomskFallbackPoint
                }

                mapView.mapWindow.map.move(
                    CameraPosition(finalPoint, 16.5f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1.5f),
                    null
                )

                binding.cvSearchResults.visibility = View.GONE
                binding.etSearch.clearFocus()
                resetSelectedPlaceState()
                startNearbySearch(finalPoint)
            }
            .addOnFailureListener {
                mapView.mapWindow.map.move(
                    CameraPosition(tomskFallbackPoint, 16.5f, 0.0f, 0.0f),
                    Animation(Animation.Type.SMOOTH, 1.5f),
                    null
                )

                startNearbySearch(tomskFallbackPoint)

                Toast.makeText(
                    requireContext(),
                    "Ошибка геолокации. Открыта точка по умолчанию: Томск",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun resetSelectedPlaceState() {
        binding.mapContainer.placeNameText.text = ""
        binding.mapContainer.placeAddressText.text = ""
        binding.mapContainer.categoryText.text = ""
        binding.mapContainer.placeRatingText.text = "Рейтинг: —"
        binding.mapContainer.placeWorkingHoursText.text = "Часы работы: —"
        binding.mapContainer.placeDescriptionText.text = "Описание: —"
        binding.mapContainer.addToCollectionButton.text = "Добавить в коллекцию"
        binding.mapContainer.addToCollectionButton.isEnabled = true

        currentPlace = null
        currentPlaceWorkingHoursText = null
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        loadImageIntoView(null)
    }

    private fun loadImageIntoView(url: String?) {
        Glide.with(this)
            .load(url ?: R.drawable.ic_map_with_point)
            .placeholder(R.drawable.ic_map_with_point)
            .error(R.drawable.ic_map_with_point)
            .into(binding.mapContainer.placeImage)
    }

    private fun formatRatingText(score: Float?, reviews: Int): String {
        return if (score != null) {
            if (reviews > 0) {
                "Рейтинг: %.1f (%d отзывов)".format(Locale.getDefault(), score, reviews)
            } else {
                "Рейтинг: %.1f".format(Locale.getDefault(), score)
            }
        } else {
            "Рейтинг: —"
        }
    }

    private fun formatWorkingHoursText(metadata: BusinessObjectMetadata): String {
        return try {
            val workingHours = metadata.getWorkingHours()
            val text = workingHours?.text
            if (!text.isNullOrBlank()) {
                "Часы работы: $text"
            } else {
                "Часы работы: —"
            }
        } catch (_: Exception) {
            "Часы работы: —"
        }
    }

    private fun buildDescriptionText(
        metadata: BusinessObjectMetadata,
        category: String,
        address: String
    ): String {
        val phone = try {
            metadata.getPhones().firstOrNull()?.formattedNumber
        } catch (_: Exception) {
            null
        }

        val parts = mutableListOf<String>()
        if (category.isNotBlank()) parts.add(category)
        if (address.isNotBlank()) parts.add(address)
        if (!phone.isNullOrBlank()) parts.add("Тел: $phone")

        return if (parts.isNotEmpty()) {
            "Описание: ${parts.joinToString(" • ")}"
        } else {
            "Описание: —"
        }
    }

    private fun loadPlacePhotoByOid(
        oid: String?,
        onResult: (String?) -> Unit
    ) {
        if (oid.isNullOrBlank()) {
            onResult(null)
            return
        }

        val options = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 1
            snippets = Snippet.PHOTOS.value
        }

        photoSession?.cancel()
        photoSession = searchManager.searchByBusinessOids(
            listOf(oid),
            options,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    try {
                        val obj = response.collection.children.firstOrNull()?.obj
                        val photoMetadata = obj?.metadataContainer
                            ?.getItem(BusinessPhotoObjectMetadata::class.java)

                        val firstPhoto = photoMetadata?.getPhotos()?.firstOrNull()
                        val rawPhotoValue = firstPhoto?.getId()

                        if (!rawPhotoValue.isNullOrBlank()) {
                            val photoUrl = if (rawPhotoValue.contains("%s")) {
                                rawPhotoValue.replace("%s", "orig")
                            } else {
                                "$rawPhotoValue/orig"
                            }
                            onResult(photoUrl)
                        } else {
                            onResult(null)
                        }
                    } catch (_: Exception) {
                        onResult(null)
                    }
                }

                override fun onSearchError(error: com.yandex.runtime.Error) {
                    onResult(null)
                }
            }
        )
    }

    private fun bindPlaceCard(
        point: Point,
        name: String,
        address: String,
        category: String,
        oid: String?,
        ratingScore: Float?,
        ratingReviews: Int,
        workingHoursText: String,
        descriptionText: String
    ) {
        currentPlace = SelectedPlace(
            external_id = (point.latitude * point.longitude).toInt(),
            title = name,
            latitude = point.latitude,
            longitude = point.longitude,
            description = null,
            address = address
        )
        currentPlaceWorkingHoursText = workingHoursText

        binding.mapContainer.addToCollectionButton.text = "Добавить в коллекцию"
        binding.mapContainer.addToCollectionButton.isEnabled = true
        binding.mapContainer.placeNameText.text = name
        binding.mapContainer.placeAddressText.text = address
        binding.mapContainer.categoryText.text = category
        binding.mapContainer.placeRatingText.text = formatRatingText(ratingScore, ratingReviews)
        binding.mapContainer.placeWorkingHoursText.text = workingHoursText
        binding.mapContainer.placeDescriptionText.text = descriptionText

        loadImageIntoView(null)

        loadPlacePhotoByOid(oid) { url ->
            if (!isAdded || _binding == null) return@loadPlacePhotoByOid
            loadImageIntoView(url)
        }

        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun startSearch(point: Point) {
        val searchOptions = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 1
            snippets = Snippet.PHOTOS.value or Snippet.BUSINESS_RATING1X.value
        }

        searchSession?.cancel()

        searchSession = searchManager.submit(
            point,
            16,
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val obj = response.collection.children.firstOrNull()?.obj
                    val bizMetadata = obj?.metadataContainer
                        ?.getItem(BusinessObjectMetadata::class.java)

                    if (bizMetadata != null) {
                        val ratingMetadata = obj.metadataContainer
                            ?.getItem(BusinessRating1xObjectMetadata::class.java)

                        val objPoint = obj.geometry.firstOrNull()?.point ?: point
                        val name = bizMetadata.name
                        val address = bizMetadata.address.formattedAddress
                        val category = bizMetadata.categories.firstOrNull()?.name ?: ""
                        val oid = try {
                            bizMetadata.getOid()
                        } catch (_: Exception) {
                            null
                        }

                        val ratingScore = try {
                            ratingMetadata?.getScore()
                        } catch (_: Exception) {
                            null
                        }

                        val ratingReviews = try {
                            ratingMetadata?.getReviews() ?: 0
                        } catch (_: Exception) {
                            0
                        }

                        val workingHoursText = formatWorkingHoursText(bizMetadata)
                        val descriptionText = buildDescriptionText(
                            metadata = bizMetadata,
                            category = category,
                            address = address
                        )

                        bindPlaceCard(
                            point = objPoint,
                            name = name,
                            address = address,
                            category = category,
                            oid = oid,
                            ratingScore = ratingScore,
                            ratingReviews = ratingReviews,
                            workingHoursText = workingHoursText,
                            descriptionText = descriptionText
                        )
                    } else {
                        currentPlace = null
                        currentPlaceWorkingHoursText = null
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                        loadImageIntoView(null)

                        Toast.makeText(
                            requireContext(),
                            "В этой точке нет организаций",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onSearchError(error: com.yandex.runtime.Error) {
                    currentPlace = null
                    currentPlaceWorkingHoursText = null
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                    loadImageIntoView(null)

                    Toast.makeText(
                        requireContext(),
                        "Ошибка поиска мест",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun startNearbySearch(userPoint: Point) {
        val query = getNearbySearchQuery()

        val searchOptions = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 10
            userPosition = userPoint
            snippets = Snippet.PHOTOS.value or Snippet.BUSINESS_RATING1X.value
        }

        val boundingBox = BoundingBox(
            Point(userPoint.latitude - 0.01, userPoint.longitude - 0.01),
            Point(userPoint.latitude + 0.01, userPoint.longitude + 0.01)
        )

        searchSession?.cancel()

        searchSession = searchManager.submit(
            query,
            Geometry.fromBoundingBox(boundingBox),
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val results = response.collection.children

                    if (results.isNotEmpty()) {
                        searchAdapter.submitList(results)
                        binding.cvSearchResults.visibility = View.VISIBLE
                        showSearchResultMarkers(results)

                        val firstResult = results.firstOrNull()
                        val obj = firstResult?.obj
                        val point = obj?.geometry?.firstOrNull()?.point
                        val metadata = obj?.metadataContainer
                            ?.getItem(BusinessObjectMetadata::class.java)

                        if (metadata != null && point != null) {
                            val ratingMetadata = obj.metadataContainer
                                ?.getItem(BusinessRating1xObjectMetadata::class.java)

                            val category = metadata.categories.firstOrNull()?.name ?: ""
                            val address = metadata.address.formattedAddress

                            val ratingScore = try {
                                ratingMetadata?.getScore()
                            } catch (_: Exception) {
                                null
                            }

                            val ratingReviews = try {
                                ratingMetadata?.getReviews() ?: 0
                            } catch (_: Exception) {
                                0
                            }

                            val workingHoursText = formatWorkingHoursText(metadata)
                            val descriptionText = buildDescriptionText(
                                metadata = metadata,
                                category = category,
                                address = address
                            )

                            fillBottomSheetWithPlace(
                                point = point,
                                name = metadata.name,
                                address = address,
                                category = category,
                                oid = try {
                                    metadata.getOid()
                                } catch (_: Exception) {
                                    null
                                },
                                ratingScore = ratingScore,
                                ratingReviews = ratingReviews,
                                workingHoursText = workingHoursText,
                                descriptionText = descriptionText
                            )
                        } else {
                            currentPlace = null
                            currentPlaceWorkingHoursText = null
                            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                            loadImageIntoView(null)
                        }
                    } else {
                        binding.cvSearchResults.visibility = View.GONE
                        clearSearchResultMarkers()
                        currentPlace = null
                        currentPlaceWorkingHoursText = null
                        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                        loadImageIntoView(null)

                        Toast.makeText(
                            requireContext(),
                            "Рядом ничего не найдено",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onSearchError(error: com.yandex.runtime.Error) {
                    currentPlace = null
                    currentPlaceWorkingHoursText = null
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                    binding.cvSearchResults.visibility = View.GONE
                    clearSearchResultMarkers()
                    loadImageIntoView(null)

                    Toast.makeText(
                        requireContext(),
                        "Ошибка поиска мест рядом",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun getNearbySearchQuery(): String {
        if (userInterests.isEmpty()) return "кафе"

        val queries = InterestSearchMapper.mapInterestsToQueries(
            userInterests.map { it.name }
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return queries.firstOrNull() ?: "кафе"
    }

    private fun fillBottomSheetWithPlace(
        point: Point,
        name: String,
        address: String,
        category: String,
        oid: String?,
        ratingScore: Float?,
        ratingReviews: Int,
        workingHoursText: String,
        descriptionText: String
    ) {
        bindPlaceCard(
            point = point,
            name = name,
            address = address,
            category = category,
            oid = oid,
            ratingScore = ratingScore,
            ratingReviews = ratingReviews,
            workingHoursText = workingHoursText,
            descriptionText = descriptionText
        )
    }

    private fun startTextSearch(query: String) {
        val searchOptions = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 7
            snippets = Snippet.PHOTOS.value or Snippet.BUSINESS_RATING1X.value
        }

        val visibleRegion = mapView.mapWindow.map.visibleRegion
        searchSession?.cancel()

        searchSession = searchManager.submit(
            query,
            VisibleRegionUtils.toPolygon(visibleRegion),
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val results = response.collection.children

                    if (results.isNotEmpty()) {
                        searchAdapter.submitList(results)
                        binding.cvSearchResults.visibility = View.VISIBLE
                        showSearchResultMarkers(results)
                    } else {
                        binding.cvSearchResults.visibility = View.GONE
                        clearSearchResultMarkers()
                    }
                }

                override fun onSearchError(error: com.yandex.runtime.Error) {
                    binding.cvSearchResults.visibility = View.GONE
                    clearSearchResultMarkers()
                }
            }
        )
    }

    private fun startSearchByUserInterests() {
        if (isInterestSearchInProgress) {
            Toast.makeText(
                requireContext(),
                "Поиск по интересам уже выполняется",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (userInterests.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "У вас пока нет выбранных интересов",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val queries = InterestSearchMapper.mapInterestsToQueries(
            userInterests.map { it.name }
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(5)

        if (queries.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Для выбранных интересов не найдено поисковых категорий",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val visibleRegion = mapView.mapWindow.map.visibleRegion
        val searchOptions = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 7
            snippets = Snippet.PHOTOS.value or Snippet.BUSINESS_RATING1X.value
        }

        interestSearchRequestId++
        val requestId = interestSearchRequestId
        isInterestSearchInProgress = true

        binding.cvSearchResults.visibility = View.GONE
        binding.etSearch.clearFocus()

        Toast.makeText(
            requireContext(),
            "Ищу места по вашим интересам",
            Toast.LENGTH_SHORT
        ).show()

        searchByQueriesSequentially(
            queries = queries,
            index = 0,
            visibleRegion = visibleRegion,
            searchOptions = searchOptions,
            aggregated = mutableListOf(),
            requestId = requestId
        )
    }

    private fun searchByQueriesSequentially(
        queries: List<String>,
        index: Int,
        visibleRegion: VisibleRegion,
        searchOptions: SearchOptions,
        aggregated: MutableList<GeoObjectCollection.Item>,
        requestId: Int
    ) {
        if (!isAdded || _binding == null) return
        if (requestId != interestSearchRequestId) return

        if (index >= queries.size) {
            isInterestSearchInProgress = false
            showAggregatedInterestResults(aggregated)
            return
        }

        val query = queries[index]

        searchSession?.cancel()
        searchSession = searchManager.submit(
            query,
            VisibleRegionUtils.toPolygon(visibleRegion),
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    if (!isAdded || _binding == null) return
                    if (requestId != interestSearchRequestId) return

                    aggregated.addAll(response.collection.children)

                    searchByQueriesSequentially(
                        queries = queries,
                        index = index + 1,
                        visibleRegion = visibleRegion,
                        searchOptions = searchOptions,
                        aggregated = aggregated,
                        requestId = requestId
                    )
                }

                override fun onSearchError(error: com.yandex.runtime.Error) {
                    if (!isAdded || _binding == null) return
                    if (requestId != interestSearchRequestId) return

                    searchByQueriesSequentially(
                        queries = queries,
                        index = index + 1,
                        visibleRegion = visibleRegion,
                        searchOptions = searchOptions,
                        aggregated = aggregated,
                        requestId = requestId
                    )
                }
            }
        )
    }

    private fun showAggregatedInterestResults(results: List<GeoObjectCollection.Item>) {
        if (!isAdded || _binding == null) return

        val uniqueResults = results
            .distinctBy { item -> buildSearchResultKey(item) }
            .filter { item -> item.obj?.geometry?.firstOrNull()?.point != null }
            .take(30)

        if (uniqueResults.isEmpty()) {
            binding.cvSearchResults.visibility = View.GONE
            clearSearchResultMarkers()
            Toast.makeText(
                requireContext(),
                "По вашим интересам ничего не найдено в текущей области карты",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        searchAdapter.submitList(uniqueResults)
        binding.cvSearchResults.visibility = View.VISIBLE
        showSearchResultMarkers(uniqueResults)

        Toast.makeText(
            requireContext(),
            "Найдено ${uniqueResults.size} мест по интересам",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun buildSearchResultKey(item: GeoObjectCollection.Item): String {
        val obj = item.obj
        val point = obj?.geometry?.firstOrNull()?.point
        val metadata = obj?.metadataContainer?.getItem(BusinessObjectMetadata::class.java)

        val name = metadata?.name?.trim().orEmpty()
        val lat = point?.latitude?.let { String.format(Locale.getDefault(), "%.5f", it) }.orEmpty()
        val lon = point?.longitude?.let { String.format(Locale.getDefault(), "%.5f", it) }.orEmpty()

        return "$name|$lat|$lon"
    }

    private fun clearSearchResultMarkers() {
        searchResultsCollection?.let {
            mapView.mapWindow.map.mapObjects.remove(it)
        }
        searchResultsCollection = null
    }

    private fun showSearchResultMarkers(results: List<GeoObjectCollection.Item>) {
        clearSearchResultMarkers()

        if (results.isEmpty()) return

        val collection = mapView.mapWindow.map.mapObjects.addCollection()
        searchResultsCollection = collection

        results.forEach { item ->
            val point = item.obj?.geometry?.firstOrNull()?.point ?: return@forEach

            collection.addPlacemark().apply {
                geometry = point
                setIcon(
                    ImageProvider.fromResource(requireContext(), R.drawable.ic_map_with_point),
                    IconStyle().apply {
                        scale = 0.14f
                    }
                )
                userData = point
                addTapListener(searchResultTapListener)
            }
        }
    }

    private fun loadPopularPlacesOnMap() {
        val centerPoint = tomskFallbackPoint

        popularPlacesLoader.loadPopularPlaces(
            center = centerPoint,
            onSuccess = { places ->
                if (!isAdded || _binding == null) return@loadPopularPlaces
                showPopularPlaces(places)
            },
            onError = { message ->
                if (!isAdded || _binding == null) return@loadPopularPlaces
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showPopularPlaces(places: List<SelectedPlace>) {
        val mapObjects = mapView.mapWindow.map.mapObjects

        popularPlacesCollection?.let {
            mapObjects.remove(it)
        }

        popularPlacesCollection = mapObjects.addCollection()

        places.forEach { place ->
            addPopularPlaceMarker(place)
        }
    }

    private fun hidePopularPlacesOnMap() {
        popularPlacesCollection?.let {
            mapView.mapWindow.map.mapObjects.remove(it)
        }
        popularPlacesCollection = null
        arePopularPlacesHiddenByRoute = true
    }

    private fun restorePopularPlacesIfNeeded() {
        if (arePopularPlacesHiddenByRoute) {
            arePopularPlacesHiddenByRoute = false
            loadPopularPlacesOnMap()
        }
    }

    private fun addPopularPlaceMarker(place: SelectedPlace) {
        val collection = popularPlacesCollection ?: return

        collection.addPlacemark().apply {
            geometry = Point(place.latitude, place.longitude)
            setIcon(
                ImageProvider.fromResource(requireContext(), R.drawable.ic_map_with_point),
                IconStyle().apply {
                    scale = 0.18f
                }
            )
            userData = place
            addTapListener(popularPlaceTapListener)
        }
    }

    private fun openPopularPlace(place: SelectedPlace) {
        val point = Point(place.latitude, place.longitude)

        mapView.mapWindow.map.move(
            CameraPosition(point, 17.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 1f),
            null
        )

        startSearch(point)
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        searchSession?.cancel()
        photoSession?.cancel()
        drivingSession?.cancel()
        popularPlacesLoader.cancel()
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mapInputListener?.let {
            mapView.mapWindow.map.removeInputListener(it)
        }

        searchSession?.cancel()
        photoSession?.cancel()
        drivingSession?.cancel()
        popularPlacesLoader.cancel()

        polylineMapObject?.let {
            mapView.mapWindow.map.mapObjects.remove(it)
        }
        polylineMapObject = null

        routeMarkersCollection?.let {
            mapView.mapWindow.map.mapObjects.remove(it)
        }
        routeMarkersCollection = null

        searchResultsCollection?.let {
            mapView.mapWindow.map.mapObjects.remove(it)
        }
        searchResultsCollection = null

        popularPlacesCollection?.let {
            mapView.mapWindow.map.mapObjects.remove(it)
        }
        popularPlacesCollection = null

        isInterestSearchInProgress = false
        isRouteBuildInProgress = false
        arePopularPlacesHiddenByRoute = false
        _binding = null
        currentPlace = null
        currentPlaceWorkingHoursText = null
    }

    private fun retryPendingRouteBuild() {
        val singlePlace = sharedViewModel.routePlace.value
        if (singlePlace != null) {
            buildRouteToSinglePlace(singlePlace)
            return
        }

        val places = sharedViewModel.routePlaces.value.orEmpty()
        if (places.isNotEmpty()) {
            buildRouteFromCurrentLocation(places)
        }
    }

    @SuppressLint("MissingPermission")
    private fun buildRouteFromCurrentLocation(places: List<SelectedPlace>) {
        if (places.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Нет точек для построения маршрута",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (isRouteBuildInProgress) {
            Toast.makeText(
                requireContext(),
                "Маршрут уже строится",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        isRouteBuildInProgress = true

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                val startPoint = if (location != null) {
                    resolveStartPoint(Point(location.latitude, location.longitude))
                } else {
                    tomskFallbackPoint
                }

                buildDrivingRoute(
                    startPoint = startPoint,
                    destinationPlaces = places,
                    routeTitle = "Подборка (${places.size} мест)",
                    onSuccess = { route ->
                        updateCollectionRouteInfo(route, startPoint, places.size)
                        sharedViewModel.clearRoutePlaces()

                        Toast.makeText(
                            requireContext(),
                            "Маршрут по подборке построен",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            .addOnFailureListener {
                buildDrivingRoute(
                    startPoint = tomskFallbackPoint,
                    destinationPlaces = places,
                    routeTitle = "Подборка (${places.size} мест)",
                    onSuccess = { route ->
                        updateCollectionRouteInfo(route, tomskFallbackPoint, places.size)
                        sharedViewModel.clearRoutePlaces()

                        Toast.makeText(
                            requireContext(),
                            "Маршрут по подборке построен",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
    }

    @SuppressLint("MissingPermission")
    private fun buildRouteToSinglePlace(place: SelectedPlace) {
        if (isRouteBuildInProgress) {
            Toast.makeText(
                requireContext(),
                "Маршрут уже строится",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        isRouteBuildInProgress = true

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                val startPoint = if (location != null) {
                    resolveStartPoint(Point(location.latitude, location.longitude))
                } else {
                    tomskFallbackPoint
                }

                buildDrivingRoute(
                    startPoint = startPoint,
                    destinationPlaces = listOf(place),
                    routeTitle = place.title,
                    onSuccess = { route ->
                        updateRouteInfo(route, startPoint, place.title)
                        sharedViewModel.clearRoutePlace()

                        Toast.makeText(
                            requireContext(),
                            "Маршрут до «${place.title}» построен",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            .addOnFailureListener {
                buildDrivingRoute(
                    startPoint = tomskFallbackPoint,
                    destinationPlaces = listOf(place),
                    routeTitle = place.title,
                    onSuccess = { route ->
                        updateRouteInfo(route, tomskFallbackPoint, place.title)
                        sharedViewModel.clearRoutePlace()

                        Toast.makeText(
                            requireContext(),
                            "Маршрут до «${place.title}» построен",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
    }

    private fun buildDrivingRoute(
        startPoint: Point,
        destinationPlaces: List<SelectedPlace>,
        routeTitle: String,
        onSuccess: (DrivingRoute) -> Unit
    ) {
        if (destinationPlaces.isEmpty()) {
            isRouteBuildInProgress = false
            Toast.makeText(
                requireContext(),
                "Нет точек назначения для маршрута",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        clearCurrentRoute(restorePopularPlaces = false)
        hidePopularPlacesOnMap()
        clearSearchResultMarkers()

        binding.cvSearchResults.visibility = View.GONE
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        currentPlace = null

        val requestPoints = mutableListOf<RequestPoint>()
        requestPoints.add(
            RequestPoint(
                startPoint,
                RequestPointType.WAYPOINT,
                null,
                null,
                null
            )
        )

        destinationPlaces.forEach { place ->
            requestPoints.add(
                RequestPoint(
                    Point(place.latitude, place.longitude),
                    RequestPointType.WAYPOINT,
                    null,
                    null,
                    null
                )
            )
        }

        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()

        drivingSession?.cancel()
        drivingSession = drivingRouter.requestRoutes(
            requestPoints,
            drivingOptions,
            vehicleOptions,
            object : DrivingSession.DrivingRouteListener {
                override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
                    isRouteBuildInProgress = false

                    if (routes.isEmpty()) {
                        hideRouteInfo()
                        restorePopularPlacesIfNeeded()
                        Toast.makeText(
                            requireContext(),
                            "Не удалось построить маршрут",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val route = routes.first()
                    renderRoute(
                        polyline = route.geometry,
                        startPoint = startPoint,
                        destinationPlaces = destinationPlaces
                    )

                    onSuccess(route)
                }

                override fun onDrivingRoutesError(error: com.yandex.runtime.Error) {
                    isRouteBuildInProgress = false
                    hideRouteInfo()
                    restorePopularPlacesIfNeeded()

                    Toast.makeText(
                        requireContext(),
                        "Ошибка построения маршрута",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun renderRoute(
        polyline: Polyline,
        startPoint: Point,
        destinationPlaces: List<SelectedPlace>
    ) {
        drawRoutePolyline(polyline)
        drawRouteMarkers(startPoint, destinationPlaces)
        moveCameraToRoute(polyline)
    }

    private fun clearCurrentRoute(restorePopularPlaces: Boolean = true) {
        drivingSession?.cancel()

        polylineMapObject?.let {
            mapView.mapWindow.map.mapObjects.remove(it)
        }
        polylineMapObject = null

        routeMarkersCollection?.let {
            mapView.mapWindow.map.mapObjects.remove(it)
        }
        routeMarkersCollection = null

        hideRouteInfo()

        if (restorePopularPlaces) {
            restorePopularPlacesIfNeeded()
        }
    }

    private fun drawRoutePolyline(polyline: Polyline) {
        polylineMapObject?.let {
            mapView.mapWindow.map.mapObjects.remove(it)
        }
        polylineMapObject = mapView.mapWindow.map.mapObjects.addPolyline(polyline)
    }

    private fun drawRouteMarkers(
        startPoint: Point,
        destinationPlaces: List<SelectedPlace>
    ) {
        routeMarkersCollection?.let {
            mapView.mapWindow.map.mapObjects.remove(it)
        }

        val collection = mapView.mapWindow.map.mapObjects.addCollection()
        routeMarkersCollection = collection

        collection.addPlacemark().apply {
            geometry = startPoint
            setIcon(
                ImageProvider.fromResource(requireContext(), R.drawable.ic_map),
                IconStyle().apply {
                    scale = 0.10f
                }
            )
            userData = "route_start"
        }

        destinationPlaces.forEachIndexed { index, place ->
            val isLast = index == destinationPlaces.lastIndex
            collection.addPlacemark().apply {
                geometry = Point(place.latitude, place.longitude)
                setIcon(
                    ImageProvider.fromResource(requireContext(), R.drawable.ic_map_with_point),
                    IconStyle().apply {
                        scale = if (isLast) 0.14f else 0.11f
                    }
                )
                userData = if (isLast) "route_end" else place
            }
        }
    }

    private fun moveCameraToRoute(polyline: Polyline) {
        val points = polyline.points
        if (points.isEmpty()) return

        var minLat = points.first().latitude
        var maxLat = points.first().latitude
        var minLon = points.first().longitude
        var maxLon = points.first().longitude

        points.forEach { point ->
            if (point.latitude < minLat) minLat = point.latitude
            if (point.latitude > maxLat) maxLat = point.latitude
            if (point.longitude < minLon) minLon = point.longitude
            if (point.longitude > maxLon) maxLon = point.longitude
        }

        val center = Point(
            (minLat + maxLat) / 2.0,
            (minLon + maxLon) / 2.0
        )

        val latDelta = maxLat - minLat
        val lonDelta = maxLon - minLon
        val maxDelta = maxOf(latDelta, lonDelta)

        val zoom = when {
            maxDelta < 0.01 -> 15.5f
            maxDelta < 0.03 -> 14.5f
            maxDelta < 0.08 -> 13.5f
            maxDelta < 0.2 -> 12.5f
            else -> 11.0f
        }

        mapView.mapWindow.map.move(
            CameraPosition(center, zoom, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 1.2f),
            null
        )
    }

    private fun checkArrivalAgainstWorkingHours(
        workingHoursText: String?,
        routeDurationSeconds: Int
    ): String? {
        if (workingHoursText.isNullOrBlank()) return null

        val normalized = workingHoursText.lowercase(Locale.getDefault())

        if (normalized.contains("круглосуточно")) return null
        if (normalized.contains("закрыто")) return "Место сейчас закрыто"

        val closingTime = extractClosingTime(workingHoursText) ?: return null

        val now = Calendar.getInstance()
        val arrival = now.clone() as Calendar
        arrival.add(Calendar.SECOND, routeDurationSeconds)

        val closing = now.clone() as Calendar
        closing.set(Calendar.HOUR_OF_DAY, closingTime.first)
        closing.set(Calendar.MINUTE, closingTime.second)
        closing.set(Calendar.SECOND, 0)
        closing.set(Calendar.MILLISECOND, 0)

        return if (arrival.after(closing)) {
            "Закроется раньше, чем вы прибудете (${formatTime(closingTime.first, closingTime.second)})"
        } else {
            null
        }
    }

    private fun extractClosingTime(workingHoursText: String): Pair<Int, Int>? {
        val text = workingHoursText
            .replace("Часы работы:", "", ignoreCase = true)
            .trim()

        val untilRegex = Regex("""до\s+(\d{1,2}):(\d{2})""", RegexOption.IGNORE_CASE)
        val rangeRegex = Regex("""(\d{1,2}):(\d{2})\s*[-–]\s*(\d{1,2}):(\d{2})""")
        val allTimesRegex = Regex("""(\d{1,2}):(\d{2})""")

        val untilMatch = untilRegex.find(text)
        if (untilMatch != null) {
            val hour = untilMatch.groupValues[1].toIntOrNull() ?: return null
            val minute = untilMatch.groupValues[2].toIntOrNull() ?: return null
            return hour to minute
        }

        val rangeMatch = rangeRegex.find(text)
        if (rangeMatch != null) {
            val hour = rangeMatch.groupValues[3].toIntOrNull() ?: return null
            val minute = rangeMatch.groupValues[4].toIntOrNull() ?: return null
            return hour to minute
        }

        val allTimes = allTimesRegex.findAll(text).toList()
        if (allTimes.size >= 2) {
            val last = allTimes.last()
            val hour = last.groupValues[1].toIntOrNull() ?: return null
            val minute = last.groupValues[2].toIntOrNull() ?: return null
            return hour to minute
        }

        return null
    }
}