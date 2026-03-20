package com.example.togetherapp.presentation.screens.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.togetherapp.R

class FriendsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Просто загружаем ваш XML файл
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }
}