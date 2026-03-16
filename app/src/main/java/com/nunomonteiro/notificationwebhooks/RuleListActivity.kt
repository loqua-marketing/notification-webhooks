package com.nunomonteiro.notificationwebhooks

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
import com.google.gson.Gson
import com.nunomonteiro.notificationwebhooks.data.model.Rule
import com.nunomonteiro.notificationwebhooks.ui.viewmodel.RuleViewModel

class RuleListActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RuleViewModel
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNewRule: Button
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_list)
        
        initViews()
        setupViewModel()
        setupListeners()
        observeData()
    }
    
    private fun initViews() {
        container = findViewById(R.id.container)
        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearch)
        tabLayout = findViewById(R.id.tabLayout)
        btnNewRule = findViewById(R.id.btnNewRule)
        progressBar = findViewById(R.id.progressBar)
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[RuleViewModel::class.java]
    }
    
    private fun setupListeners() {
        btnNewRule.setOnClickListener {
            startActivity(Intent(this, RuleCreateActivity::class.java))
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
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.rules.observe(this) { rules ->
            atualizarLista(rules)
        }
        
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun atualizarLista(rules: List<Rule>) {
        container.removeAllViews()
        
        if (rules.isEmpty()) {
            tvEmpty.text = "Nenhuma regra encontrada.\nClique em NOVA REGRA para criar uma."
            tvEmpty.visibility = TextView.VISIBLE
            return
        }
        
        tvEmpty.visibility = TextView.GONE
        
        rules.forEach { rule ->
            try {
                val itemView = LayoutInflater.from(this).inflate(R.layout.item_rule, container, false)
                
                val tvName = itemView.findViewById<TextView>(R.id.tvRuleName)
                val tvStatus = itemView.findViewById<TextView>(R.id.tvRuleStatus)
                val ivMenu = itemView.findViewById<ImageView>(R.id.ivMenu)
                val tvApp = itemView.findViewById<TextView>(R.id.tvRuleApp)
                val tvConditions = itemView.findViewById<TextView>(R.id.tvRuleConditions)
                val tvAction = itemView.findViewById<TextView>(R.id.tvRuleAction)
                val tvStats = itemView.findViewById<TextView>(R.id.tvRuleStats)
                
                tvName.text = rule.name
                
                tvStatus.text = if (rule.isActive) "✅ Ativa" else "⚪ Inativa"
                tvStatus.setBackgroundColor(if (rule.isActive) 
                    android.graphics.Color.parseColor("#4CAF50") else 
                    android.graphics.Color.parseColor("#9E9E9E"))
                
                tvApp.text = "📱 ${getAppName(rule.appPackage)}"
                tvConditions.text = viewModel.getConditionsDescription(rule)
                tvAction.text = viewModel.getActionDescription(rule)
                tvStats.text = viewModel.getStatsDescription(rule)
                
                ivMenu.setOnClickListener {
                    mostrarMenuOpcoes(rule)
                }
                
                itemView.setOnClickListener {
                    editarRegra(rule)
                }
                
                container.addView(itemView)
                
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao mostrar regra: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
    
    private fun getAppName(packageName: String?): String {
        if (packageName == null) return "Qualquer app"
        
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            when (packageName) {
                "com.google.android.gm" -> "Gmail"
                "com.whatsapp" -> "WhatsApp"
                "com.facebook.katana" -> "Facebook"
                "com.instagram.android" -> "Instagram"
                "com.linkedin.android" -> "LinkedIn"
                else -> packageName
            }
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
            .setTitle(rule.name)
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> viewModel.toggleRuleActive(rule)
                    1 -> editarRegra(rule)
                    2 -> confirmarEliminar(rule)
                    3 -> duplicarRegra(rule)
                }
            }
            .show()
    }
    
    private fun editarRegra(rule: Rule) {
        val intent = Intent(this, RuleCreateActivity::class.java).apply {
            putExtra("rule_id", rule.id)
            putExtra("rule_json", Gson().toJson(rule))
            putExtra("is_edit_mode", true)
        }
        startActivity(intent)
    }
    
    private fun duplicarRegra(rule: Rule) {
        val intent = Intent(this, RuleCreateActivity::class.java).apply {
            putExtra("rule_json", Gson().toJson(rule))
            putExtra("is_duplicate_mode", true)
        }
        startActivity(intent)
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
