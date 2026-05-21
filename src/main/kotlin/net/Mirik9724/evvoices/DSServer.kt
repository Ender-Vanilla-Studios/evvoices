package net.Mirik9724.evvoices

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.EnumSet

object DSServer {

    private var masterChannelId: Long = 0
    private var categoryId: Long = 0
    private var vipRoleId: Long = 0

    /**
     * Main entry point. Accepts only the bot token.
     */
    fun start(token: String) {
        this.masterChannelId = conf["masterChannelId"]!!.toLong()
        this.categoryId = conf["categoryId"]!!.toLong()
        this.vipRoleId = conf["vipRoleId"]!!.toLong()

        val jda = JDABuilder.createDefault(token)
            .enableIntents(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MEMBERS)
            .addEventListeners(VoiceChannelsController(), CommandsController())
            .build()

        // Registration of slash commands from global conf
        jda.updateCommands().addCommands(
            Commands.slash("limit", conf["cmdLimitDesc"]!!)
                .addOption(OptionType.INTEGER, "quantity", conf["cmdLimitArgDesc"]!!, true),

            Commands.slash("rename", conf["cmdRenameDesc"]!!)
                .addOption(OptionType.STRING, "name", conf["cmdRenameArgDesc"]!!, true)
        ).queue()
    }

    /**
     * Voice channels creation and deletion logic
     */
    private class VoiceChannelsController : ListenerAdapter() {
        override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
            val joinedChannel = event.channelJoined as? VoiceChannel
            val leftChannel = event.channelLeft as? VoiceChannel
            val member = event.member
            val guild = event.guild

            // Channel auto-creation logic
            if (joinedChannel != null && joinedChannel.idLong == masterChannelId) {
                val prefix = conf["channelPrefix"]!!
                guild.getCategoryById(categoryId)?.createVoiceChannel("$prefix${member.effectiveName}")?.queue { newChannel ->
                    newChannel.manager.putMemberPermissionOverride(
                        member.idLong,
                        EnumSet.of(Permission.MANAGE_CHANNEL),
                        null
                    ).queue {
                        guild.moveVoiceMember(member, newChannel).queue()
                    }
                }
            }

            // Channel auto-deletion logic
            if (leftChannel != null) {
                if (leftChannel.parentCategoryIdLong == categoryId && leftChannel.idLong != masterChannelId) {
                    if (leftChannel.members.isEmpty()) {
                        leftChannel.delete().queue()
                    }
                }
            }
        }
    }

    /**
     * Slash commands controller
     */
    private class CommandsController : ListenerAdapter() {
        override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
            val member = event.member ?: return
            val voiceState = member.voiceState ?: return
            val currentChannel = voiceState.channel as? VoiceChannel

            if (currentChannel == null || currentChannel.parentCategoryIdLong != categoryId || currentChannel.idLong == masterChannelId) {
                event.reply(conf["errNotInChannel"]!!).setEphemeral(true).queue()
                return
            }

            val isOwner = currentChannel.getPermissionOverride(member)?.allowed?.contains(Permission.MANAGE_CHANNEL) == true

            when (event.name) {
                "limit" -> {
                    if (!isOwner) {
                        event.reply(conf["errLimitNotOwner"]!!).setEphemeral(true).queue()
                        return
                    }

                    val limit = event.getOption("quantity")?.asInt ?: 0
                    if (limit !in 0..99) {
                        event.reply(conf["errLimitInvalidRange"]!!).setEphemeral(true).queue()
                        return
                    }

                    currentChannel.manager.setUserLimit(limit).queue {
                        val message = if (limit == 0) {
                            conf["msgLimitRemoved"]!!
                        } else {
                            conf["msgLimitChanged"]!!.replace("%limit%", limit.toString())
                        }
                        event.reply(message).setEphemeral(true).queue()
                    }
                }

                "rename" -> {
                    val isVip = member.roles.any { it.idLong == vipRoleId }

                    if (!isOwner && !isVip) {
                        event.reply(conf["errRenameNoPerms"]!!).setEphemeral(true).queue()
                        return
                    }

                    val newName = event.getOption("name")?.asString ?: "Voice Channel"
                    if (newName.isBlank() || newName.length > 32) {
                        event.reply(conf["errRenameInvalidLength"]!!).setEphemeral(true).queue()
                        return
                    }

                    val prefix = conf["channelPrefix"]!!
                    currentChannel.manager.setName("$prefix$newName").queue {
                        val message = conf["msgRenameSuccess"]!!.replace("%name%", newName)
                        event.reply(message).setEphemeral(true).queue()
                    }
                }
            }
        }
    }
}
