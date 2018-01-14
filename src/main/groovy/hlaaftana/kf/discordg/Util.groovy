package hlaaftana.kf.discordg

import com.mashape.unirest.http.Unirest
import groovy.transform.CompileStatic
import hlaaftana.discordg.util.bot.CommandEventData
import hlaaftana.discordg.util.ConversionUtil
import hlaaftana.discordg.util.MiscUtil
import hlaaftana.discordg.objects.Message
import hlaaftana.discordg.objects.User

import javax.imageio.ImageIO
import java.awt.*
import java.awt.image.BufferedImage
import java.util.regex.Matcher

@CompileStatic
class Util {
	static String CHANNEL_ARG_REGEX = /<?#?([\d\w\-]+?)>?/
	
	private static Random colorRandom = new Random()

	static ByteArrayOutputStream draw(Map arguments = Collections.emptyMap(),
	                                  @DelegatesTo(Graphics2D) Closure closure) {
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

	static String formatLongUser(User user){ "\"$user.name\"#$user.discrim ($user.id)" }

	static String formatLongMessage(Message msg){
		def time = MiscUtil.dateToLDT(msg.createdAt)
		String.format("{%s|%s} [%s] <%s>: %s",
			time.toLocalDate(),
			time.toLocalTime(),
			msg.channel.dm ? 'DM' :
			msg.channel.group ? "group#$msg.channel" :
				"$msg.guild#$msg.channel",
			formatLongUser(msg.author), msg.content)
	}

	static String uploadToPuush(bytes, String filename = 'a'){
		Unirest.post('https://puush.me/api/up')
			.field('k', KarmaFields.creds.get('puush_api_key'))
			.field('z', filename)
			.field('f', ConversionUtil.getBytes(bytes), filename)
			.asString().body.tokenize(',')[1]
	}

	static resolveColor(CommandEventData d){
		int color
		String arg = d.arguments.trim().replace('#', '')
			.replaceAll(/\s+/, "").toLowerCase()
		Matcher matches
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
		else if (arg ==~ /\w+/) {
			if (!MiscUtil.namedColors.containsKey(arg))
				return 'Invalid named color. List here: "http://www.december.com/html/spec/colorsvg.html"'
			color = MiscUtil.namedColors[arg]
		} else if ((matches = (arg =~ /\d+/)).size() == 3) {
			int[] rgb = new int[3]
			int i = 0
			while (matches.find()) {
				rgb[i] = Integer.valueOf(matches.group())
				++i
			}
			color = (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]
		} else return 'Don\'t know how to parse that color.'
		color
	}
}