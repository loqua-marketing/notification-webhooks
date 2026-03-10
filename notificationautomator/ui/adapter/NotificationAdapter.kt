package com.seuapp.notificationautomator.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.seuapp.notificationautomator.R
import com.seuapp.notificationautomator.data.model.Notification
import com.seuapp.notificationautomator.data.model.NotificationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private var notifications: List<Notification>,
    private val onItemClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {
    
    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPackage: TextView = itemView.findViewById(R.id.tvPackage)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvText: TextView = itemView.findViewById(R.id.tvText)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        
        fun bind(notification: Notification, onClick: (Notification) -> Unit) {
            tvPackage.text = notification.packageName
            tvTitle.text = notification.title ?: "(sem título)"
            tvText.text = notification.text ?: "(sem texto)"
            
            val dateFormat = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
            tvTime.text = dateFormat.format(Date(notification.timestamp))
            
            tvStatus.text = getStatusText(notification.status)
            tvStatus.setTextColor(getStatusColor(notification.status))
            
            itemView.setOnClickListener { onClick(notification) }
        }
        
        private fun getStatusText(status: NotificationStatus): String {
            return when (status) {
                NotificationStatus.RECEIVED -> "Recebida"
                NotificationStatus.PROCESSED -> "Processada"
                NotificationStatus.PENDING_AUTH -> "⏳"
                NotificationStatus.IGNORED -> "Ignorada"
            }
        }
        
        private fun getStatusColor(status: NotificationStatus): Int {
            return when (status) {
                NotificationStatus.RECEIVED -> android.graphics.Color.parseColor("#2196F3")
                NotificationStatus.PROCESSED -> android.graphics.Color.parseColor("#4CAF50")
                NotificationStatus.PENDING_AUTH -> android.graphics.Color.parseColor("#FF9800")
                NotificationStatus.IGNORED -> android.graphics.Color.parseColor("#9E9E9E")
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position], onItemClick)
    }
    
    override fun getItemCount(): Int = notifications.size
    
    fun updateNotifications(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
}
