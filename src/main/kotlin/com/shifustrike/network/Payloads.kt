package com.shifustrike.network

import com.shifustrike.ShifuStrike
import com.shifustrike.core.Box
import com.shifustrike.core.EntryKind
import com.shifustrike.core.Phase
import com.shifustrike.core.ShopEntry
import com.shifustrike.core.Team
import com.shifustrike.core.WinReason
import com.shifustrike.match.KillInfo
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

private fun <T : CustomPacketPayload> type(path: String) = CustomPacketPayload.Type<T>(ShifuStrike.id(path))

private fun <T> RegistryFriendlyByteBuf.writeEntries(list: List<T>, writer: (RegistryFriendlyByteBuf, T) -> Unit) {
    writeVarInt(list.size)
    list.forEach { writer(this, it) }
}

private fun <T> RegistryFriendlyByteBuf.readEntries(reader: (RegistryFriendlyByteBuf) -> T): List<T> = List(readVarInt()) { reader(this) }

private fun RegistryFriendlyByteBuf.writeBox(b: Box) {
    writeVarInt(b.minX); writeVarInt(b.minY); writeVarInt(b.minZ); writeVarInt(b.maxX); writeVarInt(b.maxY); writeVarInt(b.maxZ)
}

private fun RegistryFriendlyByteBuf.readBox() = Box(readVarInt(), readVarInt(), readVarInt(), readVarInt(), readVarInt(), readVarInt())

/** -1 = nenhum time. */
private fun RegistryFriendlyByteBuf.writeTeam(t: Team?) = writeByte(t?.ordinal ?: -1)
private fun RegistryFriendlyByteBuf.readTeam(): Team? = readByte().toInt().let { if (it < 0) null else Team.entries[it] }

// ---------------------------------------------------------------- servidor → cliente

/** Um quadrado da tela de compra, sem o que só o servidor precisa (item, munição). */
data class ShopEntryDto(
    val chave: String,
    val coluna: Int,
    val posicao: Int,
    val nome: String,
    val preco: Int,
    val precoAlt: Int?,
    val tipo: EntryKind,
    val icone: String,
) {
    companion object {
        fun from(e: ShopEntry) = ShopEntryDto(e.chave, e.coluna, e.posicao, e.nome, e.preco, e.precoAlt, e.tipo, e.icone)
    }
}

/** No início da partida (e quando o jogador volta): time, loja do time e os bombsites. */
data class MatchInfoPayload(val team: Team, val shop: List<ShopEntryDto>, val siteA: Box, val siteB: Box, val defuseRadius: Double) :
    CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val TYPE = type<MatchInfoPayload>("info")
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, MatchInfoPayload> = StreamCodec.of(
            { buf, p ->
                buf.writeTeam(p.team)
                buf.writeEntries(p.shop) { b, e ->
                    b.writeUtf(e.chave); b.writeVarInt(e.coluna); b.writeVarInt(e.posicao); b.writeUtf(e.nome); b.writeVarInt(e.preco)
                    b.writeVarInt(e.precoAlt ?: -1); b.writeVarInt(e.tipo.ordinal); b.writeUtf(e.icone)
                }
                buf.writeBox(p.siteA); buf.writeBox(p.siteB); buf.writeDouble(p.defuseRadius)
            },
            { buf ->
                MatchInfoPayload(
                    buf.readTeam()!!,
                    buf.readEntries {
                        ShopEntryDto(
                            it.readUtf(), it.readVarInt(), it.readVarInt(), it.readUtf(), it.readVarInt(),
                            it.readVarInt().takeIf { v -> v >= 0 }, EntryKind.entries[it.readVarInt()], it.readUtf(),
                        )
                    },
                    buf.readBox(), buf.readBox(), buf.readDouble(),
                )
            },
        )
    }
}

/** Estado da partida para o HUD e a tela de compra, a cada meio segundo. `spectating` = quem o morto está assistindo. */
data class MatchStatePayload(
    val phase: Phase,
    val round: Int,
    val ctScore: Int,
    val trScore: Int,
    val timeLeftTicks: Int,
    val buyTicksLeft: Int,
    val ctAlive: Int,
    val trAlive: Int,
    val money: Int,
    val armor: Int,
    val helmet: Boolean,
    val alive: Boolean,
    val inBuyZone: Boolean,
    val bombPlanted: Boolean,
    val bombPos: BlockPos?,
    val carryingC4: Boolean,
    val spectating: String,
) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val TYPE = type<MatchStatePayload>("estado")
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, MatchStatePayload> = StreamCodec.of(
            { buf, p ->
                buf.writeVarInt(p.phase.ordinal); buf.writeVarInt(p.round); buf.writeVarInt(p.ctScore); buf.writeVarInt(p.trScore)
                buf.writeVarInt(p.timeLeftTicks); buf.writeVarInt(p.buyTicksLeft); buf.writeVarInt(p.ctAlive); buf.writeVarInt(p.trAlive)
                buf.writeVarInt(p.money); buf.writeVarInt(p.armor); buf.writeBoolean(p.helmet); buf.writeBoolean(p.alive)
                buf.writeBoolean(p.inBuyZone); buf.writeBoolean(p.bombPlanted)
                buf.writeBoolean(p.bombPos != null)
                p.bombPos?.let { buf.writeBlockPos(it) }
                buf.writeBoolean(p.carryingC4); buf.writeUtf(p.spectating)
            },
            { buf ->
                MatchStatePayload(
                    Phase.entries[buf.readVarInt()], buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(),
                    buf.readBoolean(), buf.readBoolean(),
                    if (buf.readBoolean()) buf.readBlockPos() else null,
                    buf.readBoolean(), buf.readUtf(),
                )
            },
        )
    }
}

data class KillFeedPayload(val info: KillInfo) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val TYPE = type<KillFeedPayload>("kill")
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, KillFeedPayload> = StreamCodec.of(
            { buf, p ->
                val k = p.info
                buf.writeUtf(k.killer ?: ""); buf.writeTeam(k.killerTeam); buf.writeUtf(k.victim); buf.writeTeam(k.victimTeam)
                buf.writeUtf(k.weapon); buf.writeBoolean(k.headshot)
            },
            { buf ->
                KillFeedPayload(
                    KillInfo(buf.readUtf().ifEmpty { null }, buf.readTeam(), buf.readUtf(), buf.readTeam()!!, buf.readUtf(), buf.readBoolean()),
                )
            },
        )
    }
}

data class RoundEndPayload(val winner: Team, val reason: WinReason, val ctScore: Int, val trScore: Int) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val TYPE = type<RoundEndPayload>("fim_rodada")
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, RoundEndPayload> = StreamCodec.of(
            { buf, p -> buf.writeTeam(p.winner); buf.writeVarInt(p.reason.ordinal); buf.writeVarInt(p.ctScore); buf.writeVarInt(p.trScore) },
            { buf -> RoundEndPayload(buf.readTeam()!!, WinReason.entries[buf.readVarInt()], buf.readVarInt(), buf.readVarInt()) },
        )
    }
}

data class ScoreLine(val name: String, val team: Team, val kills: Int, val deaths: Int)

/** Fim da partida; `winner` null = cancelada. */
data class MatchEndPayload(val winner: Team?, val ctScore: Int, val trScore: Int, val lines: List<ScoreLine>) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val TYPE = type<MatchEndPayload>("fim_partida")
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, MatchEndPayload> = StreamCodec.of(
            { buf, p ->
                buf.writeTeam(p.winner); buf.writeVarInt(p.ctScore); buf.writeVarInt(p.trScore)
                buf.writeEntries(p.lines) { b, l -> b.writeUtf(l.name); b.writeTeam(l.team); b.writeVarInt(l.kills); b.writeVarInt(l.deaths) }
            },
            { buf ->
                MatchEndPayload(
                    buf.readTeam(), buf.readVarInt(), buf.readVarInt(),
                    buf.readEntries { ScoreLine(it.readUtf(), it.readTeam()!!, it.readVarInt(), it.readVarInt()) },
                )
            },
        )
    }
}

/** O jogador saiu da partida: o cliente apaga HUD e loja. */
data object MatchClearPayload : CustomPacketPayload {
    val TYPE = type<MatchClearPayload>("limpar")
    val CODEC: StreamCodec<RegistryFriendlyByteBuf, MatchClearPayload> = StreamCodec.unit(MatchClearPayload)
    override fun type() = TYPE
}

// ---------------------------------------------------------------- cliente → servidor

data class BuyPayload(val chave: String) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val TYPE = type<BuyPayload>("comprar")
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, BuyPayload> =
            StreamCodec.of({ buf, p -> buf.writeUtf(p.chave, 64) }, { buf -> BuyPayload(buf.readUtf(64)) })
    }
}

data class RefundPayload(val chave: String) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val TYPE = type<RefundPayload>("reembolsar")
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, RefundPayload> =
            StreamCodec.of({ buf, p -> buf.writeUtf(p.chave, 64) }, { buf -> RefundPayload(buf.readUtf(64)) })
    }
}

/** Morto pede para assistir o próximo colega. */
data object SpectateCyclePayload : CustomPacketPayload {
    val TYPE = type<SpectateCyclePayload>("assistir_proximo")
    val CODEC: StreamCodec<RegistryFriendlyByteBuf, SpectateCyclePayload> = StreamCodec.unit(SpectateCyclePayload)
    override fun type() = TYPE
}
