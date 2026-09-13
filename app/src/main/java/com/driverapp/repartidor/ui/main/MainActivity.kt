package com.driverapp.repartidor.ui.main

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.driverapp.repartidor.R
import com.driverapp.repartidor.data.LiveLocation
import com.driverapp.repartidor.databinding.ActivityMainBinding
import com.driverapp.repartidor.databinding.ViewToastBinding
import com.driverapp.repartidor.fcm.FirebaseRegistration
import com.driverapp.repartidor.ui.earnings.EarningsFragment
import com.driverapp.repartidor.ui.orders.OrdersFragment
import com.driverapp.repartidor.ui.profile.ProfileFragment
import com.driverapp.repartidor.ui.tracking.TrackingFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: AppViewModel by viewModels()

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
                val tracker = LiveLocation.tracker()
                tracker?.start()
                vm.toast("Ubicación en tiempo real activada")
            } else {
                locationPrefs.edit().putBoolean("enabled", false).apply()
                vm.toast("Permiso de ubicación denegado")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        LiveLocation.init(this)
        FirebaseRegistration.ensureToken(vm)
        FirebaseRegistration.ensureNotificationPermission(this)

        binding.statusPill.setOnClickListener { vm.toggleStatus() }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_orders -> {
                    showFragment(ordersFrag)
                    bindHeader("¡Hola, ${firstName()}! 👋", getString(R.string.subtitle_orders))
                    vm.loadAll()
                    true
                }
                R.id.nav_map -> {
                    showFragment(trackingFrag)
                    bindHeader(getString(R.string.tab_map), getString(R.string.subtitle_map))
                    vm.refreshActive()
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
                    vm.refreshProfileStats()
                    true
                }
                else -> false
            }
        }

        observeState()
        startLocationLoop()
        binding.bottomNav.selectedItemId = R.id.nav_orders
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
                launch { vm.error.collect { msg -> msg?.let { showToast(it) } } }
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
                val tracker = LiveLocation.tracker() ?: continue
                if (!tracker.isTracking()) continue
                val loc = tracker.location.value ?: continue
                if (tracker.shouldReport(30_000L)) {
                    vm.reportLocation(loc.latitude, loc.longitude)
                    tracker.markReported()
                }
            }
        }
    }

    fun setLocationEnabled(enabled: Boolean) {
        locationPrefs.edit().putBoolean("enabled", enabled).apply()
        val tracker = LiveLocation.tracker()
        if (enabled) {
            if (tracker?.hasPermission() == true) {
                tracker.start()
                vm.toast("Ubicación en tiempo real activada")
            } else {
                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            tracker?.stop()
            vm.toast("Ubicación en tiempo real desactivada")
        }
    }

    fun locationToggleState(): Boolean = LiveLocation.enabled()

    fun goToMap() {
        binding.bottomNav.selectedItemId = R.id.nav_map
    }

    override fun onBackPressed() {
        val frag = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (frag is OrdersFragment && frag.onBack()) return
        super.onBackPressed()
    }

    override fun onDestroy() {
        LiveLocation.tracker()?.stop()
        super.onDestroy()
    }
}