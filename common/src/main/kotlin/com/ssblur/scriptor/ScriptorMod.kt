package com.ssblur.scriptor

import com.google.common.base.Suppliers
import com.mojang.brigadier.CommandDispatcher
import com.ssblur.scriptor.advancement.ScriptorAdvancements
import com.ssblur.scriptor.block.ScriptorBlocks
import com.ssblur.scriptor.blockentity.ScriptorBlockEntities
import com.ssblur.scriptor.commands.DebugCommand
import com.ssblur.scriptor.commands.DumpDictionaryCommand
import com.ssblur.scriptor.commands.DumpWordCommand
import com.ssblur.scriptor.config.ScriptorGameRules
import com.ssblur.scriptor.data.components.ScriptorDataComponents
import com.ssblur.scriptor.effect.ScriptorEffects
import com.ssblur.scriptor.entity.ScriptorEntities
import com.ssblur.scriptor.events.ScriptorEvents
import com.ssblur.scriptor.feature.ScriptorFeatures
import com.ssblur.scriptor.item.ScriptorItems
import com.ssblur.scriptor.item.ScriptorLoot
import com.ssblur.scriptor.item.ScriptorTabs
import com.ssblur.scriptor.particle.ScriptorParticles
import com.ssblur.scriptor.recipe.ScriptorRecipes
import com.ssblur.scriptor.trade.ScriptorTrades
import com.ssblur.unfocused.ModInitializer
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.registry.registries.RegistrarManager
import net.minecraft.commands.CommandSourceStack
import org.apache.logging.log4j.LogManager
import java.util.function.Supplier

@Suppress("unused")
object ScriptorMod: ModInitializer("scriptor") {
    const val MOD_ID = "scriptor"
    val LOGGER = LogManager.getLogger(MOD_ID)!!
    val REGISTRIES: Supplier<RegistrarManager> = Suppliers.memoize { RegistrarManager.get(MOD_ID) }

    // Please don't mess with this, I'm not adding anticheat but it's no fun );
    var COMMUNITY_MODE = false

    fun registerCommands() {
        CommandRegistrationEvent.EVENT.register(dev.architectury.event.events.common.CommandRegistrationEvent { dispatcher: CommandDispatcher<CommandSourceStack?>?, registry: net.minecraft.commands.CommandBuildContext?, selection: net.minecraft.commands.Commands.CommandSelection? ->
            DumpDictionaryCommand.register(
                dispatcher,
                registry,
                selection
            )
        })
        CommandRegistrationEvent.EVENT.register(dev.architectury.event.events.common.CommandRegistrationEvent { dispatcher: CommandDispatcher<CommandSourceStack?>?, registry: net.minecraft.commands.CommandBuildContext?, selection: net.minecraft.commands.Commands.CommandSelection? ->
            DumpWordCommand.register(
                dispatcher,
                registry,
                selection
            )
        })
        CommandRegistrationEvent.EVENT.register(dev.architectury.event.events.common.CommandRegistrationEvent { dispatcher: CommandDispatcher<CommandSourceStack?>?, ignoredRegistry: net.minecraft.commands.CommandBuildContext?, ignoredSelection: net.minecraft.commands.Commands.CommandSelection? ->
            DebugCommand.register(
                dispatcher,
                ignoredRegistry,
                ignoredSelection
            )
        })
    }

    fun init() {
        ScriptorTabs.register()
        ScriptorAdvancements.register()
        ScriptorBlocks.register()
        ScriptorBlockEntities.register()
        ScriptorItems.register()
        ScriptorEntities.register()
        ScriptorEvents.register()
        ScriptorEffects.register()
        ScriptorTrades.register()
        ScriptorRecipes.register()
        ScriptorParticles.register()
        ScriptorLoot.register()
        ScriptorGameRules.register()
        ScriptorFeatures.register()
        ScriptorDataComponents.register()

        registerCommands()
    }
}