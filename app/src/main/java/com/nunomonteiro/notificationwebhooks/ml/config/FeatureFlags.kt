package com.nunomonteiro.notificationwebhooks.ml.config

import android.os.Build

/**
 * Controlo central de todas as features de ML
 * TODAS DESLIGADAS INICIALMENTE - SEGURANÇA PRIMEIRO
 * 
 * NOTA: Versão simplificada sem dependências externas
 */
object FeatureFlags {
    // Master switch - tudo desligado
    const val ML_ENABLED = true
    
    // Features individuais (dependentes do master)
    const val CLASSIFICATION_ENABLED = true
    const val SUGGESTIONS_ENABLED = false
    const val LEARNING_ENABLED = false
    
    // UI Controls
    const val SHOW_CATEGORIES = false
    const val SHOW_SUGGESTIONS = false
    
    // Performance
    const val ML_TIMEOUT_MS = 100L
    const val ML_BATCH_SIZE = 10
    
    // Hardware acceleration
    val SUPPORTS_GPU = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    
    // Modelos
    const val MODEL_VERSION = 1
    const val MODEL_MIN_SIZE_KB = 1024
    const val MODEL_MAX_SIZE_KB = 10240
    
    // Debug - fixo para já (depois usaremos BuildConfig)
    const val ML_DEBUG = true
}