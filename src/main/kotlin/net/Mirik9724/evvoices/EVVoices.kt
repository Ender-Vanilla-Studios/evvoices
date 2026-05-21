package net.Mirik9724.evvoices;

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import net.Mirik9724.api.copyFileFromJar
import net.Mirik9724.api.isAvailableNewVersion
import net.Mirik9724.api.loadYmlFile
import net.Mirik9724.api.tryCreatePath
import net.Mirik9724.api.updateYmlFromJar
import java.io.File

lateinit var conf: Map<String,String>
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
class EVVoices @Inject constructor(val logger: Logger, val server: ProxyServer) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        tryCreatePath(File("plugins/EVVoices"))

        copyFileFromJar("config.yml", "plugins/EVVoices", this.javaClass.classLoader)
        updateYmlFromJar("config.yml", "plugins/EVVoices/config.yml", this.javaClass.classLoader)

        conf = loadYmlFile("plugins/EVVoices/config.yml")

        if(conf["checkUpdates"] == "true") {
            if(isAvailableNewVersion("https://raw.githubusercontent.com/Mirik9724/EVVoices/refs/heads/master/v.txt", BuildConstants.VERSION)){
                logger.info("New version available")
            }
        }

        DSServer.start(
            token = conf["token"]!!
        )

        logger.info("Plugin ON")
    }
}
