package com.driverapp.repartidor.ui.common

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.driverapp.repartidor.R
import com.driverapp.repartidor.databinding.ViewDialogConfirmBinding

class ConfirmDialog(
    private val context: Context,
    private val title: String,
    private val message: String,
    private val confirmText: String = "Confirmar",
    private val icon: Int = R.drawable.ic_fa_circle_question,
    private val iconBg: Int = R.drawable.bg_icon_blue,
    private val iconTint: Int = android.graphics.Color.rgb(59, 130, 246),
    private val confirmStyle: Boolean = true,
    private val onConfirm: () -> Unit = {},
) {

    fun show() {
        val binding = ViewDialogConfirmBinding.inflate(LayoutInflater.from(context))
        binding.confirmIconBg.setBackgroundResource(iconBg)
        binding.confirmIcon.setImageResource(icon)
        androidx.core.graphics.drawable.DrawableCompat.setTint(binding.confirmIcon.drawable.mutate(), iconTint)
        binding.confirmTitle.text = title
        binding.confirmMessage.text = message
        binding.confirmOk.text = confirmText
        binding.confirmOk.background = androidx.core.content.res.ResourcesCompat.getDrawable(
            context.resources,
            if (confirmStyle) R.drawable.bg_btn_primary else R.drawable.bg_btn_success,
            null
        )

        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()
        dialog.setCancelable(true)
        binding.confirmCancel.setOnClickListener { dialog.dismiss() }
        binding.confirmOk.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
        dialog.show()
    }
}