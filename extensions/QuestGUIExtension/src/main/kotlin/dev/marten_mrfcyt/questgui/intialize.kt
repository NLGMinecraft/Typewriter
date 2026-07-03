package dev.marten_mrfcyt.questgui

import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import com.typewritermc.engine.paper.plugin
import org.bukkit.event.HandlerList

@Singleton
object QuestGUIInitializer : Initializable {
    private val guiListener = QuestGuiListener()

    override suspend fun initialize() {
        plugin.server.pluginManager.registerEvents(guiListener, plugin)
        println("QuestGUI Initialized!")
    }

    override suspend fun shutdown() {
        HandlerList.unregisterAll(guiListener)
        println("QuestGUI Shutdown!")
    }
}
