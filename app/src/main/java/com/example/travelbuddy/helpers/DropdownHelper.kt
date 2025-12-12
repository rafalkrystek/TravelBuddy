package com.example.travelbuddy.helpers

import com.google.android.material.textfield.MaterialAutoCompleteTextView

object DropdownHelper {
    fun setupDropdownListeners(
        editText: MaterialAutoCompleteTextView,
        checkEnabled: Boolean = false,
        onItemSelected: () -> Unit = {}
    ) {
        editText.setOnClickListener { if (!checkEnabled || editText.isEnabled) editText.showDropDown() }
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && (!checkEnabled || editText.isEnabled)) editText.showDropDown()
        }
        editText.setOnItemClickListener { _, _, _, _ -> onItemSelected() }
    }
}

