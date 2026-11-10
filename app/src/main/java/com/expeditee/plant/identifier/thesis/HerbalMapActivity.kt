package com.expeditee.plant.identifier.thesis

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.expeditee.plant.identifier.R
import com.expeditee.plant.identifier.common.BaseActivity
import com.expeditee.plant.identifier.common.BaseViewModel
import com.expeditee.plant.identifier.databinding.ActivityHerbalMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.Normalizer

class HerbalMapActivity :
    BaseActivity<ActivityHerbalMapBinding, HerbalMapViewModel, HerbalMapState>(),
    OnMapReadyCallback {

    companion object {
        private const val EXTRA_PLANT_NAME = "extra_plant_name"
        private const val EXTRA_SCIENTIFIC_NAME = "extra_scientific_name"
        private const val EXTRA_IMAGE_URL = "extra_image_url"

        private val VIETNAM_BOUNDS = LatLngBounds(
            LatLng(8.18, 102.0),
            LatLng(23.55, 109.8),
        )

        fun start(
            context: Context,
            plantName: String = "",
            scientificName: String = "",
            imageUrl: String = "",
        ) {
            context.startActivity(
                Intent(context, HerbalMapActivity::class.java)
                    .putExtra(EXTRA_PLANT_NAME, plantName)
                    .putExtra(EXTRA_SCIENTIFIC_NAME, scientificName)
                    .putExtra(EXTRA_IMAGE_URL, imageUrl),
            )
        }
    }

    private var googleMap: GoogleMap? = null
    private var allPlants = VietnamHerbalMapData.items
    private var currentPlants = VietnamHerbalMapData.items
    private val markers = mutableMapOf<Marker, HerbalRegionItem>()
    private val circles = mutableMapOf<Circle, HerbalRegionItem>()
    private val adapter = HerbalRegionAdapter { selectItem(it, moveCamera = true) }
    private val leafIcon: BitmapDescriptor by lazy { BitmapDescriptorFactory.fromBitmap(createLeafMarkerBitmap()) }

    override fun provideLayout() = R.layout.activity_herbal_map
    override fun getLazyViewModel() = viewModel<HerbalMapViewModel>()
    override fun isFitsSystemWindows() = false

    override fun initViews(savedInstanceState: Bundle?) {
        viewBinding.mapView.onCreate(savedInstanceState)
        viewBinding.mapView.getMapAsync(this)
        applyInsetToView(viewBinding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
        }

        viewBinding.toolbar.setNavigationOnClickListener { finish() }
        viewBinding.rvHerbs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBinding.rvHerbs.adapter = adapter
        viewBinding.btnZoomIn.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }
        viewBinding.btnZoomOut.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }
        viewBinding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                submitFiltered(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val initial = intent.getStringExtra(EXTRA_PLANT_NAME).orEmpty()
        val scientificName = intent.getStringExtra(EXTRA_SCIENTIFIC_NAME).orEmpty()
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL).orEmpty()
        val catalogImageUrl = resolveCatalogImage(initial, scientificName)
        allPlants = VietnamHerbalMapData.withScannedPlant(
            initial,
            scientificName,
            catalogImageUrl.ifBlank { imageUrl },
        )
        if (initial.isNotBlank()) viewBinding.edtSearch.setText(initial) else submitFiltered("")
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isTiltGesturesEnabled = false
        map.setLatLngBoundsForCameraTarget(VIETNAM_BOUNDS)
        map.setMinZoomPreference(5f)
        map.setMaxZoomPreference(14f)
        map.setMapStyle(
            MapStyleOptions(
                """
                [
                  {"featureType":"poi","stylers":[{"visibility":"off"}]},
                  {"featureType":"transit","stylers":[{"visibility":"off"}]},
                  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#BFE5F4"}]},
                  {"featureType":"landscape.natural","elementType":"geometry","stylers":[{"color":"#E9F4DE"}]},
                  {"featureType":"administrative.country","elementType":"geometry.stroke","stylers":[{"color":"#71A968"},{"weight":1.2}]}
                ]
                """.trimIndent(),
            ),
        )
        map.setOnMarkerClickListener { marker ->
            markers[marker]?.let { selectItem(it, moveCamera = false) }
            true
        }
        map.setOnCircleClickListener { circle ->
            circles[circle]?.let { selectItem(it, moveCamera = true) }
        }
        renderMap(focusItem = currentPlants.firstOrNull(), moveToVietnam = true)
    }

    private fun submitFiltered(query: String) {
        val key = normalize(query)
        val filtered = if (key.isBlank()) {
            allPlants
        } else {
            allPlants.filter {
                normalize(it.name).contains(key) ||
                    normalize(it.scientificName).contains(key) ||
                    normalize(it.region).contains(key) ||
                    normalize(it.province).contains(key)
            }.ifEmpty { allPlants }
        }
        currentPlants = filtered
        adapter.submit(filtered)
        renderMap(focusItem = filtered.firstOrNull(), moveToVietnam = key.isBlank())
        filtered.firstOrNull()?.let { bindSelected(it) }
    }

    private fun renderMap(focusItem: HerbalRegionItem?, moveToVietnam: Boolean) {
        val map = googleMap ?: return
        markers.clear()
        circles.clear()
        map.clear()

        currentPlants.forEach { item ->
            val center = item.latLng
            val circle = map.addCircle(
                CircleOptions()
                    .center(center)
                    .radius(item.radiusMeters)
                    .strokeColor(Color.rgb(69, 168, 72))
                    .strokeWidth(3f)
                    .fillColor(Color.argb(58, 83, 181, 82))
                    .clickable(true),
            )
            val marker = map.addMarker(
                MarkerOptions()
                    .position(center)
                    .title(item.name)
                    .snippet(item.region)
                    .anchor(0.5f, 0.5f)
                    .icon(leafIcon),
            )
            if (marker != null) {
                marker.tag = item
                markers[marker] = item
            }
            circles[circle] = item
        }

        when {
            moveToVietnam -> map.moveCamera(CameraUpdateFactory.newLatLngBounds(VIETNAM_BOUNDS, 48))
            focusItem != null -> moveCameraTo(focusItem, animate = false)
        }
    }

    private fun selectItem(item: HerbalRegionItem, moveCamera: Boolean) {
        bindSelected(item)
        viewBinding.rvHerbs.smoothScrollToPosition(currentPlants.indexOf(item).coerceAtLeast(0))
        markers.keys.firstOrNull { markers[it] == item }?.showInfoWindow()
        if (moveCamera) moveCameraTo(item, animate = true)
    }

    private fun moveCameraTo(item: HerbalRegionItem, animate: Boolean) {
        val update = CameraUpdateFactory.newLatLngZoom(item.latLng, item.zoom)
        if (animate) {
            googleMap?.animateCamera(update)
        } else {
            googleMap?.moveCamera(update)
        }
    }

    private fun bindSelected(item: HerbalRegionItem) {
        viewBinding.tvSelectedName.text = item.name
        viewBinding.tvSelectedScientific.text = item.scientificName
        viewBinding.tvSelectedRegion.text = "${item.region}: ${item.province}"
        viewBinding.tvSelectedNote.text = item.note
        PlantImages.load(viewBinding.ivSelectedHerb, item.imageUrl)
    }

    private fun resolveCatalogImage(plantName: String, scientificName: String): String {
        val localPlant = ThesisPlantCatalog.findByName(
            context = this,
            scientific = scientificName,
            commonNames = listOf(plantName),
        ) ?: ThesisPlantCatalog.findByQrOrId(this, plantName)

        return localPlant?.imageUrl.orEmpty()
    }

    private fun createLeafMarkerBitmap(): Bitmap {
        val size = 84
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(58, 0, 0, 0) }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(63, 156, 62) }
        val stem = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        val leaf = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(208, 239, 128) }
        canvas.drawCircle(size / 2f, size / 2f + 3f, 35f, shadow)
        canvas.drawCircle(size / 2f, size / 2f, 34f, ring)
        canvas.drawCircle(size / 2f, size / 2f, 27f, bg)
        canvas.drawLine(42f, 58f, 42f, 30f, stem)
        drawLeaf(canvas, 33f, 39f, -35f, 15f, leaf)
        drawLeaf(canvas, 52f, 35f, 30f, 16f, leaf)
        drawLeaf(canvas, 48f, 50f, 32f, 12f, leaf)
        return bitmap
    }

    private fun drawLeaf(canvas: Canvas, cx: Float, cy: Float, rotate: Float, size: Float, paint: Paint) {
        val leaf = Path().apply {
            moveTo(cx, cy - size)
            cubicTo(cx - size, cy - size * 0.55f, cx - size, cy + size * 0.55f, cx, cy + size)
            cubicTo(cx + size, cy + size * 0.55f, cx + size, cy - size * 0.55f, cx, cy - size)
            close()
        }
        canvas.save()
        canvas.rotate(rotate, cx, cy)
        canvas.drawPath(leaf, paint)
        canvas.restore()
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value.lowercase().trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
    }

    override fun onStart() {
        super.onStart()
        viewBinding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        viewBinding.mapView.onResume()
    }

    override fun onPause() {
        viewBinding.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        viewBinding.mapView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        viewBinding.mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        viewBinding.mapView.onLowMemory()
    }
}

class HerbalMapViewModel(application: android.app.Application) :
    BaseViewModel<HerbalMapState>(application) {
    override fun onState(state: HerbalMapState) = Unit
}

object HerbalMapState : BaseViewModel.State

data class HerbalRegionItem(
    val name: String,
    val scientificName: String,
    val region: String,
    val province: String,
    val note: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val imageUrl: String,
    val zoom: Float = 7f,
) {
    val latLng: LatLng get() = LatLng(latitude, longitude)
}

object VietnamHerbalMapData {
    val items = listOf(
        HerbalRegionItem(
            "Diếp cá",
            "Houttuynia cordata",
            "Miền Bắc",
            "Hà Nội, Hưng Yên, Bắc Giang",
            "Rộng khắp cả nước, thường mọc ở vùng ẩm.",
            21.03,
            105.83,
            155_000.0,
            "https://upload.wikimedia.org/wikipedia/commons/2/27/Houttuynia_cordata.jpg",
        ),
        HerbalRegionItem(
            "Đinh lăng",
            "Polyscias fruticosa",
            "Bắc Trung Bộ",
            "Thanh Hóa, Nghệ An",
            "Ưa khí hậu ấm, đất tơi xốp, thường trồng trong vườn nhà.",
            19.35,
            105.45,
            145_000.0,
            "https://upload.wikimedia.org/wikipedia/commons/8/80/Polyscias_fruticosa.jpg",
        ),
        HerbalRegionItem(
            "Nghệ vàng",
            "Curcuma longa",
            "Miền Trung",
            "Quảng Nam, Quảng Ngãi, Bình Định",
            "Hợp đất thoát nước, nhiều nắng, trồng phổ biến làm gia vị và dược liệu.",
            15.75,
            108.20,
            170_000.0,
            "https://upload.wikimedia.org/wikipedia/commons/5/5b/Curcuma_longa_roots.jpg",
        ),
        HerbalRegionItem(
            "Rau má",
            "Centella asiatica",
            "Tây Nguyên",
            "Đắk Lắk, Lâm Đồng",
            "Mọc tốt nơi ẩm mát, có thể trồng chậu hoặc luống thấp.",
            12.70,
            108.10,
            180_000.0,
            "https://upload.wikimedia.org/wikipedia/commons/2/25/Centella_asiatica.jpg",
        ),
        HerbalRegionItem(
            "Sả",
            "Cymbopogon citratus",
            "Nam Bộ",
            "Đồng Nai, Long An, Cần Thơ",
            "Ưa nắng, chịu hạn khá, hay trồng ven vườn và vùng đất cao ráo.",
            10.85,
            106.65,
            190_000.0,
            "https://upload.wikimedia.org/wikipedia/commons/8/89/Cymbopogon_citratus.jpg",
        ),
        HerbalRegionItem(
            "Lá lốt",
            "Piper lolot",
            "Đồng bằng sông Cửu Long",
            "An Giang, Tiền Giang, Vĩnh Long",
            "Cây ưa bóng nhẹ và đất ẩm, thường trồng quanh nhà.",
            10.35,
            105.55,
            145_000.0,
            "https://upload.wikimedia.org/wikipedia/commons/b/b1/Piper_lolot.jpg",
        ),
    )

    fun withScannedPlant(name: String, scientificName: String, imageUrl: String): List<HerbalRegionItem> {
        val scan = buildScannedItem(name, scientificName, imageUrl) ?: return items
        val alreadyMapped = items.firstOrNull { item ->
            samePlant(item.name, scan.name) ||
                samePlant(item.scientificName, scan.scientificName) ||
                samePlant(item.name, scientificName) ||
                samePlant(item.scientificName, name)
        }
        if (alreadyMapped != null) {
            return listOf(alreadyMapped) + items.filterNot {
                samePlant(it.name, alreadyMapped.name) &&
                    samePlant(it.scientificName, alreadyMapped.scientificName)
            }
        }
        return listOf(scan) + items
    }

    private fun buildScannedItem(name: String, scientificName: String, imageUrl: String): HerbalRegionItem? {
        val display = name.trim().ifBlank { scientificName.trim() }
        if (display.isBlank()) return null
        val science = scientificName.trim().ifBlank { "Tên khoa học đang cập nhật" }
        val key = normalize("$display $science")
        val region = regionFor(key)
        return HerbalRegionItem(
            name = display,
            scientificName = science,
            region = region.name,
            province = region.province,
            note = "Cây vừa nhận diện. Vùng khoanh xanh là gợi ý phân bố / nơi trồng thường gặp tại Việt Nam, cần đối chiếu thêm theo địa phương.",
            latitude = region.latitude,
            longitude = region.longitude,
            radiusMeters = region.radiusMeters,
            imageUrl = imageUrl.ifBlank { "plant_images/_default.jpg" },
            zoom = region.zoom,
        )
    }

    private fun regionFor(key: String): RegionHint {
        return when {
            key.hasAnyPlantAlias("diep ca", "houttuynia cordata", "houttuynia") ->
                RegionHint("Rộng khắp Việt Nam", "Nơi ẩm: Bắc Bộ, Trung Bộ, Nam Bộ", 16.4, 106.2, 760_000.0, 5.8f)
            key.hasAnyPlantAlias("rau ma", "centella asiatica", "centella") ->
                RegionHint("Tây Nguyên và Nam Bộ", "Lâm Đồng, Đắk Lắk, Đồng Nai", 12.2, 107.4, 300_000.0, 6.4f)
            key.hasAnyPlantAlias("sa", "cymbopogon citratus", "cymbopogon") ->
                RegionHint("Nam Bộ", "Đồng Nai, Long An, Cần Thơ", 10.7, 106.2, 250_000.0, 6.7f)
            key.hasAnyPlantAlias("nghe", "curcuma longa", "curcuma") ->
                RegionHint("Miền Trung", "Quảng Nam, Quảng Ngãi, Bình Định", 15.2, 108.5, 260_000.0, 6.5f)
            key.hasAnyPlantAlias("dinh lang", "polyscias fruticosa", "polyscias") ->
                RegionHint("Bắc Trung Bộ", "Thanh Hóa, Nghệ An", 19.3, 105.4, 190_000.0, 6.9f)
            key.hasAnyPlantAlias("la lot", "piper lolot") ->
                RegionHint("Đồng bằng sông Cửu Long", "An Giang, Tiền Giang, Vĩnh Long", 10.3, 105.7, 190_000.0, 6.9f)
            key.hasAnyPlantAlias("hoa giay", "bougainvillea glabra", "bougainvillea") ->
                RegionHint("Đô thị và vùng nắng ấm", "Nam Trung Bộ, Nam Bộ, ven biển", 12.7, 109.0, 330_000.0, 6.3f)
            key.hasAnyPlantAlias("hoa hong", "rosa chinensis", "rosa") ->
                RegionHint("Vùng mát và nhà vườn", "Đà Lạt, Sa Pa, Hà Nội", 11.94, 108.45, 185_000.0, 7f)
            key.hasAnyPlantAlias("luoi ho", "sansevieria", "dracaena trifasciata") ->
                RegionHint("Cây cảnh trồng rộng rãi", "Nhà vườn và đô thị trên cả nước", 16.0, 106.4, 700_000.0, 5.9f)
            key.hasAnyPlantAlias("trau ba", "epipremnum aureum", "epipremnum", "pothos") ->
                RegionHint("Cây cảnh trồng rộng rãi", "Vườn nhà, ban công, văn phòng", 14.0, 107.0, 580_000.0, 6.0f)
            else ->
                RegionHint("Việt Nam", "Vùng phân bố tham khảo theo ảnh vừa scan", 16.0, 106.6, 620_000.0, 6.0f)
        }
    }

    private fun samePlant(a: String, b: String): Boolean {
        val left = normalize(a)
        val right = normalize(b)
        if (left.isBlank() || right.isBlank()) return false
        return left == right ||
            left.hasPlantAlias(right) ||
            right.hasPlantAlias(left)
    }

    private fun String.hasAnyPlantAlias(vararg aliases: String): Boolean {
        return aliases.any { hasPlantAlias(normalize(it)) }
    }

    private fun String.hasPlantAlias(alias: String): Boolean {
        if (alias.isBlank()) return false
        if (this == alias) return true
        val aliasTokens = alias.split(' ').filter { it.isNotBlank() }
        if (aliasTokens.size > 1) {
            return " $this ".contains(" $alias ")
        }
        return split(' ')
            .filter { it.isNotBlank() }
            .any { token -> token == alias }
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value.lowercase().trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace('đ', 'd')
    }

    private data class RegionHint(
        val name: String,
        val province: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Double,
        val zoom: Float,
    )
}

private class HerbalRegionAdapter(
    private val onClick: (HerbalRegionItem) -> Unit,
) : RecyclerView.Adapter<HerbalRegionAdapter.VH>() {
    private val items = mutableListOf<HerbalRegionItem>()

    fun submit(list: List<HerbalRegionItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_herbal_region, parent, false))
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], onClick)

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: HerbalRegionItem, onClick: (HerbalRegionItem) -> Unit) {
            itemView.findViewById<TextView>(R.id.tvHerbName).text = item.name
            itemView.findViewById<TextView>(R.id.tvHerbScientific).text = item.scientificName
            itemView.findViewById<TextView>(R.id.tvHerbRegion).text = "${item.region} · ${item.province}"
            itemView.findViewById<TextView>(R.id.tvHerbNote).text = item.note
            PlantImages.load(itemView.findViewById<ImageView>(R.id.ivHerb), item.imageUrl)
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
