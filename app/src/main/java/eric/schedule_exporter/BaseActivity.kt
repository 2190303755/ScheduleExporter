package eric.schedule_exporter

import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.View.OnApplyWindowInsetsListener
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import com.google.android.material.color.DynamicColors
import eric.schedule_exporter.util.INDICATOR_NAV_MODE_ANDROID
import eric.schedule_exporter.util.INDICATOR_NAV_MODE_HARMONY
import eric.schedule_exporter.util.STRING_NAV_MODE

abstract class BaseActivity : AppCompatActivity(), OnApplyWindowInsetsListener {
    var isIndicatorEnabled = false
        private set

    abstract fun applyContentInsets(window: View, insets: Insets)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        this.window.let {
            it.decorView.setOnApplyWindowInsetsListener(this)
            WindowCompat.setDecorFitsSystemWindows(it, false)
        }
    }

    final override fun onApplyWindowInsets(view: View, insets: WindowInsets): WindowInsets {
        isIndicatorEnabled = when (
            Settings.Secure.getInt(this.contentResolver, STRING_NAV_MODE, 0)
        ) {
            INDICATOR_NAV_MODE_ANDROID, INDICATOR_NAV_MODE_HARMONY -> true
            else -> false
        }
        this.applyContentInsets(
            view,
            WindowInsetsCompat.toWindowInsetsCompat(insets).getInsets(
                Type.systemBars() or Type.displayCutout() or Type.ime()
            )
        )
        return insets
    }
}