package com.gchan.sts2_sherpa.ui

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun darkOutlinedTextFieldColors(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFFFFEAD0),
        unfocusedTextColor = Color(0xFFFFEAD0),
        cursorColor = Color(0xFFD6B15E),
        focusedBorderColor = Color(0xFFD6B15E),
        unfocusedBorderColor = Color(0xB3D6B15E),
        focusedLabelColor = Color(0xFFFFE0A0),
        unfocusedLabelColor = Color(0xFFBEB29A),
        focusedPlaceholderColor = Color(0xCCBEB29A),
        unfocusedPlaceholderColor = Color(0x99BEB29A),
        focusedContainerColor = Color(0xFF17140F),
        unfocusedContainerColor = Color(0xFF17140F),
    )
