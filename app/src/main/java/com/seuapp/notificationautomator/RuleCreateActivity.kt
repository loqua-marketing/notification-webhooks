package com.seuapp.notificationautomator

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.seuapp.notificationautomator.data.model.ActionType
import com.seuapp.notificationautomator.data.model.Rule
import com.seuapp.notificationautomator.ui.viewmodel.RuleViewModel
import java.util.*
import com.seuapp.notificationautomator.data.model.SavedWebhook
import com.seuapp.notificationautomator.data.model.SecurityConfig
import com.seuapp.notificationautomator.data.model.AuthType
import com.seuapp.notificationautomator.data.model.AdvancedWebhookConfig
import com.seuapp.notificationautomator.data.repository.WebhookRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit


class RuleCreateActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RuleViewModel
    private val TAG = "RuleCreateActivity"
    private val gson = Gson()
    
    override fun onBackPressed() {
        if (currentStep > 1) {
            setupStep(currentStep - 1)
        } else {
            super.onBackPressed()
        }
    }

    // WebhookRepository para endereços guardados
    private lateinit var webhookRepository: WebhookRepository

    // Lista de webhooks guardados
    private var savedWebhooks: List<SavedWebhook> = emptyList()

    // Novas configurações
    private var securityConfig: SecurityConfig = SecurityConfig()
    private var advancedConfig: AdvancedWebhookConfig = AdvancedWebhookConfig()
    private var selectedWebhookName: String? = null

    // Views do step 3
    private lateinit var actWebhookUrl: EditText
    private lateinit var etWebhookName: EditText
    private lateinit var btnSaveWebhook: Button
    private lateinit var btnTestWebhook: Button
    private lateinit var btnDeleteWebhook: Button
    private lateinit var layoutSavedWebhooks: LinearLayout
    private lateinit var cbSignHmac: CheckBox
    private lateinit var layoutHmacSecret: LinearLayout
    private lateinit var etHmacSecret: EditText
    private lateinit var rgAuthType: RadioGroup
    private lateinit var layoutAuthBasic: LinearLayout
    private lateinit var etAuthUsername: EditText
    private lateinit var etAuthPassword: EditText
    private lateinit var layoutAuthBearer: LinearLayout
    private lateinit var etAuthToken: EditText
    private lateinit var layoutAuthApiKey: LinearLayout
    private lateinit var etAuthKeyName: EditText
    private lateinit var etAuthKeyValue: EditText
    private lateinit var etHeadersAdvanced: EditText
    private lateinit var etTimeout: EditText
    private lateinit var etRetries: EditText
    private lateinit var rgPayloadType: RadioGroup
    private lateinit var etCustomPayload: EditText
    private lateinit var cbWaitForResponse: CheckBox

    // Views do stepper
    private lateinit var tvStep1: TextView
    private lateinit var tvStep2: TextView
    private lateinit var tvStep3: TextView
    private lateinit var tvStep4: TextView
    
    // Botões de navegação
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    
    // Container para os steps
    private lateinit var container: LinearLayout
    
    // Step atual
    private var currentStep = 1
    
    // Dados da regra (vão sendo preenchidos)
    private var selectedAppPackage: String? = null
    private var selectedAppName: String? = null
    private var titleContains: String? = null
    private var textContains: String? = null
    private var hourFrom: String? = null
    private var hourTo: String? = null
    private var selectedDays: MutableList<String> = mutableListOf()
    private var isSilent: Boolean? = null
    private var hasImage: Boolean = false
    private var actionType: ActionType = ActionType.WEBHOOK_AUTO
    private var webhookUrl: String? = null
    private var webhookHeaders: Map<String, String>? = null
    private var ruleName: String = ""
    
    // Para edição
    private var editingRuleId: Long? = null
    private var isEditMode: Boolean = false
    private var isDuplicateMode: Boolean = false
    
    // Para aplicar regra à notificação atual
    private var applyRuleNow: Boolean = false
    private var notificationId: Long = 0
    
    // Para pré-preenchimento (quando vem de notificação)
    private var notificationPackage: String? = null
    private var notificationTitle: String? = null
    private var notificationText: String? = null
    private var notificationTimestamp: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_create)
        
        isEditMode = intent.getBooleanExtra("is_edit_mode", false)
        isDuplicateMode = intent.getBooleanExtra("is_duplicate_mode", false)
        editingRuleId = intent.getLongExtra("rule_id", 0).takeIf { it != 0L }
        
        applyRuleNow = intent.getBooleanExtra("apply_rule_now", false)
        notificationId = intent.getLongExtra("notification_id", 0)
        
        val ruleJson = intent.getStringExtra("rule_json")
        if (ruleJson != null) {
            carregarRegraExistente(ruleJson)
        }
        
        notificationPackage = intent.getStringExtra("notification_package")
        notificationTitle = intent.getStringExtra("notification_title")
        notificationText = intent.getStringExtra("notification_text")
        notificationTimestamp = intent.getLongExtra("notification_timestamp", 0)
        
        Log.d(TAG, "Modo: ${if (isEditMode) "Edição" else if (isDuplicateMode) "Duplicação" else "Criação"}")
        Log.d(TAG, "Notificação package: $notificationPackage")
        Log.d(TAG, "Aplicar regra à notificação $notificationId: $applyRuleNow")
        
        initViews()
        webhookRepository = WebhookRepository.getInstance(this)
        webhookRepository.initDefaultWebhookIfNeeded()
        setupViewModel()
        setupStep(1)
    }
    
    private fun carregarRegraExistente(ruleJson: String) {
        try {
            val rule = gson.fromJson(ruleJson, Rule::class.java)
            
            selectedAppPackage = rule.appPackage
            titleContains = rule.titleContains
            textContains = rule.textContains
            hourFrom = rule.hourFrom
            hourTo = rule.hourTo
            rule.daysOfWeek?.let {
                selectedDays = it.split(",").toMutableList()
            }
            isSilent = rule.isSilent
            hasImage = rule.hasImage ?: false
            actionType = rule.actionType
            webhookUrl = rule.webhookUrl
            rule.webhookHeaders?.let {
                try {
                    webhookHeaders = gson.fromJson(it, object : TypeToken<Map<String, String>>() {}.type)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ruleName = rule.name
            
            if (!rule.securityConfig.isNullOrEmpty()) {
                securityConfig = gson.fromJson(rule.securityConfig, SecurityConfig::class.java)
            }
            if (!rule.advancedConfig.isNullOrEmpty()) {
                advancedConfig = gson.fromJson(rule.advancedConfig, AdvancedWebhookConfig::class.java)
            }
            selectedWebhookName = rule.selectedWebhookName
            
            Log.d(TAG, "Regra carregada: $ruleName - Package: $selectedAppPackage")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar regra", e)
        }
    }
    
    private fun initViews() {
        tvStep1 = findViewById(R.id.tvStep1)
        tvStep2 = findViewById(R.id.tvStep2)
        tvStep3 = findViewById(R.id.tvStep3)
        tvStep4 = findViewById(R.id.tvStep4)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        container = findViewById(R.id.container)
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[RuleViewModel::class.java]
    }
    
    private fun setupStep(step: Int) {
        currentStep = step
        atualizarStepper()
        container.removeAllViews()
        
        try {
            when (step) {
                1 -> showStepApp()
                2 -> showStepConditions()
                3 -> {
                    showStepAction()
                    if (selectedWebhookName != null) {
                        val webhook = savedWebhooks.find { it.name == selectedWebhookName }
                        webhook?.let { carregarDadosWebhook(it) }
                    }
                }
                4 -> showStepReview()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao mostrar step $step", e)
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        
        btnPrevious.isEnabled = step > 1
        btnPrevious.setOnClickListener {
            if (step > 1) {
                setupStep(step - 1)
            }
        }
        
        btnNext.visibility = if (step < 4) View.VISIBLE else View.GONE
        btnSave.visibility = if (step == 4) View.VISIBLE else View.GONE
    }
    
    private fun atualizarStepper() {
        tvStep1.setBackgroundResource(R.drawable.step_background_inactive)
        tvStep2.setBackgroundResource(R.drawable.step_background_inactive)
        tvStep3.setBackgroundResource(R.drawable.step_background_inactive)
        tvStep4.setBackgroundResource(R.drawable.step_background_inactive)
        
        when (currentStep) {
            1 -> tvStep1.setBackgroundResource(R.drawable.step_background_active)
            2 -> tvStep2.setBackgroundResource(R.drawable.step_background_active)
            3 -> tvStep3.setBackgroundResource(R.drawable.step_background_active)
            4 -> tvStep4.setBackgroundResource(R.drawable.step_background_active)
        }
    }
    
    private fun showStepApp() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.step_app, container, true)
        
        val rgAppChoice = view.findViewById<RadioGroup>(R.id.rgAppChoice)
        val spinnerApps = view.findViewById<Spinner>(R.id.spinnerApps)
        val tvSelectedApp = view.findViewById<TextView>(R.id.tvSelectedApp)
        val tvPackageHint = view.findViewById<TextView>(R.id.tvPackageHint)
        
        val apps = getInstalledApps()
        val appNames = apps.map { it.first }
        val appPackages = apps.map { it.second }
        
        val appDisplayList = apps.map { (name, pkg) ->
            "$name ($pkg)"
        }
        
        if (appDisplayList.isEmpty()) {
            Toast.makeText(this, "Nenhuma app encontrada", Toast.LENGTH_SHORT).show()
            return
        }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, appDisplayList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerApps.adapter = adapter
        
        var initialSelection = 0
        if (notificationPackage != null) {
            val pkg = notificationPackage!!
            val index = appPackages.indexOfFirst { it == pkg }
            if (index >= 0) {
                initialSelection = index
                selectedAppPackage = pkg
                selectedAppName = appNames[index]
            }
        } else if (selectedAppPackage != null) {
            val index = appPackages.indexOfFirst { it == selectedAppPackage }
            if (index >= 0) {
                initialSelection = index
            }
        }
        
        spinnerApps.setSelection(initialSelection)
        
        val selectedIndex = spinnerApps.selectedItemPosition
        selectedAppPackage = appPackages[selectedIndex]
        selectedAppName = appNames[selectedIndex]
        tvSelectedApp.text = "App selecionada: ${appNames[selectedIndex]}"
        tvPackageHint.text = "Package: ${appPackages[selectedIndex]}"
        
        spinnerApps.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAppPackage = appPackages[position]
                selectedAppName = appNames[position]
                tvSelectedApp.text = "App selecionada: ${appNames[position]}"
                tvPackageHint.text = "Package: ${appPackages[position]}"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        rgAppChoice.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbSpecificApp -> {
                    spinnerApps.isEnabled = true
                    tvPackageHint.visibility = View.VISIBLE
                    if (appNames.isNotEmpty()) {
                        val position = spinnerApps.selectedItemPosition
                        selectedAppPackage = appPackages[position]
                        selectedAppName = appNames[position]
                        tvSelectedApp.text = "App selecionada: ${appNames[position]}"
                        tvPackageHint.text = "Package: ${appPackages[position]}"
                    }
                }
                R.id.rbAnyApp -> {
                    spinnerApps.isEnabled = false
                    tvPackageHint.visibility = View.GONE
                    selectedAppPackage = null
                    selectedAppName = null
                    tvSelectedApp.text = "Qualquer app"
                }
            }
        }
        
        btnPrevious.setOnClickListener { }
        btnNext.setOnClickListener {
            setupStep(2)
        }
    }

    private fun getInstalledApps(): List<Pair<String, String>> {
        return try {
            val pm = packageManager
            val apps = pm.getInstalledApplications(0)
            
            val mappedApps = apps.map { app ->
                val name = try {
                    pm.getApplicationLabel(app).toString()
                } catch (e: Exception) {
                    app.packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                }
                name to app.packageName
            }.sortedBy { it.first }
            
            mappedApps
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao listar apps", e)
            emptyList()
        }
    }
    
    private fun showStepConditions() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.step_conditions, container, true)
        
        val etTitleContains = view.findViewById<EditText>(R.id.etTitleContains)
        val etTextContains = view.findViewById<EditText>(R.id.etTextContains)
        val cbEnableTime = view.findViewById<CheckBox>(R.id.cbEnableTime)
        val layoutTime = view.findViewById<LinearLayout>(R.id.layoutTime)
        val etHourFrom = view.findViewById<EditText>(R.id.etHourFrom)
        val etHourTo = view.findViewById<EditText>(R.id.etHourTo)
        
        val cbEnableSpecificDays = view.findViewById<CheckBox>(R.id.cbEnableSpecificDays)
        val layoutDays = view.findViewById<LinearLayout>(R.id.layoutDays)
        
        val cbMonday = view.findViewById<CheckBox>(R.id.cbMonday)
        val cbTuesday = view.findViewById<CheckBox>(R.id.cbTuesday)
        val cbWednesday = view.findViewById<CheckBox>(R.id.cbWednesday)
        val cbThursday = view.findViewById<CheckBox>(R.id.cbThursday)
        val cbFriday = view.findViewById<CheckBox>(R.id.cbFriday)
        val cbSaturday = view.findViewById<CheckBox>(R.id.cbSaturday)
        val cbSunday = view.findViewById<CheckBox>(R.id.cbSunday)
        val cbAllDays = view.findViewById<CheckBox>(R.id.cbAllDays)
        
        val rgNotificationType = view.findViewById<RadioGroup>(R.id.rgNotificationType)
        val cbHasImage = view.findViewById<CheckBox>(R.id.cbHasImage)
        
        titleContains?.let { etTitleContains.setText(it) }
        textContains?.let { etTextContains.setText(it) }
        
        cbEnableTime.isChecked = false
        layoutTime.visibility = View.GONE
        
        if (hourFrom != null && hourTo != null) {
            cbEnableTime.isChecked = true
            layoutTime.visibility = View.VISIBLE
            etHourFrom.setText(hourFrom)
            etHourTo.setText(hourTo)
        }
        
        cbEnableSpecificDays.isChecked = false
        layoutDays.visibility = View.GONE
        
        if (selectedDays.isNotEmpty()) {
            cbEnableSpecificDays.isChecked = true
            layoutDays.visibility = View.VISIBLE
            
            cbMonday.isChecked = false
            cbTuesday.isChecked = false
            cbWednesday.isChecked = false
            cbThursday.isChecked = false
            cbFriday.isChecked = false
            cbSaturday.isChecked = false
            cbSunday.isChecked = false
            
            selectedDays.forEach { day ->
                when (day) {
                    "MONDAY" -> cbMonday.isChecked = true
                    "TUESDAY" -> cbTuesday.isChecked = true
                    "WEDNESDAY" -> cbWednesday.isChecked = true
                    "THURSDAY" -> cbThursday.isChecked = true
                    "FRIDAY" -> cbFriday.isChecked = true
                    "SATURDAY" -> cbSaturday.isChecked = true
                    "SUNDAY" -> cbSunday.isChecked = true
                }
            }
        }
        
        when (isSilent) {
            true -> rgNotificationType.check(R.id.rbSilent)
            false -> rgNotificationType.check(R.id.rbAlerting)
            else -> rgNotificationType.check(R.id.rbAnyType)
        }
        
        cbHasImage.isChecked = hasImage
        
        if (!isEditMode && !isDuplicateMode && notificationTimestamp > 0) {
            if (etTitleContains.text.isNullOrBlank() && !notificationTitle.isNullOrBlank()) {
                etTitleContains.setText(notificationTitle)
            }
            if (etTextContains.text.isNullOrBlank() && !notificationText.isNullOrBlank()) {
                etTextContains.setText(notificationText)
            }
        }
        
        cbEnableTime.setOnCheckedChangeListener { _, isChecked ->
            layoutTime.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                etHourFrom.text?.clear()
                etHourTo.text?.clear()
            }
        }
        
        cbEnableSpecificDays.setOnCheckedChangeListener { _, isChecked ->
            layoutDays.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked) {
                val calendar = Calendar.getInstance()
                cbMonday.isChecked = false
                cbTuesday.isChecked = false
                cbWednesday.isChecked = false
                cbThursday.isChecked = false
                cbFriday.isChecked = false
                cbSaturday.isChecked = false
                cbSunday.isChecked = false
                
                when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> cbMonday.isChecked = true
                    Calendar.TUESDAY -> cbTuesday.isChecked = true
                    Calendar.WEDNESDAY -> cbWednesday.isChecked = true
                    Calendar.THURSDAY -> cbThursday.isChecked = true
                    Calendar.FRIDAY -> cbFriday.isChecked = true
                    Calendar.SATURDAY -> cbSaturday.isChecked = true
                    Calendar.SUNDAY -> cbSunday.isChecked = true
                }
            }
        }
        
        cbAllDays.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cbMonday.isChecked = true
                cbTuesday.isChecked = true
                cbWednesday.isChecked = true
                cbThursday.isChecked = true
                cbFriday.isChecked = true
                cbSaturday.isChecked = true
                cbSunday.isChecked = true
            }
        }
        
        val dayCheckListener = CompoundButton.OnCheckedChangeListener { _, _ ->
            val allChecked = cbMonday.isChecked && cbTuesday.isChecked && 
                             cbWednesday.isChecked && cbThursday.isChecked && 
                             cbFriday.isChecked && cbSaturday.isChecked && cbSunday.isChecked
            cbAllDays.setOnCheckedChangeListener(null)
            cbAllDays.isChecked = allChecked
            cbAllDays.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    cbMonday.isChecked = true
                    cbTuesday.isChecked = true
                    cbWednesday.isChecked = true
                    cbThursday.isChecked = true
                    cbFriday.isChecked = true
                    cbSaturday.isChecked = true
                    cbSunday.isChecked = true
                }
            }
        }
        
        cbMonday.setOnCheckedChangeListener(dayCheckListener)
        cbTuesday.setOnCheckedChangeListener(dayCheckListener)
        cbWednesday.setOnCheckedChangeListener(dayCheckListener)
        cbThursday.setOnCheckedChangeListener(dayCheckListener)
        cbFriday.setOnCheckedChangeListener(dayCheckListener)
        cbSaturday.setOnCheckedChangeListener(dayCheckListener)
        cbSunday.setOnCheckedChangeListener(dayCheckListener)
        
        btnPrevious.setOnClickListener { setupStep(1) }
        btnNext.setOnClickListener {
            titleContains = etTitleContains.text.toString().takeIf { it.isNotBlank() }
            textContains = etTextContains.text.toString().takeIf { it.isNotBlank() }
            
            if (cbEnableTime.isChecked) {
                hourFrom = etHourFrom.text.toString().takeIf { it.isNotBlank() }
                hourTo = etHourTo.text.toString().takeIf { it.isNotBlank() }
            } else {
                hourFrom = null
                hourTo = null
            }
            
            selectedDays.clear()
            if (cbEnableSpecificDays.isChecked) {
                if (cbMonday.isChecked) selectedDays.add("MONDAY")
                if (cbTuesday.isChecked) selectedDays.add("TUESDAY")
                if (cbWednesday.isChecked) selectedDays.add("WEDNESDAY")
                if (cbThursday.isChecked) selectedDays.add("THURSDAY")
                if (cbFriday.isChecked) selectedDays.add("FRIDAY")
                if (cbSaturday.isChecked) selectedDays.add("SATURDAY")
                if (cbSunday.isChecked) selectedDays.add("SUNDAY")
            } else {
                selectedDays.addAll(listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"))
            }
            
            isSilent = when (rgNotificationType.checkedRadioButtonId) {
                R.id.rbSilent -> true
                R.id.rbAlerting -> false
                else -> null
            }
            
            hasImage = cbHasImage.isChecked
            
            setupStep(3)
        }
    }
    
    private fun showStepAction() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.step_action_new, container, true)
        
        try {
            actWebhookUrl = view.findViewById(R.id.actWebhookUrl)
            etWebhookName = view.findViewById(R.id.etWebhookName)
            btnSaveWebhook = view.findViewById(R.id.btnSaveWebhook)
            btnTestWebhook = view.findViewById(R.id.btnTestWebhook)
            btnDeleteWebhook = view.findViewById(R.id.btnDeleteWebhook)
            layoutSavedWebhooks = view.findViewById(R.id.layoutSavedWebhooks)
            cbSignHmac = view.findViewById(R.id.cbSignHmac)
            layoutHmacSecret = view.findViewById(R.id.layoutHmacSecret)
            etHmacSecret = view.findViewById(R.id.etHmacSecret)
            rgAuthType = view.findViewById(R.id.rgAuthType)
            layoutAuthBasic = view.findViewById(R.id.layoutAuthBasic)
            etAuthUsername = view.findViewById(R.id.etAuthUsername)
            etAuthPassword = view.findViewById(R.id.etAuthPassword)
            layoutAuthBearer = view.findViewById(R.id.layoutAuthBearer)
            etAuthToken = view.findViewById(R.id.etAuthToken)
            layoutAuthApiKey = view.findViewById(R.id.layoutAuthApiKey)
            etAuthKeyName = view.findViewById(R.id.etAuthKeyName)
            etAuthKeyValue = view.findViewById(R.id.etAuthKeyValue)
            etHeadersAdvanced = view.findViewById(R.id.etHeadersAdvanced)
            etTimeout = view.findViewById(R.id.etTimeout)
            etRetries = view.findViewById(R.id.etRetries)
            rgPayloadType = view.findViewById(R.id.rgPayloadType)
            etCustomPayload = view.findViewById(R.id.etCustomPayload)
            cbWaitForResponse = view.findViewById(R.id.cbWaitForResponse)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar views: ${e.message}")
            Toast.makeText(this, "Erro no layout: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        
        val rgActionType = view.findViewById<RadioGroup>(R.id.rgActionType)
        
        when (actionType) {
            ActionType.WEBHOOK_AUTO -> rgActionType.check(R.id.rbWebhookAuto)
            ActionType.WEBHOOK_AUTH -> rgActionType.check(R.id.rbWebhookAuth)
        }
        
        rgActionType.setOnCheckedChangeListener { _, checkedId ->
            actionType = when (checkedId) {
                R.id.rbWebhookAuto -> ActionType.WEBHOOK_AUTO
                R.id.rbWebhookAuth -> ActionType.WEBHOOK_AUTH
                else -> ActionType.WEBHOOK_AUTO
            }
        }
        
        carregarWebhooksGuardados()
        webhookUrl?.let { actWebhookUrl.setText(it) }
        
        if (isEditMode && selectedWebhookName != null) {
            val webhook = savedWebhooks.find { it.name == selectedWebhookName }
            webhook?.let { carregarDadosWebhook(it) }
        } else {
            carregarConfiguracoesDasVariaveis()
        }
        
        btnSaveWebhook.setOnClickListener {
            val url = actWebhookUrl.text.toString().trim()
            val name = etWebhookName.text.toString().trim()
            
            if (url.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Nome e URL são obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val authType = when (rgAuthType.checkedRadioButtonId) {
                R.id.rbAuthBasic -> AuthType.BASIC
                R.id.rbAuthBearer -> AuthType.BEARER
                R.id.rbAuthApiKey -> AuthType.API_KEY
                else -> AuthType.NONE
            }
            
            val currentSecurityConfig = SecurityConfig(
                signWithHmac = cbSignHmac.isChecked,
                hmacSecret = if (cbSignHmac.isChecked) etHmacSecret.text.toString() else null,
                pgpEnabled = false,
                pgpPublicKey = null,
                pgpFormat = null,
                auth = com.seuapp.notificationautomator.data.model.AuthConfig(
                    type = authType,
                    username = if (authType == AuthType.BASIC) etAuthUsername.text.toString() else null,
                    password = if (authType == AuthType.BASIC) etAuthPassword.text.toString() else null,
                    token = if (authType == AuthType.BEARER) etAuthToken.text.toString() else null,
                    apiKeyName = if (authType == AuthType.API_KEY) etAuthKeyName.text.toString() else null,
                    apiKeyValue = if (authType == AuthType.API_KEY) etAuthKeyValue.text.toString() else null
                )
            )
            
            val currentHeaders = try {
                val headersText = etHeadersAdvanced.text.toString()
                if (headersText.isNotBlank()) {
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    gson.fromJson(headersText, type)
                } else null
            } catch (e: Exception) {
                null
            }
            
            if (selectedWebhookName != null) {
                webhookRepository.updateWebhook(
                    oldName = selectedWebhookName!!,
                    newName = name,
                    newUrl = url,
                    securityConfig = currentSecurityConfig,
                    headers = currentHeaders,
                    timeoutSeconds = etTimeout.text.toString().toIntOrNull() ?: 10,
                    maxRetries = etRetries.text.toString().toIntOrNull() ?: 3,
                    useCustomPayload = rgPayloadType.checkedRadioButtonId == R.id.rbPayloadCustom,
                    customPayloadTemplate = if (rgPayloadType.checkedRadioButtonId == R.id.rbPayloadCustom) 
                        etCustomPayload.text.toString() else null,
                    waitForResponse = cbWaitForResponse.isChecked
                )
                selectedWebhookName = name
                Toast.makeText(this, "✅ Webhook atualizado!", Toast.LENGTH_SHORT).show()
            } else {
                val saved = webhookRepository.saveWebhook(
                    name = name,
                    url = url,
                    securityConfig = currentSecurityConfig,
                    headers = currentHeaders,
                    timeoutSeconds = etTimeout.text.toString().toIntOrNull() ?: 10,
                    maxRetries = etRetries.text.toString().toIntOrNull() ?: 3,
                    useCustomPayload = rgPayloadType.checkedRadioButtonId == R.id.rbPayloadCustom,
                    customPayloadTemplate = if (rgPayloadType.checkedRadioButtonId == R.id.rbPayloadCustom) 
                        etCustomPayload.text.toString() else null,
                    waitForResponse = cbWaitForResponse.isChecked
                )
                selectedWebhookName = saved.name
                Toast.makeText(this, "✅ Webhook guardado!", Toast.LENGTH_SHORT).show()
            }
            
            carregarWebhooksGuardados()
        }
        
        btnTestWebhook.setOnClickListener {
            val url = actWebhookUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "URL é obrigatório", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            testarWebhook(url)
        }
        
        btnDeleteWebhook.setOnClickListener {
            if (selectedWebhookName == null) {
                Toast.makeText(this, "Seleciona um webhook", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            android.app.AlertDialog.Builder(this)
                .setTitle("Eliminar")
                .setMessage("Eliminar '${selectedWebhookName}'?")
                .setPositiveButton("Sim") { _, _ ->
                    webhookRepository.deleteWebhook(selectedWebhookName!!)
                    selectedWebhookName = null
                    actWebhookUrl.text.clear()
                    etWebhookName.text.clear()
                    carregarWebhooksGuardados()
                    Toast.makeText(this, "Webhook eliminado", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Não", null)
                .show()
        }
        
        cbSignHmac.setOnCheckedChangeListener { _, isChecked ->
            layoutHmacSecret.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        rgAuthType.setOnCheckedChangeListener { _, checkedId ->
            layoutAuthBasic.visibility = View.GONE
            layoutAuthBearer.visibility = View.GONE
            layoutAuthApiKey.visibility = View.GONE
            
            when (checkedId) {
                R.id.rbAuthBasic -> layoutAuthBasic.visibility = View.VISIBLE
                R.id.rbAuthBearer -> layoutAuthBearer.visibility = View.VISIBLE
                R.id.rbAuthApiKey -> layoutAuthApiKey.visibility = View.VISIBLE
            }
        }
        
        webhookHeaders?.let {
            etHeadersAdvanced.setText(gson.toJson(it))
        }
        
        view.findViewById<Button>(R.id.btnTemplateN8n)?.setOnClickListener {
            etHeadersAdvanced.setText("""{"Content-Type": "application/json", "User-Agent": "n8n"}""")
        }
        
        view.findViewById<Button>(R.id.btnTemplateZapier)?.setOnClickListener {
            etHeadersAdvanced.setText("""{"Content-Type": "application/json", "User-Agent": "Zapier"}""")
        }
        
        view.findViewById<Button>(R.id.btnTemplateIfttt)?.setOnClickListener {
            etHeadersAdvanced.setText("""{"Content-Type": "application/json", "User-Agent": "IFTTT"}""")
        }
        
        view.findViewById<Button>(R.id.btnTemplateHomeAssistant)?.setOnClickListener {
            etHeadersAdvanced.setText("""{"Content-Type": "application/json", "User-Agent": "HomeAssistant"}""")
        }
        
        view.findViewById<TextView>(R.id.chipVarId)?.setOnClickListener {
            inserirVariavelHeader("id", "{{notification.id}}")
        }
        
        view.findViewById<TextView>(R.id.chipVarTitle)?.setOnClickListener {
            inserirVariavelHeader("title", "{{notification.title}}")
        }
        
        view.findViewById<TextView>(R.id.chipVarText)?.setOnClickListener {
            inserirVariavelHeader("text", "{{notification.text}}")
        }
        
        view.findViewById<TextView>(R.id.chipVarPackage)?.setOnClickListener {
            inserirVariavelHeader("package", "{{notification.package}}")
        }
        
        view.findViewById<TextView>(R.id.chipVarRule)?.setOnClickListener {
            inserirVariavelHeader("rule", "{{rule.name}}")
        }
        
        rgPayloadType.setOnCheckedChangeListener { _, checkedId ->
            etCustomPayload.visibility = if (checkedId == R.id.rbPayloadCustom) View.VISIBLE else View.GONE
        }
        
        carregarConfiguracoesAvancadas()
        
        btnPrevious.setOnClickListener { setupStep(2) }
        btnNext.setOnClickListener {
            webhookUrl = actWebhookUrl.text.toString().takeIf { it.isNotBlank() }
            if (salvarConfiguracoesWebhook()) {
                setupStep(4)
            }
        }
    }

    private fun carregarConfiguracoesDasVariaveis() {
        webhookUrl?.let { actWebhookUrl.setText(it) }
        
        cbSignHmac.isChecked = securityConfig.signWithHmac
        if (securityConfig.signWithHmac) {
            layoutHmacSecret.visibility = View.VISIBLE
            etHmacSecret.setText(securityConfig.hmacSecret ?: "")
        }
        
        when (securityConfig.auth.type) {
            AuthType.BASIC -> {
                rgAuthType.check(R.id.rbAuthBasic)
                layoutAuthBasic.visibility = View.VISIBLE
                etAuthUsername.setText(securityConfig.auth.username ?: "")
                etAuthPassword.setText(securityConfig.auth.password ?: "")
            }
            AuthType.BEARER -> {
                rgAuthType.check(R.id.rbAuthBearer)
                layoutAuthBearer.visibility = View.VISIBLE
                etAuthToken.setText(securityConfig.auth.token ?: "")
            }
            AuthType.API_KEY -> {
                rgAuthType.check(R.id.rbAuthApiKey)
                layoutAuthApiKey.visibility = View.VISIBLE
                etAuthKeyName.setText(securityConfig.auth.apiKeyName ?: "")
                etAuthKeyValue.setText(securityConfig.auth.apiKeyValue ?: "")
            }
            else -> rgAuthType.check(R.id.rbAuthNone)
        }
        
        webhookHeaders?.let {
            etHeadersAdvanced.setText(gson.toJson(it))
        }
        
        etTimeout.setText(advancedConfig.timeoutSeconds.toString())
        etRetries.setText(advancedConfig.maxRetries.toString())
        
        if (advancedConfig.useCustomPayload) {
            rgPayloadType.check(R.id.rbPayloadCustom)
            etCustomPayload.visibility = View.VISIBLE
            etCustomPayload.setText(advancedConfig.customPayloadTemplate ?: "")
        }
        
        cbWaitForResponse.isChecked = advancedConfig.waitForResponse
    }

    private fun carregarDadosWebhook(webhook: SavedWebhook) {
        actWebhookUrl.setText(webhook.url)
        etWebhookName.setText(webhook.name)
        selectedWebhookName = webhook.name
        
        webhook.securityConfig?.let { config ->
            cbSignHmac.isChecked = config.signWithHmac
            if (config.signWithHmac) {
                layoutHmacSecret.visibility = View.VISIBLE
                etHmacSecret.setText(config.hmacSecret ?: "")
            } else {
                layoutHmacSecret.visibility = View.GONE
                etHmacSecret.text.clear()
            }
            
            when (config.auth.type) {
                AuthType.BASIC -> {
                    rgAuthType.check(R.id.rbAuthBasic)
                    layoutAuthBasic.visibility = View.VISIBLE
                    etAuthUsername.setText(config.auth.username ?: "")
                    etAuthPassword.setText(config.auth.password ?: "")
                    layoutAuthBearer.visibility = View.GONE
                    layoutAuthApiKey.visibility = View.GONE
                }
                AuthType.BEARER -> {
                    rgAuthType.check(R.id.rbAuthBearer)
                    layoutAuthBearer.visibility = View.VISIBLE
                    etAuthToken.setText(config.auth.token ?: "")
                    layoutAuthBasic.visibility = View.GONE
                    layoutAuthApiKey.visibility = View.GONE
                }
                AuthType.API_KEY -> {
                    rgAuthType.check(R.id.rbAuthApiKey)
                    layoutAuthApiKey.visibility = View.VISIBLE
                    etAuthKeyName.setText(config.auth.apiKeyName ?: "")
                    etAuthKeyValue.setText(config.auth.apiKeyValue ?: "")
                    layoutAuthBasic.visibility = View.GONE
                    layoutAuthBearer.visibility = View.GONE
                }
                else -> {
                    rgAuthType.check(R.id.rbAuthNone)
                    layoutAuthBasic.visibility = View.GONE
                    layoutAuthBearer.visibility = View.GONE
                    layoutAuthApiKey.visibility = View.GONE
                }
            }
        } ?: run {
            cbSignHmac.isChecked = false
            layoutHmacSecret.visibility = View.GONE
            etHmacSecret.text.clear()
            rgAuthType.check(R.id.rbAuthNone)
            layoutAuthBasic.visibility = View.GONE
            layoutAuthBearer.visibility = View.GONE
            layoutAuthApiKey.visibility = View.GONE
        }
        
        if (webhook.headers != null) {
            etHeadersAdvanced.setText(gson.toJson(webhook.headers))
        } else {
            etHeadersAdvanced.text.clear()
        }
        
        etTimeout.setText(webhook.timeoutSeconds.toString())
        etRetries.setText(webhook.maxRetries.toString())
        
        if (webhook.useCustomPayload) {
            rgPayloadType.check(R.id.rbPayloadCustom)
            etCustomPayload.visibility = View.VISIBLE
            etCustomPayload.setText(webhook.customPayloadTemplate ?: "")
        } else {
            rgPayloadType.check(R.id.rbPayloadDefault)
            etCustomPayload.visibility = View.GONE
            etCustomPayload.text.clear()
        }
        
        cbWaitForResponse.isChecked = webhook.waitForResponse
    }

    private fun carregarWebhooksGuardados() {
        if (!::webhookRepository.isInitialized) {
            Log.e(TAG, "webhookRepository não inicializado!")
            return
        }
        
        savedWebhooks = webhookRepository.getAllSavedWebhooks()
        
        layoutSavedWebhooks.removeAllViews()
        
        val cardNovo = layoutInflater.inflate(R.layout.item_saved_webhook, layoutSavedWebhooks, false)
        
        val tvNomeNovo = cardNovo.findViewById<TextView>(R.id.tvWebhookName)
        val tvUrlNovo = cardNovo.findViewById<TextView>(R.id.tvWebhookUrl)
        val btnUsarNovo = cardNovo.findViewById<Button>(R.id.btnUseWebhook)
        val btnAtualizarNovo = cardNovo.findViewById<Button>(R.id.btnUpdateWebhook)
        val btnEliminarNovo = cardNovo.findViewById<Button>(R.id.btnDeleteWebhook)
        
        tvNomeNovo.text = "➕ Novo Webhook"
        tvUrlNovo.text = "http://localhost/"
        
        btnAtualizarNovo.visibility = View.GONE
        btnEliminarNovo.visibility = View.GONE
        btnUsarNovo.visibility = View.VISIBLE
        btnUsarNovo.text = "Usar"
        
        btnUsarNovo.setOnClickListener {
            actWebhookUrl.setText("http://localhost/")
            etWebhookName.setText("")
            selectedWebhookName = null
            
            cbSignHmac.isChecked = false
            layoutHmacSecret.visibility = View.GONE
            etHmacSecret.text.clear()
            
            rgAuthType.check(R.id.rbAuthNone)
            layoutAuthBasic.visibility = View.GONE
            etAuthUsername.text.clear()
            etAuthPassword.text.clear()
            layoutAuthBearer.visibility = View.GONE
            etAuthToken.text.clear()
            layoutAuthApiKey.visibility = View.GONE
            etAuthKeyName.text.clear()
            etAuthKeyValue.text.clear()
            
            etHeadersAdvanced.text.clear()
            
            etTimeout.setText("10")
            etRetries.setText("3")
            rgPayloadType.check(R.id.rbPayloadDefault)
            etCustomPayload.visibility = View.GONE
            etCustomPayload.text.clear()
            cbWaitForResponse.isChecked = false
            
            carregarWebhooksGuardados()
        }
        
        layoutSavedWebhooks.addView(cardNovo)
        
        if (savedWebhooks.isNotEmpty()) {
            savedWebhooks.forEach { webhook ->
                val cardView = layoutInflater.inflate(R.layout.item_saved_webhook, layoutSavedWebhooks, false)
                
                val tvName = cardView.findViewById<TextView>(R.id.tvWebhookName)
                val tvUrl = cardView.findViewById<TextView>(R.id.tvWebhookUrl)
                val btnUsar = cardView.findViewById<Button>(R.id.btnUseWebhook)
                val btnAtualizar = cardView.findViewById<Button>(R.id.btnUpdateWebhook)
                val btnEliminar = cardView.findViewById<Button>(R.id.btnDeleteWebhook)
                
                tvName.text = webhook.name
                tvUrl.text = webhook.url
                
                val isSelected = (selectedWebhookName == webhook.name)
                
                if (isSelected) {
                    cardView.setBackgroundColor(0x1F6200EE.toInt())
                } else {
                    cardView.setBackgroundColor(0xFFFFFFFF.toInt())
                }
                
                btnUsar.visibility = View.VISIBLE
                btnAtualizar.visibility = View.GONE
                btnEliminar.visibility = View.VISIBLE
                
                btnUsar.text = "Usar"
                btnEliminar.text = "Eliminar"
                
                btnUsar.setOnClickListener {
                    carregarDadosWebhook(webhook)
                    carregarWebhooksGuardados()
                }
                
                btnEliminar.setOnClickListener {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Eliminar webhook")
                        .setMessage("Tens a certeza que queres eliminar '${webhook.name}'?")
                        .setPositiveButton("Sim") { _, _ ->
                            webhookRepository.deleteWebhook(webhook.name)
                            if (selectedWebhookName == webhook.name) {
                                selectedWebhookName = null
                                actWebhookUrl.text.clear()
                                etWebhookName.text.clear()
                            }
                            carregarWebhooksGuardados()
                            Toast.makeText(this, "Webhook eliminado", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Não", null)
                        .show()
                }
                
                layoutSavedWebhooks.addView(cardView)
            }
        }
    }

    private fun testarWebhook(url: String) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@RuleCreateActivity, "⏳ A testar...", Toast.LENGTH_SHORT).show()
                
                val result = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .writeTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                        .build()
                    
                    val request = Request.Builder()
                        .url(url)
                        .method("HEAD", null)
                        .build()
                    
                    try {
                        val response = client.newCall(request).execute()
                        "✅ Sucesso! Código: ${response.code}"
                    } catch (e: Exception) {
                        "❌ Erro: ${e.message}"
                    }
                }
                
                Toast.makeText(this@RuleCreateActivity, result, Toast.LENGTH_LONG).show()
                
            } catch (e: Exception) {
                Toast.makeText(this@RuleCreateActivity, "❌ Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun inserirVariavelHeader(chave: String, valorVar: String) {
        val currentText = etHeadersAdvanced.text.toString().trim()
        val newEntry = "\"$chave\": $valorVar"
        
        val newText = when {
            currentText.isEmpty() || currentText == "{}" -> {
                "{$newEntry}"
            }
            currentText.startsWith("{") && currentText.endsWith("}") -> {
                currentText.replaceRange(currentText.length-1, currentText.length-1, ", $newEntry")
            }
            else -> {
                "{$newEntry}"
            }
        }
        
        etHeadersAdvanced.setText(newText)
    }

    private fun carregarConfiguracoesAvancadas() {
        etTimeout.setText(advancedConfig.timeoutSeconds.toString())
        etRetries.setText(advancedConfig.maxRetries.toString())
        
        if (advancedConfig.useCustomPayload) {
            rgPayloadType.check(R.id.rbPayloadCustom)
            etCustomPayload.visibility = View.VISIBLE
            advancedConfig.customPayloadTemplate?.let { etCustomPayload.setText(it) }
        } else {
            rgPayloadType.check(R.id.rbPayloadDefault)
            etCustomPayload.visibility = View.GONE
        }
        
        cbWaitForResponse.isChecked = advancedConfig.waitForResponse
    }

    private fun salvarConfiguracoesWebhook(): Boolean {
        webhookUrl = actWebhookUrl.text.toString().takeIf { it.isNotBlank() }
        
        val headersText = etHeadersAdvanced.text.toString()
        if (headersText.isNotBlank()) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                webhookHeaders = gson.fromJson(headersText, type)
            } catch (e: Exception) {
                Toast.makeText(this, "Headers inválidos. Use formato JSON", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            webhookHeaders = null
        }
        
        val authType = when (rgAuthType.checkedRadioButtonId) {
            R.id.rbAuthBasic -> AuthType.BASIC
            R.id.rbAuthBearer -> AuthType.BEARER
            R.id.rbAuthApiKey -> AuthType.API_KEY
            else -> AuthType.NONE
        }
        
        securityConfig = SecurityConfig(
            signWithHmac = cbSignHmac.isChecked,
            hmacSecret = if (cbSignHmac.isChecked) etHmacSecret.text.toString() else null,
            pgpEnabled = false,
            pgpPublicKey = null,
            pgpFormat = null,
            auth = com.seuapp.notificationautomator.data.model.AuthConfig(
                type = authType,
                username = if (authType == AuthType.BASIC) etAuthUsername.text.toString() else null,
                password = if (authType == AuthType.BASIC) etAuthPassword.text.toString() else null,
                token = if (authType == AuthType.BEARER) etAuthToken.text.toString() else null,
                apiKeyName = if (authType == AuthType.API_KEY) etAuthKeyName.text.toString() else null,
                apiKeyValue = if (authType == AuthType.API_KEY) etAuthKeyValue.text.toString() else null
            )
        )
        
        advancedConfig = AdvancedWebhookConfig(
            timeoutSeconds = etTimeout.text.toString().toIntOrNull() ?: 10,
            maxRetries = etRetries.text.toString().toIntOrNull() ?: 3,
            useCustomPayload = rgPayloadType.checkedRadioButtonId == R.id.rbPayloadCustom,
            customPayloadTemplate = if (rgPayloadType.checkedRadioButtonId == R.id.rbPayloadCustom) 
                etCustomPayload.text.toString() else null,
            waitForResponse = cbWaitForResponse.isChecked
        )
        
        return true
    }

    private fun showStepReview() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.step_review, container, true)
        

        val tvReviewApp = view.findViewById<TextView>(R.id.tvReviewApp)
        val tvReviewConditions = view.findViewById<TextView>(R.id.tvReviewConditions)
        val tvReviewAction = view.findViewById<TextView>(R.id.tvReviewAction)
        val etRuleName = view.findViewById<EditText>(R.id.etRuleName)
        val cbActivateNow = view.findViewById<CheckBox>(R.id.cbActivateNow)
        val cbApplyToCurrent = view.findViewById<CheckBox>(R.id.cbApplyToCurrent)
        
        if (applyRuleNow && notificationId > 0) {
            cbApplyToCurrent.visibility = View.VISIBLE
            cbApplyToCurrent.isChecked = true
        } else {
            cbApplyToCurrent.visibility = View.GONE
        }
        
        if (ruleName.isNotEmpty()) {
            etRuleName.setText(ruleName)
        } else {
            val suggestedName = buildString {
                append(selectedAppName ?: "Qualquer app")
                if (!titleContains.isNullOrBlank()) {
                    append(" - \"$titleContains\"")
                }
            }
            etRuleName.setText(suggestedName)
        }
        
        tvReviewApp.text = "📱 App: ${selectedAppName ?: "Qualquer app"}"
        
        val conditions = mutableListOf<String>()
        titleContains?.let { conditions.add("Título contém \"$it\"") }
        textContains?.let { conditions.add("Texto contém \"$it\"") }
        if (hourFrom != null && hourTo != null) {
            conditions.add("Horário: $hourFrom-$hourTo")
        }
        if (selectedDays.isNotEmpty()) {
            val days = selectedDays.joinToString(" ") {
                when (it) {
                    "MONDAY" -> "Seg"
                    "TUESDAY" -> "Ter"
                    "WEDNESDAY" -> "Qua"
                    "THURSDAY" -> "Qui"
                    "FRIDAY" -> "Sex"
                    "SATURDAY" -> "Sáb"
                    "SUNDAY" -> "Dom"
                    else -> it
                }
            }
            conditions.add("Dias: $days")
        }
        when (isSilent) {
            true -> conditions.add("🔇 Silenciosa")
            false -> conditions.add("🔔 Alertante")
            else -> { }
        }
        if (hasImage) conditions.add("🖼️ Com imagem")
        
        tvReviewConditions.text = if (conditions.isEmpty()) "Sem condições" else conditions.joinToString("\n")
        
        val actionText = StringBuilder()
        actionText.append(when (actionType) {
            ActionType.WEBHOOK_AUTO -> "🌐 Webhook automático"
            ActionType.WEBHOOK_AUTH -> "🔐 Webhook com autorização"
        })

        val urlToShow = webhookUrl ?: actWebhookUrl.text.toString().takeIf { it.isNotBlank() }
        actionText.append("\n📌 URL: ${urlToShow ?: "Não definido"}")

        if (securityConfig.signWithHmac) {
            actionText.append("\n🔏 Assinado com HMAC")
        }
        if (securityConfig.auth.type != AuthType.NONE) {
            actionText.append("\n🔑 Auth: ${securityConfig.auth.type}")
        }

        actionText.append("\n⏱️ Timeout: ${advancedConfig.timeoutSeconds}s, Retries: ${advancedConfig.maxRetries}")

        if (advancedConfig.waitForResponse) {
            actionText.append("\n⏳ Aguarda resposta do servidor")
        }

        tvReviewAction.text = actionText.toString()
        
        btnPrevious.setOnClickListener { setupStep(3) }
        btnSave.setOnClickListener {
            ruleName = etRuleName.text.toString()
            if (ruleName.isBlank()) {
                Toast.makeText(this@RuleCreateActivity, "Nome da regra é obrigatório", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val rule = Rule(
                id = if (isEditMode) editingRuleId ?: 0 else 0,
                name = ruleName,
                isActive = if (isEditMode) {
                    true
                } else {
                    cbActivateNow.isChecked
                },
                appPackage = selectedAppPackage,
                titleContains = titleContains,
                textContains = textContains,
                hourFrom = hourFrom,
                hourTo = hourTo,
                daysOfWeek = if (selectedDays.isNotEmpty()) selectedDays.joinToString(",") else null,
                isSilent = isSilent,
                hasImage = hasImage,
                actionType = actionType,
                webhookUrl = webhookUrl,
                webhookHeaders = gson.toJson(webhookHeaders),
                savedWebhooks = gson.toJson(savedWebhooks),
                selectedWebhookName = selectedWebhookName,
                securityConfig = gson.toJson(securityConfig),
                advancedConfig = gson.toJson(advancedConfig)
            )
            
            if (isEditMode) {
                viewModel.updateRule(rule)
                Toast.makeText(this@RuleCreateActivity, "Regra atualizada!", Toast.LENGTH_SHORT).show()
                
                if (applyRuleNow && notificationId > 0 && cbApplyToCurrent.isChecked) {
                    viewModel.applyRuleToNotification(rule, notificationId)
                }
            } else {
                viewModel.saveRule(rule) { newRuleId ->
                    if (applyRuleNow && notificationId > 0 && cbApplyToCurrent.isChecked) {
                        val ruleWithId = rule.copy(id = newRuleId)
                        viewModel.applyRuleToNotification(ruleWithId, notificationId)
                    }
                    Toast.makeText(this@RuleCreateActivity, "Regra criada!", Toast.LENGTH_SHORT).show()
                }
            }
            
            finish()
        }
    }
}