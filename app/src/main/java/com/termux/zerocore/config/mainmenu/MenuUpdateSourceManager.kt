package com.termux.zerocore.config.mainmenu

import android.content.Context
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.termux.R
import com.termux.zerocore.http.HTTPIP
import com.termux.zerocore.utils.XinhaoStoragePath
import java.io.File

object MenuUpdateSourceManager {

    private const val SOURCES_FILE = "menu_update_sources.json"
    private const val SELECTED_FILE = "selected_update_source.txt"
    private val gson = Gson()

    @JvmStatic
    fun getDefaultSource(): MenuUpdateSource {
        return MenuUpdateSource(
            id = MenuUpdateSource.DEFAULT_ID,
            url = HTTPIP.MENU_PACKAGE_URL,
            isDefault = true
        )
    }

    @JvmStatic
    fun buildListItems(context: Context): List<MenuUpdateSource> {
        val selectedId = getSelectedSourceId(context)
        val items = ArrayList<MenuUpdateSource>()
        val default = getDefaultSource()
        items.add(default.copy())
        getCustomSources(context).forEach { source ->
            items.add(source)
        }
        items.add(
            MenuUpdateSource(
                id = MenuUpdateSource.ADD_ID,
                url = "",
                isAddAction = true
            )
        )
        return items
    }

    @JvmStatic
    fun isSelected(source: MenuUpdateSource, selectedId: String): Boolean {
        if (source.isAddAction) {
            return false
        }
        return source.id == selectedId
    }

    @JvmStatic
    fun getSelectedSourceId(context: Context): String {
        val file = File(XinhaoStoragePath.getMenuDir(context), SELECTED_FILE)
        if (!file.exists()) {
            return MenuUpdateSource.DEFAULT_ID
        }
        val id = file.readText().trim()
        if (id.isEmpty()) {
            return MenuUpdateSource.DEFAULT_ID
        }
        if (id == MenuUpdateSource.DEFAULT_ID) {
            return id
        }
        val exists = getCustomSources(context).any { it.id == id }
        return if (exists) id else MenuUpdateSource.DEFAULT_ID
    }

    @JvmStatic
    fun setSelectedSourceId(context: Context, sourceId: String) {
        val menuDir = XinhaoStoragePath.getMenuDir(context)
        menuDir.mkdirs()
        File(menuDir, SELECTED_FILE).writeText(sourceId)
    }

    @JvmStatic
    fun getSelectedUrl(context: Context): String {
        val selectedId = getSelectedSourceId(context)
        if (selectedId == MenuUpdateSource.DEFAULT_ID) {
            return getDefaultSource().url
        }
        return getCustomSources(context).firstOrNull { it.id == selectedId }?.url
            ?: getDefaultSource().url
    }

    @JvmStatic
    fun isCustomSource(source: MenuUpdateSource): Boolean {
        return !source.isDefault && !source.isAddAction
    }

    @JvmStatic
    fun updateCustomSource(context: Context, sourceId: String, url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        if (getDefaultSource().url == trimmed) {
            return false
        }
        val sources = getCustomSources(context).toMutableList()
        val index = sources.indexOfFirst { it.id == sourceId }
        if (index < 0) {
            return false
        }
        if (sources.any { it.id != sourceId && it.url == trimmed }) {
            return false
        }
        sources[index] = sources[index].copy(url = trimmed)
        saveCustomSources(context, sources)
        return true
    }

    @JvmStatic
    fun deleteCustomSource(context: Context, sourceId: String): Boolean {
        val sources = getCustomSources(context).toMutableList()
        if (!sources.removeAll { it.id == sourceId }) {
            return false
        }
        val wasSelected = getSelectedSourceId(context) == sourceId
        saveCustomSources(context, sources)
        if (wasSelected) {
            setSelectedSourceId(context, MenuUpdateSource.DEFAULT_ID)
        }
        return true
    }

    @JvmStatic
    fun addCustomSource(context: Context, url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            return false
        }
        val sources = getCustomSources(context).toMutableList()
        if (sources.any { it.url == trimmed } || getDefaultSource().url == trimmed) {
            return false
        }
        val newSource = MenuUpdateSource(
            id = "custom_${System.currentTimeMillis()}",
            url = trimmed
        )
        sources.add(newSource)
        saveCustomSources(context, sources)
        return true
    }

    @JvmStatic
    fun getSourceLabel(source: MenuUpdateSource): String {
        if (source.isDefault) {
            return UUtils.getString(R.string.menu_update_source_default)
        }
        if (source.isAddAction) {
            return UUtils.getString(R.string.menu_update_source_add)
        }
        return UUtils.getString(R.string.menu_update_source_custom)
    }

    private fun getCustomSources(context: Context): List<MenuUpdateSource> {
        val file = File(XinhaoStoragePath.getMenuDir(context), SOURCES_FILE)
        if (!file.exists()) {
            return emptyList()
        }
        return try {
            val type = object : TypeToken<List<MenuUpdateSource>>() {}.type
            gson.fromJson<List<MenuUpdateSource>>(file.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveCustomSources(context: Context, sources: List<MenuUpdateSource>) {
        val menuDir = XinhaoStoragePath.getMenuDir(context)
        menuDir.mkdirs()
        File(menuDir, SOURCES_FILE).writeText(gson.toJson(sources))
    }
}
