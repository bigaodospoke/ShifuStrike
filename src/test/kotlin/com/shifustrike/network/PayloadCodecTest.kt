package com.shifustrike.network

import com.shifustrike.core.Box
import com.shifustrike.core.DefaultShop
import com.shifustrike.core.Phase
import com.shifustrike.core.Team
import com.shifustrike.core.WinReason
import com.shifustrike.match.KillInfo
import io.netty.buffer.Unpooled
import net.minecraft.core.BlockPos
import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import kotlin.test.Test
import kotlin.test.assertEquals

class PayloadCodecTest {
    private fun <T> idaEVolta(codec: StreamCodec<RegistryFriendlyByteBuf, T>, v: T): T {
        val buf = RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY)
        codec.encode(buf, v)
        return codec.decode(buf)
    }

    private fun estado(bomba: BlockPos?) = MatchStatePayload(
        phase = Phase.PLANTED, round = 3, ctScore = 2, trScore = 1, timeLeftTicks = 1234, buyTicksLeft = 0,
        ctAlive = 4, trAlive = 3, money = 3250, armor = 88, helmet = true, alive = true, inBuyZone = false,
        bombPlanted = bomba != null, bombPos = bomba, carryingC4 = false, spectating = "",
    )

    @Test
    fun `estado com e sem bomba`() {
        assertEquals(estado(null), idaEVolta(MatchStatePayload.CODEC, estado(null)))
        assertEquals(estado(BlockPos(1, 2, 3)), idaEVolta(MatchStatePayload.CODEC, estado(BlockPos(1, 2, 3))))
    }

    @Test
    fun `info da partida com a loja inteira`() {
        val p = MatchInfoPayload(Team.TR, DefaultShop.entries.map { ShopEntryDto.from(it) }, Box(1, 2, 3, 4, 5, 6), Box(-1, -2, -3, 0, 0, 0), 2.0)
        assertEquals(p, idaEVolta(MatchInfoPayload.CODEC, p))
    }

    @Test
    fun `feed de kills`() {
        val k = KillFeedPayload(KillInfo("A", Team.CT, "B", Team.TR, "AK-47", true))
        assertEquals(k, idaEVolta(KillFeedPayload.CODEC, k))
        val sozinho = KillFeedPayload(KillInfo(null, null, "B", Team.TR, "", false))
        assertEquals(sozinho, idaEVolta(KillFeedPayload.CODEC, sozinho))
    }

    @Test
    fun `fim de rodada e de partida`() {
        val r = RoundEndPayload(Team.CT, WinReason.BOMB_DEFUSED, 3, 2)
        assertEquals(r, idaEVolta(RoundEndPayload.CODEC, r))
        val f = MatchEndPayload(Team.TR, 3, 5, listOf(ScoreLine("A", Team.CT, 4, 6), ScoreLine("B", Team.TR, 9, 1)))
        assertEquals(f, idaEVolta(MatchEndPayload.CODEC, f))
        val cancelada = MatchEndPayload(null, 0, 0, emptyList())
        assertEquals(cancelada, idaEVolta(MatchEndPayload.CODEC, cancelada))
    }

    @Test
    fun `pedidos do cliente`() {
        assertEquals(BuyPayload("ak47"), idaEVolta(BuyPayload.CODEC, BuyPayload("ak47")))
        assertEquals(RefundPayload("colete"), idaEVolta(RefundPayload.CODEC, RefundPayload("colete")))
        assertEquals(SpectateCyclePayload, idaEVolta(SpectateCyclePayload.CODEC, SpectateCyclePayload))
        assertEquals(MatchClearPayload, idaEVolta(MatchClearPayload.CODEC, MatchClearPayload))
    }
}
