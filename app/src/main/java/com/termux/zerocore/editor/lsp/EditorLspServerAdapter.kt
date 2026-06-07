package com.termux.zerocore.editor.lsp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.termux.R

class EditorLspServerAdapter(
    private val manager: EditorLspManager,
    private val onInstallClick: (EditorLspInstaller.ServerPackage) -> Unit
) : RecyclerView.Adapter<EditorLspServerAdapter.ViewHolder>() {

    private var packages: List<EditorLspInstaller.ServerPackage> = manager.availablePackages()

    fun refresh() {
        packages = manager.availablePackages()
        notifyDataSetChanged()
    }

    fun hasInstallingPackage(): Boolean {
        return packages.any { manager.isPackageInstalling(it.id) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lsp_server, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val serverPackage = packages[position]
        val context = holder.itemView.context
        val installed = manager.isPackageInstalled(serverPackage.id)
        val installing = manager.isPackageInstalling(serverPackage.id)

        holder.icon.text = packageIconText(serverPackage)
        holder.title.text = serverPackage.displayName
        holder.description.text = serverPackage.description
        holder.languages.text = context.getString(
            R.string.editor_settings_lsp_supported_languages,
            formatLanguageLabels(serverPackage.languageIds)
        )
        holder.required.visibility = if (serverPackage.requiredOnFirstOpen) View.VISIBLE else View.GONE

        when {
            installing -> {
                holder.status.visibility = View.INVISIBLE
                holder.progress.visibility = View.VISIBLE
                holder.action.text = context.getString(R.string.editor_settings_lsp_installing)
                holder.action.setTextColor(0xFF48BAF3.toInt())
            }
            installed -> {
                holder.status.visibility = View.VISIBLE
                holder.progress.visibility = View.GONE
                holder.status.setBackgroundResource(R.drawable.shape_lsp_status_installed)
                holder.status.setTextColor(0xFF6FCF97.toInt())
                holder.status.text = context.getString(R.string.editor_settings_lsp_installed)
                holder.action.text = context.getString(R.string.editor_settings_lsp_action_installed)
                holder.action.setTextColor(0xFF6FCF97.toInt())
            }
            else -> {
                holder.status.visibility = View.VISIBLE
                holder.progress.visibility = View.GONE
                holder.status.setBackgroundResource(R.drawable.shape_lsp_status_pending)
                holder.status.setTextColor(0xFFB0B0B0.toInt())
                holder.status.text = context.getString(R.string.editor_settings_lsp_status_not_installed)
                holder.action.text = context.getString(R.string.editor_settings_lsp_action_install)
                holder.action.setTextColor(0xFF48BAF3.toInt())
            }
        }

        holder.itemView.setOnClickListener {
            if (installed || installing) return@setOnClickListener
            onInstallClick(serverPackage)
        }
        holder.itemView.isClickable = !installed && !installing
        holder.itemView.alpha = if (installed) 0.92f else 1f
    }

    override fun getItemCount(): Int = packages.size

    private fun packageIconText(serverPackage: EditorLspInstaller.ServerPackage): String {
        return when (serverPackage.id) {
            EditorLspInstaller.SHELL_BASIC_ID -> "Sh"
            "json" -> "J"
            "typescript" -> "TS"
            "python" -> "Py"
            "yaml" -> "Ym"
            else -> serverPackage.displayName.take(2)
        }
    }

    private fun formatLanguageLabels(languageIds: List<String>): String {
        return languageIds.joinToString(" · ") { languageId ->
            when (languageId) {
                EditorLspManager.LANGUAGE_SHELL -> "Shell (.sh/.bash)"
                EditorLspManager.LANGUAGE_JSON -> "JSON"
                EditorLspManager.LANGUAGE_JSONC -> "JSONC"
                EditorLspManager.LANGUAGE_JAVASCRIPT -> "JavaScript"
                EditorLspManager.LANGUAGE_TYPESCRIPT -> "TypeScript"
                EditorLspManager.LANGUAGE_PYTHON -> "Python"
                EditorLspManager.LANGUAGE_YAML -> "YAML"
                else -> languageId
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: TextView = itemView.findViewById(R.id.lsp_server_icon)
        val title: TextView = itemView.findViewById(R.id.lsp_server_title)
        val description: TextView = itemView.findViewById(R.id.lsp_server_description)
        val languages: TextView = itemView.findViewById(R.id.lsp_server_languages)
        val required: TextView = itemView.findViewById(R.id.lsp_server_required)
        val status: TextView = itemView.findViewById(R.id.lsp_server_status)
        val progress: ProgressBar = itemView.findViewById(R.id.lsp_server_progress)
        val action: TextView = itemView.findViewById(R.id.lsp_server_action)
    }
}
