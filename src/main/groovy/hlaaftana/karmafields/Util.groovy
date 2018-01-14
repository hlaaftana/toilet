package hlaaftana.karmafields

import com.mashape.unirest.http.Unirest
import groovy.transform.CompileStatic
import hlaaftana.karmafields.relics.CommandEventData
import hlaaftana.karmafields.relics.ConversionUtil
import hlaaftana.karmafields.relics.MiscUtil
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User

import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.time.OffsetDateTime

class Util {
	static String CHANNEL_ARG_REGEX = /<?#?([\d\w\-]+?)>?/
	
	private static Random colorRandom = new Random()

	@CompileStatic
	static ByteArrayOutputStream draw(Map arguments = [:], @DelegatesTo(Graphics2D) Closure closure){
		BufferedImage image = new BufferedImage((arguments.width as Integer) ?: 256,
			(arguments['height'] as Integer) ?: 256, (arguments.colorType as Integer) ?:
			BufferedImage.TYPE_INT_RGB)
		Graphics2D graphics = image.createGraphics()
		Closure ass = (Closure) closure.clone()
		ass.delegate = graphics
		ass.resolveStrategy = Closure.DELEGATE_FIRST
		ass(image)
		graphics.dispose()
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		ImageIO.write(image, (arguments.imageType?.toString()) ?: 'png', baos)
		baos
	}

	static String formatLongUser(User user){ "\"$user.name\"#$user.discriminator ($user.id)" }

	@CompileStatic
	static String formatLongMessage(Message msg){
		OffsetDateTime time = msg.creationTime
		String.format("{%s|%s} [%s] <%s>: %s",
			time.toLocalDate(),
			time.toLocalTime(),
			msg.channelType == ChannelType.PRIVATE ? 'DM' :
			msg.channelType == ChannelType.GROUP ? "group#$msg.channel" :
				"$msg.guild#$msg.channel",
			formatLongUser(msg.author), msg.content)
	}

	@CompileStatic
	static String uploadToPuush(bytes, String filename = 'a'){
		Unirest.post('https://puush.me/api/up')
			.field('k', KarmaFields.creds.get('puush_api_key'))
			.field('z', filename)
			.field('f', ConversionUtil.getBytes(bytes), filename)
			.asString().body.tokenize(',')[1]
	}

	@CompileStatic
	static resolveColor(CommandEventData d){
		int color
		String arg = d.arguments.toString().trim().replace('#', '')
			.replaceAll(/\s+/, "").toLowerCase()
		if (arg.toLowerCase() == 'random')
			color = colorRandom.nextInt(0xFFFFFF)
		else if (arg.toLowerCase() in ['me', 'my', 'mine'])
			color = d.member.roles.findAll { it.color.RGB }
					.sort { it.position }[-1]?.color?.RGB ?: 0
		else if (arg ==~ /[0-9a-fA-F]+/)
			try {
				color = Integer.parseInt(arg, 16)
			} catch (NumberFormatException ignored) {
				return 'Invalid hexadecimal number. Probably too large.'
			}
		else if (arg ==~ /(?:rgb\()?[0-9]+,[0-9]+,[0-9]+(?:\))?/){
			int[] rgb = arg.findAll(/\d+/).collect { it.toInteger() }
			color = (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]
		}else if (arg ==~ /\w+/){
			if (!MiscUtil.namedColors.containsKey(arg))
				return 'Invalid named color. List here: "http://www.december.com/html/spec/colorsvg.html"'
			color = MiscUtil.namedColors[arg]
		} else return 'What'
		color
	}
}