package com.typewritermc.basic.entries.action

import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.utils.UntickedAsync
import com.typewritermc.core.utils.launch
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger
import com.typewritermc.engine.paper.entry.entries.ConstVar
import com.typewritermc.engine.paper.entry.entries.Var
import com.typewritermc.engine.paper.utils.Sync
import kotlinx.coroutines.Dispatchers

@Entry("give_experience", "Give experience to a player.", Colors.RED, "material-symbols:magic-button-outline")

class GiveExperienceEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    val experience: Var<Int> = ConstVar(0),
    val levels: Var<Int> = ConstVar(0),
)  : ActionEntry {
    override fun ActionTrigger.execute() {
        val xp = experience.get(player)
        val levels = levels.get(player)
        Dispatchers.Sync.launch {
            player.giveExp(xp)
            player.giveExpLevels(levels)
        }
    }
}