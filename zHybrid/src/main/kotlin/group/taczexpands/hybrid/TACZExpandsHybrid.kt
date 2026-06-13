package group.taczexpands.hybrid

import group.taczexpands.common.TACZExpandsCommon
import group.taczexpands.hybrid.network.NetworkManager
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import org.slf4j.Logger
import thedarkcolour.kotlinforforge.forge.MOD_BUS

class TACZExpandsHybrid {
    companion object {
        lateinit var INSTANCE: TACZExpandsHybrid
        lateinit var LOGGER: Logger
    }

    init {
        INSTANCE = this
        LOGGER = TACZExpandsCommon.LOGGER
        Bus.FORGE.bus().get().register(this)
        MOD_BUS.addListener(::onSetup)
    }

    fun onSetup(event: FMLCommonSetupEvent) {
        NetworkManager.init()
    }
}
