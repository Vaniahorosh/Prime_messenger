package com.messenger.prime

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.messenger.prime.databinding.ActivityChatPersonContentBinding
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

class ChatPersonActivity : AppCompatActivity() {

    private var contentBinding: ActivityChatPersonContentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            overrideActivityTransition(
                android.app.Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }
        
        setContentView(R.layout.activity_chat_person)

        setupEdgeToEdge(isDarkIcons = true)

        val name = intent.getStringExtra("EXTRA_CHAT_NAME") ?: "Контакт"
        val avatarUri = intent.getStringExtra("EXTRA_CHAT_AVATAR")

        findViewById<ComposeView>(R.id.composeRoot).setContent {
            val hazeState = remember { HazeState() }
            
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { _ ->
                        val view = layoutInflater.inflate(R.layout.activity_chat_person_content, null)
                        val binding = ActivityChatPersonContentBinding.bind(view)
                        contentBinding = binding

                        // Закрашиваем область статус-бара
                        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
                            val insetsType = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                            val params = binding.statusBarBackground.layoutParams
                            params.height = insetsType.top
                            binding.statusBarBackground.layoutParams = params
                            insets
                        }

                        binding.tvChatName.text = name
                        if (avatarUri != null && avatarUri.isNotEmpty()) {
                            try {
                                binding.ivChatAvatar.setImageURI(Uri.parse(avatarUri))
                            } catch (e: Exception) {
                                binding.ivChatAvatar.setImageResource(R.drawable.ic_person)
                            }
                        }

                        binding.btnBack.setOnClickListener {
                            onBackPressedDispatcher.onBackPressed()
                        }
                        
                        view
                    },
                    modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)
                )

                // Размытие для системной панели навигации
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp)
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = 20.dp,
                                noiseFactor = 0f,
                                tint = dev.chrisbanes.haze.HazeTint(Color(0x0DFFFFFF))
                            )
                        )
                )
            }
        }

        // Возвращаем Slidr для всех версий
        val slidrConfig = SlidrConfig.Builder()
            .position(SlidrPosition.LEFT)
            .build()
        Slidr.attach(this, slidrConfig)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
