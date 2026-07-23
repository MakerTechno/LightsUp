package nowebsite.makertechno.lights_up;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import nowebsite.makertechno.lights_up.client.render.SpotlightPipelines;
import nowebsite.makertechno.lights_up.common.init.LUBlockReg;
import nowebsite.makertechno.lights_up.common.init.LUCreativeTabs;
import nowebsite.makertechno.lights_up.common.init.LUItemReg;
import org.slf4j.Logger;

@Mod(LightsUp.MOD_ID)
public class LightsUp {
    public static final String MOD_ID = "lights_up";
    private static final Logger LOGGER = LogUtils.getLogger();

    public LightsUp(IEventBus modEventBus) {
        LUItemReg.touch();
        LUBlockReg.touch();
        LUCreativeTabs.touch();

        LUBlockReg.BLOCKS.register(modEventBus);
        LUItemReg.ITEMS.register(modEventBus);
        LUBlockReg.BLOCK_ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(SpotlightPipelines::register);
        LUCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(LUCreativeTabs::registerCreativeTabs);
    }
}