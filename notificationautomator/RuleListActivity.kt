package com.seuapp.notificationautomator

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.seuapp.notificationautomator.data.model.Rule
import com.seuapp.notificationautomator.ui.viewmodel.RuleViewModel

class RuleListActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RuleViewModel
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNewRule: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_list)
        
        initViews()
        setupViewModel()
        setupListeners()
        observeData()
        
        // Para teste: criar regras de exemplo (remover depois)
        // viewModel.createSampleRules()
    }
    
    private fun initViews() {
        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearch)
        tabLayout = findViewById(R.id.tabLayout)
        btnNewRule = findViewById(R.id.btnNewRule)
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[RuleViewModel::class.java]
    }
    
    private fun setupListeners() {
        btnNewRule.setOnClickListener {
            Toast.makeText(this, "Criar nova regra (em breve)", Toast.LENGTH_SHORT).show()
        }
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewModel.setCurrentTab(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s.toString())
            }
        })
    }
    
    private fun observeData() {
        viewModel.rules.observe(this) { rules ->
            atualizarLista(rules)
        }
        
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun atualizarLista(rules: List<Rule>) {
        container.removeAllViews()
        
        if (rules.isEmpty()) {
            tvEmpty.visibility = TextView.VISIBLE
            return
        }
        
        tvEmpty.visibility = TextView.GONE
        
        rules.forEach { rule ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_rule, container, false)
            
            // Referências
            val tvName = itemView.findViewById<TextView>(R.id.tvRuleName)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvRuleStatus)
            val ivMenu = itemView.findViewById<ImageView>(R.id.ivMenu)
            val tvApp = itemView.findViewById<TextView>(R.id.tvRuleApp)
            val tvConditions = itemView.findViewById<TextView>(R.id.tvRuleConditions)
            val tvAction = itemView.findViewById<TextView>(R.id.tvRuleAction)
            val tvStats = itemView.findViewById<TextView>(R.id.tvRuleStats)
            
            // Nome
            tvName.text = rule.name
            
            // Status
            tvStatus.text = if (rule.isActive) "✅ Ativa" else "⚪ Inativa"
            tvStatus.setBackgroundColor(if (rule.isActive) 
                android.graphics.Color.parseColor("#4CAF50") else 
                android.graphics.Color.parseColor("#9E9E9E"))
            
            // App
            tvApp.text = "📱 ${rule.appPackage ?: "Qualquer app"}"
            
            // Condições
            tvConditions.text = viewModel.getConditionsDescription(rule)
            
            // Ação
            tvAction.text = viewModel.getActionDescription(rule)
            
            // Estatísticas
            tvStats.text = viewModel.getStatsDescription(rule)
            
            // Menu de opções
            ivMenu.setOnClickListener {
                mostrarMenuOpcoes(rule)
            }
            
            // Clique no item para editar (futuramente)
            itemView.setOnClickListener {
                Toast.makeText(this, "Editar regra (em breve)", Toast.LENGTH_SHORT).show()
            }
            
            container.addView(itemView)
        }
    }
    
    private fun mostrarMenuOpcoes(rule: Rule) {
        val opcoes = arrayOf(
            if (rule.isActive) "❌ Desativar" else "✅ Ativar",
            "✏️ Editar",
            "🗑️ Eliminar",
            "📋 Duplicar",
            "❌ Cancelar"
        )
        
        AlertDialog.Builder(this)
            .setTitle("Opções")
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> { // Ativar/Desativar
                        viewModel.toggleRuleActive(rule)
                        Toast.makeText(this, 
                            if (rule.isActive) "Regra desativada" else "Regra ativada", 
                            Toast.LENGTH_SHORT).show()
                    }
                    1 -> { // Editar
                        Toast.makeText(this, "Editar regra (em breve)", Toast.LENGTH_SHORT).show()
                    }
                    2 -> { // Eliminar
                        confirmarEliminar(rule)
                    }
                    3 -> { // Duplicar
                        Toast.makeText(this, "Duplicar regra (em breve)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }
    
    private fun confirmarEliminar(rule: Rule) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar regra")
            .setMessage("Tem a certeza que pretende eliminar a regra '${rule.name}'?")
            .setPositiveButton("Sim") { _, _ ->
                viewModel.deleteRule(rule)
                Toast.makeText(this, "Regra eliminada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Não", null)
            .show()
    }
}
