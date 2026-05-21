package net.Mirik9724.evvoices;

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import org.slf4j.Logger

@Plugin(
    id = "evvoices",
    name = "EVVoices",
    version = BuildConstants.VERSION,
    description = "s",
    url = "https://github.com/Ender-Vanilla-Studios/evvoices",
    authors = ["Mirik9724"],
    dependencies = [
        Dependency(id = "mirikapi")
    ]
)
class EVVoices @Inject constructor(val logger: Logger) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
    }
}
