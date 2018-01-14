package hlaaftana.karmafields.registers

import groovy.transform.CompileStatic
import hlaaftana.karmafields.CommandRegister
import hlaaftana.karmafields.relics.MiscUtil
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.utils.PermissionUtil
import static java.lang.System.currentTimeMillis as now

@CompileStatic
class GuildCommands extends CommandRegister {
	{ group = 'Guild' }
	
	static Map<String, Closure<Boolean>> roleOptions = [
		color: { Role r ->
			!r.hoisted && !r.permissions && r.color.RGB &&
				r.name ==~ /#?[A-Fa-f0-9]+/
		},
		unused: { Role r ->
			!r.guild.getMembersWithRoles(r)
		},
		no_overwrites: { Role r ->
			!((r.guild.textChannels as List<Channel>) + (r.guild.voiceChannels as List<Channel>))*.rolePermissionOverrides
					.any { it.any { it.role.idLong == r.idLong } }
		},
		no_permissions: { Role r ->
			!r.permissions
		},
		color_ignore_perms: { Role r ->
			!r.hoisted && r.color.RGB && r.name ==~ /#?[A-Fa-f0-9]+/
		}
	]

	static {
		roleOptions.colour = roleOptions.color
		roleOptions.colour_ignore_perms = roleOptions.color_ignore_perms
	}

	def register(){
		command(['filterroles', 'roles',
			~/(?:filter)?roles([\-!]+)/],
			id: '23',
			description: 'Finds roles with specific filters. If no filters are given, all filters will be used.\n\n' +
				'List of filters: ' + roleOptions.keySet().join(', '),
			usages: [
				'': 'Uses all filters.',
				' (filter1) (filter2)...': 'Uses the given filters. Note: space separated.',
				'- ...': 'Removes the filtered roles.',
			],
			examples: [
				'',
				' color',
				' unused no_overwrites',
				' unused ! color'
			],
			guildOnly: true){
			def params = captures[0]?.toList() ?: []
			List<Role> roles = guild.roles
			roles.remove(guild.publicRole)
			List<Closure<Boolean>> options = []
			if (arguments){
				boolean neg = false
				for (o in arguments.tokenize())
					if (o == '!') neg = true
					else {
						if (roleOptions[o])
							options.add(neg ? { Role x -> !roleOptions[o](x) } : roleOptions[o])
						else {
							sendMessage("Unknown filter: $o.\nList of filters: " +
								roleOptions.keySet().join(", "))
							return
						}
						if (neg) neg = false
					}
			} else { options = roleOptions.values() as List<Closure<Boolean>> }
			for (x in options) roles = roles.findAll(x)
			if (params.contains('-')){
				if (!PermissionUtil.checkPermission(member, Permission.MANAGE_ROLES))
					return formatted('You don\'t have permissions.')
				def a = formatted(true, "Deleting ${roles.size()} roles in about ${roles.size() / 2} seconds...")
				long s = now()
				if (roles) {
					for (r in roles.dropRight(1)) {
						r.delete().queue()
						Thread.sleep 500
					}
					roles.last().delete().queue()
				}
				a.editMessage(MiscUtil.block("> Deleted all ${roles.size()} roles in ${(now() - s) / 1000} seconds.", 'accesslog'))
			} else formatted("${roles.join(", ")}\n${roles.size()} total")
		}
	}
}
