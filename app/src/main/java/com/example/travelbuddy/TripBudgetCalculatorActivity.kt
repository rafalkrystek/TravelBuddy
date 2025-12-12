package com.example.travelbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.example.travelbuddy.helpers.getTripDocument
import com.example.travelbuddy.helpers.setupBackButton
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TripBudgetCalculatorActivity : BaseActivity() {
    private lateinit var tripId: String
    private var initialBudget: Int = 0
    private lateinit var budgetTextView: TextView
    private lateinit var remainingBudgetTextView: TextView
    private lateinit var totalSpentTextView: TextView
    private lateinit var expensesRecyclerView: RecyclerView
    private lateinit var expensesAdapter: ExpenseAdapter
    private val expenses = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_budget_calculator)
        tripId = intent.getStringExtra("trip_id") ?: ""
        initialBudget = intent.getIntExtra("trip_budget", 0)

        if (tripId.isEmpty()) {
            Toast.makeText(this, "Błąd: Brak ID podróży", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.destinationTextView).text = intent.getStringExtra("trip_destination") ?: ""
        budgetTextView = findViewById(R.id.budgetTextView)
        remainingBudgetTextView = findViewById(R.id.remainingBudgetTextView)
        totalSpentTextView = findViewById(R.id.totalSpentTextView)
        expensesRecyclerView = findViewById(R.id.expensesRecyclerView)

        budgetTextView.text = "Budżet: ${initialBudget} zł"
        expensesAdapter = ExpenseAdapter(expenses) { showDeleteExpenseDialog(it) }
        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        expensesRecyclerView.adapter = expensesAdapter

        setupBackButton()
        findViewById<Button>(R.id.addHotelButton).setOnClickListener { showAddExpenseDialog("Hotel/Apartament") }
        findViewById<Button>(R.id.addTransportButton).setOnClickListener { showAddExpenseDialog("Dojazd") }
        findViewById<Button>(R.id.addActivityButton).setOnClickListener { showAddExpenseDialog("Aktywność") }
        findViewById<Button>(R.id.addFoodButton).setOnClickListener { showAddExpenseDialog("Jedzenie") }
        findViewById<Button>(R.id.addShoppingButton).setOnClickListener { showAddExpenseDialog("Zakupy przed wyjazdem") }
        findViewById<Button>(R.id.addSouvenirsButton).setOnClickListener { showAddExpenseDialog("Pamiątki") }
        findViewById<Button>(R.id.addOtherButton).setOnClickListener { showAddExpenseDialog("Inne") }
        loadExpenses()
    }

    override fun onResume() {
        super.onResume()
        // Załaduj aktualny budżet z Firebase
        FirebaseFirestore.getInstance().getTripDocument(tripId).get().addOnSuccessListener {
            initialBudget = (it.getLong("budget") ?: 0).toInt()
            budgetTextView.text = "Budżet: ${initialBudget} zł"
            // Załaduj wydatki ponownie, aby mieć aktualne dane
            loadExpenses()
        }.addOnFailureListener {
            // Jeśli nie uda się załadować budżetu, załaduj tylko wydatki
            loadExpenses()
        }
    }

    private fun showAddExpenseDialog(category: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null)
        val nameEdit = view.findViewById<TextInputEditText>(R.id.expenseNameEditText)
        val amountEdit = view.findViewById<TextInputEditText>(R.id.expenseAmountEditText)
        val nameLayout = view.findViewById<TextInputLayout>(R.id.expenseNameLayout)
        val amountLayout = view.findViewById<TextInputLayout>(R.id.expenseAmountLayout)

        if (category == "Aktywność") nameLayout?.hint = "Nazwa aktywności"

        val dialog = AlertDialog.Builder(this).setTitle("Dodaj wydatek: $category").setView(view)
            .setPositiveButton("Dodaj", null).setNegativeButton("Anuluj", null).create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { dialog.dismiss() }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameEdit.text?.toString()?.trim() ?: ""
                val amount = amountEdit.text?.toString()?.trim()?.toIntOrNull() ?: 0
                nameLayout?.error = null
                amountLayout?.error = null

                when {
                    name.isEmpty() -> nameLayout?.error = "Wprowadź nazwę"
                    amount <= 0 -> amountLayout?.error = "Wprowadź kwotę"
                    FirebaseAuth.getInstance().currentUser == null -> {
                        Toast.makeText(this, "Błąd: Nie jesteś zalogowany", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    else -> {
                        dialog.dismiss()
                        addExpense(Expense("", category, name, amount))
                    }
                }
            }
        }
        dialog.show()
    }

    private fun addExpense(expense: Expense) {
        if (FirebaseAuth.getInstance().currentUser == null || tripId.isEmpty()) return
        FirebaseFirestore.getInstance().getTripDocument(tripId).collection("expenses").add(hashMapOf(
            "category" to expense.category,
            "name" to expense.name,
            "amount" to expense.amount,
            "createdAt" to com.google.firebase.Timestamp.now()
        )).addOnSuccessListener {
            expenses.add(expense.copy(id = it.id))
            expensesAdapter.notifyItemInserted(expenses.size - 1)
            updateBudgetDisplay()
            updateRemainingBudgetInFirestore()
            Toast.makeText(this, "Wydatek dodany", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Błąd: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeleteExpenseDialog(expense: Expense) {
        AlertDialog.Builder(this).setTitle("Usuń wydatek")
            .setMessage("Usunąć ${expense.name}?")
            .setPositiveButton("Usuń") { _, _ -> deleteExpense(expense) }
            .setNegativeButton("Anuluj", null).show()
    }

    private fun deleteExpense(expense: Expense) {
        if (expense.id.isEmpty()) {
            val idx = expenses.indexOf(expense)
            expenses.removeAt(idx)
            expensesAdapter.notifyItemRemoved(idx)
            updateBudgetDisplay()
            updateRemainingBudgetInFirestore()
            return
        }
        FirebaseFirestore.getInstance().getTripDocument(tripId).collection("expenses").document(expense.id).delete()
            .addOnSuccessListener {
                val idx = expenses.indexOf(expense)
                expenses.removeAt(idx)
                expensesAdapter.notifyItemRemoved(idx)
                updateBudgetDisplay()
                updateRemainingBudgetInFirestore()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd usuwania", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBudgetDisplay() {
        val totalSpent = expenses.sumOf { it.amount }
        val remaining = initialBudget - totalSpent
        totalSpentTextView.text = "Wydano: $totalSpent zł"
        remainingBudgetTextView.text = "Pozostało: $remaining zł"
        remainingBudgetTextView.setTextColor(ContextCompat.getColor(this,
            if (remaining < 0) android.R.color.holo_red_dark else android.R.color.holo_green_dark))
    }
    
    private fun updateRemainingBudgetInFirestore() {
        if (tripId.isEmpty()) return
        val totalSpent = expenses.sumOf { it.amount }
        val remaining = initialBudget - totalSpent
        
        FirebaseFirestore.getInstance().getTripDocument(tripId).update(
            hashMapOf(
                "remainingBudget" to remaining,
                "totalSpent" to totalSpent,
                "updatedAt" to com.google.firebase.Timestamp.now()
            ) as Map<String, Any>
        ).addOnFailureListener { e ->
            android.util.Log.e("TripBudgetCalculator", "Error updating remaining budget", e)
        }
    }

    private fun loadExpenses() {
        if (tripId.isEmpty()) return
        
        // Spróbuj załadować z sortowaniem (najnowsze na górze)
        FirebaseFirestore.getInstance().getTripDocument(tripId).collection("expenses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                expenses.clear()
                docs.forEach { doc ->
                    expenses.add(Expense(
                        doc.id,
                        doc.getString("category") ?: "",
                        doc.getString("name") ?: "",
                        (doc.getLong("amount") ?: 0).toInt()
                    ))
                }
                expensesAdapter.notifyDataSetChanged()
                updateBudgetDisplay()
                // Zaktualizuj remainingBudget po załadowaniu wydatków
                updateRemainingBudgetInFirestore()
            }
            .addOnFailureListener { e ->
                // Jeśli orderBy nie działa (brak indeksu), załaduj bez sortowania
                android.util.Log.w("TripBudgetCalculator", "Error with orderBy, trying without sort", e)
                FirebaseFirestore.getInstance().getTripDocument(tripId).collection("expenses")
                    .get()
                    .addOnSuccessListener { docs ->
                        expenses.clear()
                        docs.forEach { doc ->
                            expenses.add(Expense(
                                doc.id,
                                doc.getString("category") ?: "",
                                doc.getString("name") ?: "",
                                (doc.getLong("amount") ?: 0).toInt()
                            ))
                        }
                        // Sortuj lokalnie (najnowsze na górze)
                        expenses.sortByDescending { 
                            // Jeśli nie ma createdAt, użyj ID (nowsze ID są większe)
                            it.id 
                        }
                        expensesAdapter.notifyDataSetChanged()
                        updateBudgetDisplay()
                        // Zaktualizuj remainingBudget po załadowaniu wydatków
                        updateRemainingBudgetInFirestore()
                    }
                    .addOnFailureListener { e2 ->
                        android.util.Log.e("TripBudgetCalculator", "Error loading expenses", e2)
                        Toast.makeText(this, "Błąd ładowania wydatków: ${e2.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    data class Expense(val id: String, val category: String, val name: String, val amount: Int)

    class ExpenseAdapter(private val expenses: List<Expense>, private val onClick: (Expense) -> Unit) :
        RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryTextView: TextView = view.findViewById(R.id.expenseCategoryTextView)
            val nameTextView: TextView = view.findViewById(R.id.expenseNameTextView)
            val amountTextView: TextView = view.findViewById(R.id.expenseAmountTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val expense = expenses[position]
            holder.categoryTextView.text = expense.category
            holder.nameTextView.text = expense.name
            holder.amountTextView.text = "${expense.amount} zł"
            holder.itemView.setOnClickListener { onClick(expense) }
        }

        override fun getItemCount() = expenses.size
    }
}
