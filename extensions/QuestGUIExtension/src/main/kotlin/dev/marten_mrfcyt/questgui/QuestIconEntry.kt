package dev.marten_mrfcyt.questgui

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.StaticEntry
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.utils.item.Item
import com.typewritermc.quest.entries.QuestEntry

/**
 * A `Quest Icon` maps a [QuestEntry] to the item that represents it inside the
 * Quest GUI. Create one of these per quest you want to show with a custom icon.
 *
 * ## How could this be used?
 * Give each quest a recognisable item (a map for an exploration quest, a sword
 * for a combat quest, ...). Quests without a matching icon fall back to a
 * sensible default item based on their state.
 *
 * The currently tracked quest automatically receives an enchant glint in the
 * GUI, so you don't need to configure that here.
 */
@Entry("quest_icon", "An icon mapping for a quest in the Quest GUI", Colors.BLUE, "mdi:bookmark-box-outline")
class QuestIconEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("The quest this icon represents.")
    val quest: Ref<QuestEntry> = emptyRef(),
    @Help("The item shown for this quest in the GUI.")
    val item: Var<Item> = ConstVar(Item.Empty),
) : StaticEntry
