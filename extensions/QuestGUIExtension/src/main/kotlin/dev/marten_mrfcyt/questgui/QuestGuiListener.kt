package dev.marten_mrfcyt.questgui

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

/**
 * Routes clicks inside a quest GUI back to its [QuestGuiHolder] and prevents any
 * item from being moved out of the menu.
 */
class QuestGuiListener : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? QuestGuiHolder ?: return
        // Cancel every click while the GUI is open so items can never be taken.
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        if (player != holder.player) return

        // Only act on clicks inside the GUI itself (the top inventory).
        if (event.rawSlot !in 0 until QuestGuiHolder.SIZE) return
        holder.handleClick(event.rawSlot)
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder is QuestGuiHolder) {
            event.isCancelled = true
        }
    }
}
