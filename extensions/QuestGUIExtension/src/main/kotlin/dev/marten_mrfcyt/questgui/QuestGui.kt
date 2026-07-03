package dev.marten_mrfcyt.questgui

import com.typewritermc.core.entries.Query
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.get
import com.typewritermc.quest.activeQuests
import com.typewritermc.quest.completedQuests
import com.typewritermc.quest.entries.QuestEntry
import com.typewritermc.quest.inactiveQuests
import com.typewritermc.quest.isQuestTracked
import com.typewritermc.quest.trackQuest
import com.typewritermc.quest.unTrackQuest
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import kotlin.math.max

private val mini = MiniMessage.miniMessage()
private val plain = PlainTextComponentSerializer.plainText()

/** Parse a MiniMessage string into a non-italic component (vanilla makes item text italic by default). */
private fun text(value: String): Component =
    mini.deserialize(value).decoration(TextDecoration.ITALIC, false)

enum class QuestTab(val tabSlot: Int, val title: String, val fallback: Material) {
    NOT_PLAYED(1, "<gray>Nog te ontdekken", Material.MAP),
    ACTIVE(4, "<gold>Actief", Material.WRITABLE_BOOK),
    COMPLETED(7, "<green>Voltooid", Material.WRITTEN_BOOK),
}

/**
 * A single quest resolved and validated for display. Built once per render so
 * the same quest can never end up in more than one tab.
 */
private class QuestView(
    val ref: Ref<QuestEntry>,
    val displayName: Component,
    /** Lower-cased, tag-stripped name, used to detect duplicate quest titles. */
    val normalizedName: String,
)

/**
 * Holds the state of one open quest GUI. Acts as the [InventoryHolder] so the
 * click listener can recognise our inventory and route clicks back here.
 *
 * Layout: tabs live in the top row, quests fill the four middle rows and the
 * bottom row holds pagination plus a progress summary.
 */
class QuestGuiHolder(val player: Player) : InventoryHolder {
    private var tab = QuestTab.ACTIVE
    private var page = 0
    private val slotQuests = HashMap<Int, Ref<QuestEntry>>()
    private lateinit var inventory: Inventory

    // Snapshot of the current render, so content, page counts and tab counts
    // all agree with each other.
    private var categorized: Map<QuestTab, List<QuestView>> = emptyMap()
    private var duplicateNames: Set<String> = emptySet()

    override fun getInventory(): Inventory = inventory

    fun open() {
        inventory = Bukkit.createInventory(this, SIZE, text("<dark_red>⚜ <gold>Quests van het Koninkrijk</gold> ⚜"))
        render()
        player.openInventory(inventory)
    }

    /**
     * Take a single, consistent snapshot of the player's quests and place each
     * quest in exactly one category. A quest reported under several statuses (a
     * race while its status is changing) is kept only in the most-progressed
     * tab, so it can never appear in all three at once.
     */
    private fun classify(): Map<QuestTab, List<QuestView>> {
        val placed = HashSet<String>()

        fun build(refs: List<Ref<QuestEntry>>): List<QuestView> = refs.mapNotNull { ref ->
            val quest = ref.get() ?: return@mapNotNull null      // skip dangling references
            if (!placed.add(ref.id)) return@mapNotNull null       // already placed in a higher-priority tab
            val (display, normalized) = resolveName(ref, quest) ?: return@mapNotNull null // skip quests without a title
            QuestView(ref, display, normalized)
        }

        // Priority order: completed > active > inactive.
        val completed = build(player.completedQuests())
        val active = build(player.activeQuests())
        val inactive = build(player.inactiveQuests())

        return mapOf(
            QuestTab.COMPLETED to completed,
            QuestTab.ACTIVE to active,
            QuestTab.NOT_PLAYED to inactive,
        )
    }

    /**
     * Resolve and validate a quest's title. Returns `null` when the quest has no
     * usable display name, so it is simply left out of the GUI rather than shown
     * as a nameless item.
     */
    private fun resolveName(ref: Ref<QuestEntry>, quest: QuestEntry): Pair<Component, String>? {
        val raw = runCatching { quest.display(player) }.getOrDefault("")
        val component = runCatching { mini.deserialize(raw) }.getOrElse { Component.text(raw) }
        val stripped = plain.serialize(component).trim()

        if (stripped.isEmpty()) return null // no display name -> hide from the GUI
        return component.decoration(TextDecoration.ITALIC, false) to stripped.lowercase()
    }

    private fun questsFor(tab: QuestTab): List<QuestView> = categorized[tab].orEmpty()

    private fun maxPage(): Int = max(0, (questsFor(tab).size - 1) / PER_PAGE)

    fun handleClick(slot: Int) {
        when (slot) {
            QuestTab.NOT_PLAYED.tabSlot -> switchTab(QuestTab.NOT_PLAYED)
            QuestTab.ACTIVE.tabSlot -> switchTab(QuestTab.ACTIVE)
            QuestTab.COMPLETED.tabSlot -> switchTab(QuestTab.COMPLETED)
            PREV_SLOT -> if (page > 0) {
                page--
                render()
            }
            NEXT_SLOT -> if (page < maxPage()) {
                page++
                render()
            }
            else -> {
                val ref = slotQuests[slot] ?: return
                if (tab != QuestTab.ACTIVE) return
                // Toggle tracking: clicking the tracked quest untracks, any other tracks it.
                if (player.isQuestTracked(ref)) player.unTrackQuest() else player.trackQuest(ref)
                render()
            }
        }
    }

    private fun switchTab(newTab: QuestTab) {
        if (tab != newTab) {
            tab = newTab
            page = 0
        }
        render()
    }

    private fun render() {
        inventory.clear()
        slotQuests.clear()

        categorized = classify()
        duplicateNames = categorized.values.asSequence()
            .flatten()
            .groupingBy { it.normalizedName }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        page = page.coerceIn(0, maxPage())

        val quests = questsFor(tab)
        val pageQuests = quests.drop(page * PER_PAGE).take(PER_PAGE)

        if (pageQuests.isEmpty()) {
            inventory.setItem(EMPTY_SLOT, infoItem(Material.BARRIER, "<red>Geen quests in deze categorie"))
        } else {
            pageQuests.forEachIndexed { i, view ->
                val slot = QUEST_AREA_START + i
                inventory.setItem(slot, questItem(view))
                slotQuests[slot] = view.ref
            }
        }

        renderControls()
    }

    private fun renderControls() {
        // Fill the top and bottom rows with orange glass, then overwrite with controls.
        for (slot in 0 until QUEST_AREA_START) {
            inventory.setItem(slot, filler())
        }
        for (slot in BOTTOM_ROW_START until SIZE) {
            inventory.setItem(slot, filler())
        }

        for (entry in QuestTab.entries) {
            inventory.setItem(entry.tabSlot, tabItem(entry))
        }

        inventory.setItem(PROGRESS_SLOT, progressItem())

        if (page > 0) inventory.setItem(PREV_SLOT, infoItem(Material.ARROW, "<gold>Vorige pagina"))
        if (page < maxPage()) inventory.setItem(NEXT_SLOT, infoItem(Material.ARROW, "<gold>Volgende pagina"))
    }

    private fun questItem(view: QuestView): ItemStack {
        val ref = view.ref
        val tracked = tab == QuestTab.ACTIVE && player.isQuestTracked(ref)

        // Use the configured icon, otherwise a state-based default.
        val configured = Query.find<QuestIconEntry>()
            .firstOrNull { it.quest == ref }
            ?.item?.get(player)?.build(player)
        val item = configured?.takeIf { it.type != Material.AIR } ?: ItemStack(tab.fallback)

        item.editMeta { meta ->
            val name =
                if (tracked) Component.text("⚜ ", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(view.displayName)
                else view.displayName
            meta.displayName(name)

            val lore = mutableListOf<Component>()
            when (tab) {
                QuestTab.NOT_PLAYED -> lore += text("<gray>Dit avontuur wacht nog op je")
                QuestTab.ACTIVE -> {
                    if (tracked) {
                        lore += text("<gold>Je volgt deze quest")
                        lore += text("<dark_gray>Klik om te stoppen met volgen")
                    } else {
                        lore += text("<yellow>Bezig")
                        lore += text("<dark_gray>Klik om deze quest te volgen")
                    }
                }
                QuestTab.COMPLETED -> lore += text("<green>Volbracht!")
            }

            // Surface misconfigured data: several quests sharing one title is
            // almost always the cause of a quest "showing up everywhere".
            if (view.normalizedName in duplicateNames) {
                lore += Component.empty()
                lore += text("<red>⚠ Meerdere quests hebben deze titel")
            }

            meta.lore(lore)

            // Glint for the tracked quest.
            meta.setEnchantmentGlintOverride(tracked)
        }
        return item
    }

    private fun tabItem(entry: QuestTab): ItemStack {
        val selected = entry == tab
        val item = ItemStack(entry.fallback)
        item.editMeta { meta ->
            meta.displayName(text((if (selected) "<bold>" else "") + entry.title))
            val count = questsFor(entry).size
            meta.lore(
                listOf(
                    text("<gray>$count quest(s)"),
                    if (selected) text("<gold>Geopend") else text("<dark_gray>Klik om te bekijken"),
                )
            )
            meta.setEnchantmentGlintOverride(selected)
        }
        return item
    }

    /** Summary of the player's overall progress, shown in the middle of the bottom row. */
    private fun progressItem(): ItemStack {
        val item = ItemStack(Material.GOLDEN_HELMET)
        item.editMeta { meta ->
            meta.displayName(text("<gold>⚜ Jouw voortgang"))
            meta.lore(
                listOf(
                    text("<gray>Nog te ontdekken: <white>${questsFor(QuestTab.NOT_PLAYED).size}"),
                    text("<gray>Actief: <gold>${questsFor(QuestTab.ACTIVE).size}"),
                    text("<gray>Voltooid: <green>${questsFor(QuestTab.COMPLETED).size}"),
                )
            )
            // Hide the armor stats of the helmet, only the quest counts matter.
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
        }
        return item
    }

    private fun infoItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        item.editMeta { it.displayName(text(name)) }
        return item
    }

    private fun filler(): ItemStack {
        val item = ItemStack(Material.ORANGE_STAINED_GLASS_PANE)
        item.editMeta { it.displayName(Component.empty()) }
        return item
    }

    companion object {
        const val SIZE = 54
        const val QUEST_AREA_START = 9
        const val PER_PAGE = 36
        const val EMPTY_SLOT = 31
        const val BOTTOM_ROW_START = 45
        const val PREV_SLOT = 45
        const val PROGRESS_SLOT = 49
        const val NEXT_SLOT = 53
    }
}
