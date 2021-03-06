package hlaaftana.kf.registers

import groovy.transform.CompileStatic
import hlaaftana.kf.CommandRegister
import hlaaftana.discordg.objects.Role

import static java.lang.System.currentTimeMillis as now

@CompileStatic
class GuildCommands extends CommandRegister {
	{ group = 'Guild' }
	
	static Map<? extends String, ? extends Closure> roleOptions = [
		color: { Role r ->
			!r.hoist && !r.permissions && r.color.RGB &&
				r.name ==~ /#?[A-Fa-f0-9]+/
		},
		unused: { Role r ->
			!r.memberIds
		},
		no_overwrites: { Role r ->
			!r.guild.channels*.overwrites.any { it.any { it.id == r.id } }
		},
		no_permissions: { Role r ->
			!r.permissions
		},
		color_ignore_perms: { Role r ->
			!r.hoist && r.color.RGB && r.name ==~ /#?[A-Fa-f0-9]+/
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
			roles.remove(guild.defaultRole)
			List<Closure> options = []
			if (arguments){
				boolean neg = false
				for (o in arguments.tokenize())
					if (o == '!') neg = true
					else {
						if (roleOptions[o])
							options.add(neg ? (Closure) { Role x -> !roleOptions[o](x) } : roleOptions[o])
						else {
							respond("Unknown filter: $o.\nList of filters: " +
								roleOptions.keySet().join(", "))
							return
						}
						if (neg) neg = false
					}
			} else { options = roleOptions.values() as List<Closure> }
			for (x in options) roles = roles.findAll(x)
			if (params.contains('-')){
				if (!member.permissions['manageRoles'])
					return respond('You dont have permissions')
				def a = respond("Deleting ${roles.size()} roles in about ${roles.size() / 2} seconds...")
				long s = now()
				if (roles) {
					for (r in roles.init()) {
						r.delete()
						Thread.sleep 500
					}
					roles.last().delete()
				}
				a.edit("Deleted all ${roles.size()} roles in ${(now() - s) / 1000} seconds.")
			} else respond("${roles.join(", ")}\n${roles.size()} total")
		}
	}
}
