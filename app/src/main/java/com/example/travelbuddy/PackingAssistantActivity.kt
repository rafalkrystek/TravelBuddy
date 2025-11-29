package com.example.travelbuddy

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class PackingAssistantActivity : BaseActivity() {
    
    private lateinit var daysCountEditText: TextInputEditText
    private lateinit var destinationSpinner: AutoCompleteTextView
    private lateinit var seasonSpinner: AutoCompleteTextView
    private lateinit var departureDateEditText: TextInputEditText
    private lateinit var activitiesSpinner: AutoCompleteTextView
    private lateinit var genderRadioGroup: RadioGroup
    private lateinit var familyTripCheckBox: CheckBox
    private lateinit var generateListButton: MaterialButton
    private lateinit var addCustomItemButton: MaterialButton
    private lateinit var templateNameEditText: TextInputEditText
    private lateinit var saveTemplateButton: MaterialButton
    private lateinit var loadTemplateButton: MaterialButton
    private lateinit var shareListButton: MaterialButton
    private lateinit var aiSuggestionsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_packing_assistant)

        initializeViews()
        setupSpinners()
        setupDatePicker()
        setupClickListeners()
    }

    private fun initializeViews() {
        daysCountEditText = findViewById(R.id.daysCountEditText)
        destinationSpinner = findViewById(R.id.destinationSpinner)
        seasonSpinner = findViewById(R.id.seasonSpinner)
        departureDateEditText = findViewById(R.id.departureDateEditText)
        activitiesSpinner = findViewById(R.id.activitiesSpinner)
        genderRadioGroup = findViewById(R.id.genderRadioGroup)
        familyTripCheckBox = findViewById(R.id.familyTripCheckBox)
        generateListButton = findViewById(R.id.generateListButton)
        addCustomItemButton = findViewById(R.id.addCustomItemButton)
        templateNameEditText = findViewById(R.id.templateNameEditText)
        saveTemplateButton = findViewById(R.id.saveTemplateButton)
        loadTemplateButton = findViewById(R.id.loadTemplateButton)
        shareListButton = findViewById(R.id.shareListButton)
        aiSuggestionsTextView = findViewById(R.id.aiSuggestionsTextView)

        genderRadioGroup.check(R.id.maleRadioButton)
    }

    private fun setupSpinners() {
        val destinations = arrayOf(
            "Polska", "Niemcy", "Francja", "Włochy", "Hiszpania", "Grecja", "Turcja", "Egipt", "Tunezja",
            "Maroko", "USA", "Kanada", "Meksyk", "Brazylia", "Argentyna", "Chile", "Peru", "Kolumbia",
            "Japonia", "Chiny", "Korea Południowa", "Tajlandia", "Wietnam", "Indonezja", "Malezja",
            "Singapur", "Australia", "Nowa Zelandia", "Fidżi", "Tahiti", "Hawaii", "Islandia",
            "Norwegia", "Szwecja", "Finlandia", "Dania", "Holandia", "Belgia", "Szwajcaria",
            "Austria", "Czechy", "Słowacja", "Węgry", "Rumunia", "Bułgaria", "Serbia", "Chorwacja",
            "Słowenia", "Czarnogóra", "Albania", "Macedonia", "Kosowo", "Bośnia i Hercegowina"
        )
        val destinationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, destinations)
        destinationSpinner.setAdapter(destinationAdapter)

        val seasonAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Constants.SEASONS)
        seasonSpinner.setAdapter(seasonAdapter)

        val activitiesAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, Constants.ACTIVITIES)
        activitiesSpinner.setAdapter(activitiesAdapter)
    }

    private fun setupDatePicker() {
        departureDateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val date = Calendar.getInstance()
                    date.set(year, month, dayOfMonth)
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    departureDateEditText.setText(dateFormat.format(date.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        generateListButton.setOnClickListener {
            if (validateInputs()) {
                generatePackingList()
            }
        }

        addCustomItemButton.setOnClickListener {
            showAddCustomItemDialog()
        }

        saveTemplateButton.setOnClickListener {
            saveTemplate()
        }

        loadTemplateButton.setOnClickListener {
            loadTemplate()
        }

        shareListButton.setOnClickListener {
            shareList()
        }

        familyTripCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "Lista będzie dostosowana dla dzieci", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(): Boolean {
        if (daysCountEditText.text.isNullOrEmpty()) {
            Toast.makeText(this, "Wprowadź liczbę dni podróży", Toast.LENGTH_SHORT).show()
            return false
        }
        if (destinationSpinner.text.isNullOrEmpty()) {
            Toast.makeText(this, "Wybierz destynację", Toast.LENGTH_SHORT).show()
            return false
        }
        if (seasonSpinner.text.isNullOrEmpty()) {
            Toast.makeText(this, "Wybierz porę roku", Toast.LENGTH_SHORT).show()
            return false
        }
        if (departureDateEditText.text.isNullOrEmpty()) {
            Toast.makeText(this, "Wybierz datę wyjazdu", Toast.LENGTH_SHORT).show()
            return false
        }
        if (activitiesSpinner.text.isNullOrEmpty()) {
            Toast.makeText(this, "Wybierz planowane aktywności", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun generatePackingList() {
        Toast.makeText(this, "Generowanie listy rzeczy do spakowania...", Toast.LENGTH_SHORT).show()
        val suggestions = "Na podstawie Twoich preferencji:\n" +
                "• Destynacja: ${destinationSpinner.text}\n" +
                "• Pora roku: ${seasonSpinner.text}\n" +
                "• Aktywności: ${activitiesSpinner.text}\n" +
                "• Liczba dni: ${daysCountEditText.text}\n\n" +
                "Sugerowane przedmioty:\n" +
                "• Odpowiednia odzież na ${seasonSpinner.text}\n" +
                "• Sprzęt do ${activitiesSpinner.text}\n" +
                "• Dokumenty i pieniądze\n" +
                "• Apteczka pierwszej pomocy\n" +
                "• Ładowarki i powerbank"
        
        aiSuggestionsTextView.text = suggestions
    }

    private fun showAddCustomItemDialog() {
        Toast.makeText(this, "Funkcja dodawania własnej rzeczy - do implementacji", Toast.LENGTH_SHORT).show()
    }

    private fun saveTemplate() {
        val templateName = templateNameEditText.text.toString()
        if (templateName.isNotEmpty()) {
            Toast.makeText(this, "Szablon '$templateName' został zapisany", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Wprowadź nazwę szablonu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTemplate() {
        Toast.makeText(this, "Funkcja wczytywania szablonu - do implementacji", Toast.LENGTH_SHORT).show()
    }

    private fun shareList() {
        Toast.makeText(this, "Funkcja udostępniania listy - do implementacji", Toast.LENGTH_SHORT).show()
    }
} 