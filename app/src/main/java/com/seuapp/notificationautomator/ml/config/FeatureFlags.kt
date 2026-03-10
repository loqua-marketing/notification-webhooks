package com.seuapp.notificationautomator.ml.config

import android.os.Build

/**
 * Controlo central de todas as features de ML
 * TODAS DESLIGADAS INICIALMENTE - SEGURANÇA PRIMEIRO
 */
object FeatureFlags {
    // Master switch - tudo desligado em produção
    const val ML_ENABLED = false
    
    // Features individuais (dependentes do master)
    const val CLASSIFICATION_ENABLED = ML_ENABLED && false
    const val SUGGESTIONS_ENABLED = ML_ENABLED && false
    const val LEARNING_ENABLED = ML_ENABLED && false
    
    // UI Controls
    const val SHOW_CATEGORIES = CLASSIFICATION_ENABLED
    const val SHOW_SUGGESTIONS = SUGGESTIONS_ENABLED
    
    // Performance
    const val ML_TIMEOUT_MS = 100L // Máx 100ms
    const val ML_BATCH_SIZE = 10
    
    // Hardware acceleration (opcional)
    val SUPPORTS_GPU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    
    // Modelos
    const val MODEL_VERSION = 1
    const val MODEL_MIN_SIZE_KB = 1024 // 1MB
    const val MODEL_MAX_SIZE_KB = 10240 // 10MB
    
    // Debug - logs detalhados em desenvolvimento
    val ML_DEBUG = BuildConfig.DEBUG
}
