package dev.marten_mrfcyt.questgui

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.engine.paper.command.dsl.DslCommand
import com.typewritermc.engine.paper.command.dsl.command
import com.typewritermc.engine.paper.command.dsl.sender
import com.typewritermc.engine.paper.entry.ManifestEntry
import com.typewritermc.engine.paper.entry.entries.CustomCommandEntry
import com.typewritermc.engine.paper.utils.msg
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

@Entry(
    "quest_gui_command",
    "Registers the /quests command that opens the Quest GUI",
    Colors.BLUE,
    "mdi:book-open-page-variant"
)
/**
 * The `Quest GUI Command` entry registers the root-level `/quests` command
 * (with `/quest` as alias) that opens the Quest GUI for the player who runs it.
 *
 * ## How could this be used?
 * Add one of these to a manifest page to give players a `/quests` command to
 * browse their not-played, active and completed quests.
 */
class QuestGuiCommandEntry(
    override val id: String = "",
    override val name: String = "",
) : ManifestEntry, CustomCommandEntry {
    override fun command(): DslCommand<CommandSourceStack> =
        command("quests", "quest") {
            executes {
                val player = (source.executor as? Player) ?: (sender as? Player)
                if (player == null) {
                    sender.msg("You must be a player to run this command.")
                    return@executes
                }
                QuestGuiHolder(player).open()
            }
        }
}
