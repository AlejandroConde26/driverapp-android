package com.driverapp.repartidor.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.driverapp.repartidor.App
import com.driverapp.repartidor.R
import com.driverapp.repartidor.data.firebase.FcmTokenHolder
import com.driverapp.repartidor.data.firebase.FirebaseConfig
import com.driverapp.repartidor.databinding.ActivityMainBinding
import com.driverapp.repartidor.databinding.ViewToastBinding
import com.driverapp.repartidor.domain.repository.LocationRepository
import com.driverapp.repartidor.ui.earnings.EarningsFragment
import com.driverapp.repartidor.ui.earnings.EarningsViewModel
import com.driverapp.repartidor.ui.orders.OrdersFragment
import com.driverapp.repartidor.ui.orders.OrdersViewModel
import com.driverapp.repartidor.ui.profile.ProfileFragment
import com.driverapp.repartidor.ui.profile.ProfileViewModel
import com.driverapp.repartidor.ui.tracking.TrackingFragment
import com.driverapp.repartidor.ui.tracking.TrackingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val container get() = (application as App).container

    private val vm: MainViewModel by viewModels { container.mainViewModelFactory() }
    private val ordersVm: OrdersViewModel by viewModels { container.ordersViewModelFactory() }
    private val trackingVm: TrackingViewModel by viewModels { container.trackingViewModelFactory() }
    val profileVm: ProfileViewModel by viewModels { container.profileViewModelFactory() }

    private val locationRepo: LocationRepository by lazy { container.locationRepository }

    private val ordersFrag = OrdersFragment()
    private val trackingFrag = TrackingFragment()
    private val earningsFrag = EarningsFragment()
    private val profileFrag = ProfileFragment()

    private val locationPrefs by lazy {
        getSharedPreferences("driverapp_location", MODE_PRIVATE)
    }

    private val locationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                locationRepo.start()
                vm.toast("Ubicación en tiempo real activada")
            } else {
                locationPrefs.edit().putBoolean("enabled", false).apply()
                vm.toast("Permiso de ubicación denegado")
            }
        }

    private val notifLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureFcmToken()
        ensureNotificationPermission()

        binding.statusPill.setOnClickListener { vm.toggleStatus() }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_orders -> {
                    showFragment(ordersFrag)
                    bindHeader("¡Hola, ${firstName()}! 👋", getString(R.string.subtitle_orders))
                    ordersVm.loadAll()
                    true
                }
                R.id.nav_map -> {
                    showFragment(trackingFrag)
                    bindHeader(getString(R.string.tab_map), getString(R.string.subtitle_map))
                    trackingVm.refreshActive()
                    true
                }
                R.id.nav_earnings -> {
                    showFragment(earningsFrag)
                    bindHeader("Mis Ganancias", getString(R.string.subtitle_earnings))
                    true
                }
                R.id.nav_profile -> {
                    showFragment(profileFrag)
                    bindHeader("Mi Perfil", getString(R.string.subtitle_profile))
                    profileVm.refreshAll()
                    true
                }
                else -> false
            }
        }

        observeState()
        startLocationLoop()
        binding.bottomNav.selectedItemId = R.id.nav_orders
    }

    fun ordersViewModel(): OrdersViewModel = ordersVm
    fun trackingViewModel(): TrackingViewModel = trackingVm
    fun mainViewModel(): MainViewModel = vm
    fun earningsViewModelFactory() = container.earningsViewModelFactory()

    private fun ensureFcmToken() {
        if (!FirebaseConfig.isConfigured()) return
        lifecycleScope.launch {
            val token = container.firebaseMessaging.fetchToken() ?: return@launch
            FcmTokenHolder.token = token
            vm.updateFcmToken(token)
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                notifLauncher.launch(permission)
            }
        }
    }

    private fun firstName(): String =
        vm.user.value?.name?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Repartidor"

    private fun bindHeader(title: String, subtitle: String) {
        binding.headerTitle.text = title
        binding.headerSubtitle.text = subtitle
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commitAllowingStateLoss()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.online.collect { bindStatus(it) } }
                launch {
                    container.messenger.message.collect { msg ->
                        msg?.let {
                            showToast(it)
                            container.messenger.consume()
                        }
                    }
                }
            }
        }
    }

    private fun bindStatus(online: Boolean) {
        if (online) {
            binding.statusPill.setBackgroundResource(R.drawable.bg_status_online)
            binding.dotStatus.setBackgroundResource(R.drawable.bg_dot_green)
            binding.statusText.text = getString(R.string.status_online)
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.success))
        } else {
            binding.statusPill.setBackgroundResource(R.drawable.bg_status_offline)
            binding.dotStatus.setBackgroundResource(R.drawable.bg_dot_red)
            binding.statusText.text = getString(R.string.status_offline)
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }
    }

    private fun showToast(message: String) {
        val toast = ViewToastBinding.inflate(LayoutInflater.from(this))
        toast.toastMessage.text = message

        val style = when {
            message.startsWith("Error") -> Triple(
                R.drawable.bg_icon_red, R.drawable.ic_fa_circle_exclamation,
                ContextCompat.getColor(this, R.color.primary)
            )
            message.startsWith("¡") -> Triple(
                R.drawable.bg_icon_green, R.drawable.ic_fa_circle_check,
                ContextCompat.getColor(this, R.color.success)
            )
            else -> Triple(R.drawable.bg_icon_blue, R.drawable.ic_fa_circle_check, ContextCompat.getColor(this, R.color.info))
        }
        toast.toastIconBg.setBackgroundResource(style.first)
        toast.toastIcon.setImageResource(style.second)
        androidx.core.graphics.drawable.DrawableCompat.setTint(toast.toastIcon.drawable.mutate(), style.third)

        binding.toastContainer.removeAllViews()
        binding.toastContainer.addView(toast.root, android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.toastContainer.isVisible = true
        toast.root.alpha = 0f
        toast.root.translationY = -60f
        toast.root.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()

        val dismiss: () -> Unit = {
            toast.root.animate().alpha(0f).translationY(-60f).setDuration(180)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    binding.toastContainer.removeView(toast.root)
                    if (binding.toastContainer.childCount == 0) binding.toastContainer.isVisible = false
                }
                .start()
        }
        toast.toastClose.setOnClickListener { dismiss() }
        lifecycleScope.launch {
            delay(3200)
            if (toast.root.parent != null) dismiss()
        }
    }

    private fun startLocationLoop() {
        lifecycleScope.launch {
            while (isActive) {
                delay(10_000)
                if (!locationRepo.isTracking()) continue
                val loc = locationRepo.location.value ?: continue
                if (locationRepo.shouldReport(30_000L)) {
                    vm.reportLocation(loc.lat, loc.lng)
                    locationRepo.markReported()
                }
            }
        }
    }

    fun setLocationEnabled(enabled: Boolean) {
        locationPrefs.edit().putBoolean("enabled", enabled).apply()
        if (enabled) {
            if (locationRepo.hasPermission()) {
                locationRepo.start()
                vm.toast("Ubicación en tiempo real activada")
            } else {
                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            locationRepo.stop()
            vm.toast("Ubicación en tiempo real desactivada")
        }
    }

    fun locationToggleState(): Boolean = locationRepo.isTracking()

    fun goToMap() {
        binding.bottomNav.selectedItemId = R.id.nav_map
    }

    override fun onBackPressed() {
        val frag = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (frag is OrdersFragment && frag.onBack()) return
        super.onBackPressed()
    }

    override fun onDestroy() {
        locationRepo.stop()
        super.onDestroy()
    }
}

// Acceso compartido para los fragments (mismo scope de Activity + misma factory).
fun OrdersFragment.ordersVm(): OrdersViewModel =
    (activity as MainActivity).ordersViewModel()

fun TrackingFragment.trackingVm(): TrackingViewModel =
    (activity as MainActivity).trackingViewModel()

fun androidx.fragment.app.Fragment.mainVm(): MainViewModel =
    (activity as MainActivity).mainViewModel()
