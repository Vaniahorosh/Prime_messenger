package com.messenger.prime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ChatListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        // Делаем статус-бар белым, а иконки темными (true)
        setDynamicStatusBar(R.color.white, true)
    }
}