package com.ssblur.scriptor.word

import com.ssblur.scriptor.ScriptorMod.LOGGER
import com.ssblur.scriptor.advancement.ScriptorAdvancements
import com.ssblur.scriptor.api.word.Descriptor
import com.ssblur.scriptor.api.word.Subject
import com.ssblur.scriptor.api.word.Word
import com.ssblur.scriptor.api.word.Word.COSTTYPE
import com.ssblur.scriptor.effect.ScriptorEffects.MUTE
import com.ssblur.scriptor.events.network.client.ParticleNetwork
import com.ssblur.scriptor.helpers.targetable.EntityTargetable
import com.ssblur.scriptor.helpers.targetable.Targetable
import com.ssblur.scriptor.word.descriptor.AfterCastDescriptor
import com.ssblur.scriptor.word.descriptor.CastDescriptor
import com.ssblur.scriptor.word.descriptor.focus.FocusDescriptor
import com.ssblur.scriptor.word.descriptor.target.TargetDescriptor
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import kotlin.math.pow

/**
 * A record used to represent a complete spell.
 * @param subject The target of a spell
 * @param spells Groups of descriptors and actions
 */
class Spell(
  @JvmField val subject: Subject,
  @JvmField vararg val spells: PartialSpell
) {
    fun castOnTargets(caster: Targetable?, targets: List<Targetable?>) {
        var caster = caster
        var targets = targets
        assert(spells.size >= 1)
        for (descriptor in spells[0].deduplicatedDescriptors()) {
            if (descriptor is TargetDescriptor) targets = descriptor.modifyTargets(targets, caster)
            if (descriptor is FocusDescriptor) caster = descriptor.modifyFocus(caster)
        }

        for (target in targets) {
            for (spell in spells) spell.action.apply(caster, target, spells[0].deduplicatedDescriptors())
        }
    }

    fun createFuture(caster: Targetable?): CompletableFuture<List<Targetable?>> {
        val targetFuture = CompletableFuture<List<Targetable?>>()

        targetFuture.whenComplete { targets: List<Targetable?>, throwable: Throwable? ->
            if (throwable == null) castOnTargets(caster, targets)
        }

        return targetFuture
    }

    /**
     * Casts this spell.
     * @param caster The entity which cast this spell.
     */
    fun cast(caster: Targetable) {
        var entity: Entity? = null
        var caster = caster
        if(caster is EntityTargetable) {
            entity = caster.targetEntity
            if (entity is LivingEntity)
                if (entity.hasEffect(MUTE)) {
                    if (entity is Player) entity.sendSystemMessage(Component.translatable("extra.scriptor.mute"))
                    return
                }
        }

        assert(spells.size >= 1)
        for (descriptor in spells[0].deduplicatedDescriptors()) {
            if (descriptor is CastDescriptor) if (descriptor.cannotCast(caster)) {
                if (entity is Player) {
                    entity.sendSystemMessage(Component.translatable("extra.scriptor.condition_not_met"))
                    ScriptorAdvancements.FIZZLE.get().trigger(entity as ServerPlayer)
                }
                if (!caster.level.isClientSide) ParticleNetwork.fizzle(caster.level, caster.targetBlockPos)
                return
            }
            if (descriptor is FocusDescriptor) caster = descriptor.modifyFocus(caster)
        }

        val targetFuture = subject.getTargets(caster, this)

        for (descriptor in spells[0].deduplicatedDescriptors()) if (descriptor is AfterCastDescriptor) descriptor.afterCast(
            caster
        )

        val finalCaster = caster.finalTargetable
        if (targetFuture.isDone) {
            try {
                val targets = targetFuture.get()
                castOnTargets(finalCaster, targets)
            } catch (e: InterruptedException) {
                LOGGER.error(e)
            } catch (e: ExecutionException) {
                LOGGER.error(e)
            }
        } else {
            targetFuture.whenComplete { targets: List<Targetable?>, throwable: Throwable? ->
                if (throwable == null) castOnTargets(finalCaster, targets)
            }
        }
    }

    /**
     * Casts this spell with a specified list of Targetables.
     * @param caster The entity which cast this spell.
     */
    fun cast(caster: Targetable?, vararg targetables: Targetable?) {
        castOnTargets(caster, Arrays.stream(targetables).toList())
    }

    /**
     * The cost for this spell, generally affects cooldowns / material cost.
     * @return A number representing cost.
     */
    fun cost(): Double {
        var sum = 0.0
        var scalar = 1.0
        var discount = 0.0

        var subCount = 0.0
        var sub = 0.0

        for (d in words()) {
            val cost = d!!.cost()
            when (cost.type) {
                COSTTYPE.ADDITIVE -> {
                    if (sum < 0) {
                        subCount++
                        sub += cost.cost
                    } else {
                        sum += cost.cost
                    }
                }

                COSTTYPE.MULTIPLICATIVE -> scalar *= cost.cost
                COSTTYPE.ADDITIVE_POST -> discount += cost.cost
            }
        }

        if (subCount > 0) {
            val squeeze = 0.5
            var denominator: Double = squeeze.pow(subCount)
            denominator = squeeze - denominator
            denominator = 1 - denominator
            sub = sub / denominator
            sum -= sub
        }

        var out = sum * scalar
        out += discount
        return out
    }


    private fun words(): Array<Word?> {
        var length = 1
        var index = 1
        for (spell in spells) length += spell.deduplicatedDescriptors().size + 1

        val words = arrayOfNulls<Word>(length)
        words[0] = subject

        for (spell in spells) {
            words[index] = spell.action
            index++

            val descriptors = spell.deduplicatedDescriptors()
            System.arraycopy(descriptors, 0, words, index, descriptors.size)
            index += descriptors.size
        }
        return words
    }

    fun deduplicatedDescriptorsForSubjects(): Array<Descriptor> {
        assert(spells.size >= 1)
        return spells[0].deduplicatedDescriptors()
    }

    fun deduplicatedDescriptorsForAccumulation(): Array<Descriptor?> {
        var length = 0
        var index = 0

        for (spell in spells) length += spell.deduplicatedDescriptors().size

        val descriptors = arrayOfNulls<Descriptor>(length)

        for (spell in spells) {
            System.arraycopy(spell.deduplicatedDescriptors(), 0, descriptors, index, descriptors.size)
            index += descriptors.size
        }

        return descriptors
    }
}
