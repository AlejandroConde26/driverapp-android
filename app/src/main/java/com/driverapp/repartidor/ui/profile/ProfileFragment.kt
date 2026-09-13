package com.driverapp.repartidor.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.driverapp.repartidor.R
import com.driverapp.repartidor.databinding.FragmentProfileBinding
import com.driverapp.repartidor.ui.common.ConfirmDialog
import com.driverapp.repartidor.ui.login.LoginActivity
import com.driverapp.repartidor.ui.main.AppViewModel
import com.driverapp.repartidor.ui.main.MainActivity
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val vm: AppViewModel by activityViewModels()

    private val uiPrefs by lazy { requireContext().getSharedPreferences("driverapp_ui", AppCompatActivity.MODE_PRIVATE) }
    private val notifPrefs by lazy { requireContext().getSharedPreferences("driverapp_notifications", AppCompatActivity.MODE_PRIVATE) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.logoutButton.setOnClickListener {
            ConfirmDialog(
                requireContext(),
                "Cerrar Sesión",
                "¿Seguro que quieres cerrar tu sesión?",
                confirmText = "Cerrar sesión",
                icon = R.drawable.ic_fa_right_from_bracket,
                iconBg = R.drawable.bg_icon_red,
                iconTint = android.graphics.Color.rgb(229, 57, 53),
            ) {
                vm.logout()
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }.show()
        }

        binding.settingsProfile.setOnClickListener { editProfileDialog() }
        binding.settingsPassword.setOnClickListener { passwordDialog() }
        binding.settingsPayments.setOnClickListener { infoDialog("Métodos de Pago", "Efectivo y tarjeta al repartidor. Las liquidaciones se muestran en Ganancias.") }
        binding.settingsHelp.setOnClickListener { infoDialog("Centro de Ayuda", "Revisa la documentación o contacta a soporte para resolver dudas.") }
        binding.settingsContact.setOnClickListener { infoDialog("Contactar Soporte", "soporte@driverapp.com\n+1 800 555 0199") }
        binding.settingsTerms.setOnClickListener { infoDialog("Términos y Condiciones", "Al usar DriverApp aceptas los términos del servicio. Versión 1.0.") }

        binding.settingsVehicle.setOnClickListener { vehicleDialog() }

        binding.darkModeSwitch.setOnCheckedChangeListener { _, checked ->
            uiPrefs.edit().putBoolean("dark", checked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.notificationsSwitch.setOnCheckedChangeListener { _, checked ->
            notifPrefs.edit().putBoolean("on", checked).apply()
            vm.toast(if (checked) "Notificaciones activadas" else "Notificaciones desactivadas")
        }

        binding.locationSwitch.setOnCheckedChangeListener { _, checked ->
            (activity as? MainActivity)?.setLocationEnabled(checked)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.user.collect { u -> u?.let { renderUser(it) } } }
                launch { vm.profileStatsFlow.collect { (orders, rating, tips) ->
                    binding.statOrders.text = "${orders}"
                    binding.statRating.text = String.format(Locale.ROOT, "%.1f", rating)
                    binding.statTips.text = "$" + String.format(Locale.ROOT, "%.2f", tips)
                } }
            }
        }

        SyncSwitches()
        vm.user.value?.let { renderUser(it) }
    }

    private fun SyncSwitches() {
        binding.darkModeSwitch.isChecked = uiPrefs.getBoolean("dark", false)
        binding.notificationsSwitch.isChecked = notifPrefs.getBoolean("on", true)
        binding.locationSwitch.isChecked = (activity as? MainActivity)?.locationToggleState() ?: true
    }

    private fun renderUser(u: com.driverapp.repartidor.data.User) {
        binding.profileName.text = u.name
        binding.profileRole.text = "Repartidor · ${u.email ?: ""}"
        binding.profileAvatar.text = u.name.take(2).uppercase(Locale.ROOT)
        bindVehicle(u.vehicle)
    }

    private fun bindVehicle(vehicle: String?) {
        val v = vehicle ?: "Bicicleta"
        binding.vehicleValue.text = v
        val icon = when (v.lowercase()) {
            "moto" -> R.drawable.ic_fa_motorcycle
            "auto", "carro" -> R.drawable.ic_fa_car
            else -> R.drawable.ic_fa_bicycle
        }
        binding.vehicleIcon.setImageResource(icon)
    }

    private fun vehicleDialog() {
        val current = vm.user.value?.vehicle ?: "Bicicleta"
        val options = listOf("Bicicleta", "Moto", "Carro")
        val selected = options.indexOfFirst { it.equals(current, true) }.takeIf { it >= 0 } ?: 0
        AlertDialog.Builder(requireContext())
            .setTitle("Modo Vehículo")
            .setSingleChoiceItems(options.toTypedArray(), selected) { dialog, which ->
                val value = options[which]
                dialog.dismiss()
                vm.updateVehicle(value)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editProfileDialog() {
        val u = vm.user.value ?: return
        val nameInput = android.widget.EditText(requireContext()).apply {
            setText(u.name)
            hint = "Nombre"
        }
        val phoneInput = android.widget.EditText(requireContext()).apply {
            setText(u.phone ?: "")
            hint = "Teléfono"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 20, 60, 0)
            addView(nameInput, android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(phoneInput, android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Editar Perfil")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                vm.updateProfile(nameInput.text?.toString(), phoneInput.text?.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun passwordDialog() {
        val pass = android.widget.EditText(requireContext()).apply {
            hint = "Nueva contraseña"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 20, 60, 0)
            addView(pass, android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Cambiar Contraseña")
            .setView(layout)
            .setPositiveButton("Cambiar") { _, _ ->
                vm.toast(if (pass.text?.isNotBlank() == true) "Contraseña actualizada" else "Ingresa una contraseña")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun infoDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}