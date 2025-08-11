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

@Entry("remove_experience", "Remove experience of a player.", Colors.RED, "material-symbols:magic-button-outline")

class RemoveExperienceEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    val experience: Var<Int> = ConstVar(0),
    val levels: Var<Int> = ConstVar(0),
)  : ActionEntry {
    override fun ActionTrigger.execute() {
        val xp = (player.totalExperience - experience.get(player)).coerceAtLeast(0)
        val level = (player.level - levels.get(player)).coerceAtLeast(0)

        Dispatchers.Sync.launch {
            player.totalExperience = xp
            player.level = level
        }
    }
}