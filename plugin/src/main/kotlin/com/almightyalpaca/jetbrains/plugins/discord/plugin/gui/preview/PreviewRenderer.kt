package com.almightyalpaca.jetbrains.plugins.discord.plugin.gui.preview

import com.almightyalpaca.jetbrains.plugins.discord.plugin.components.ApplicationComponent
import com.almightyalpaca.jetbrains.plugins.discord.plugin.rpc.RichPresence
import com.almightyalpaca.jetbrains.plugins.discord.plugin.rpc.RichPresenceService
import com.almightyalpaca.jetbrains.plugins.discord.plugin.rpc.renderer.RenderContext
import com.almightyalpaca.jetbrains.plugins.discord.plugin.rpc.renderer.Renderer
import com.almightyalpaca.jetbrains.plugins.discord.plugin.utils.*
import com.almightyalpaca.jetbrains.plugins.discord.plugin.utils.Color.blurple
import com.almightyalpaca.jetbrains.plugins.discord.plugin.utils.Color.darkOverlay
import com.almightyalpaca.jetbrains.plugins.discord.plugin.utils.Color.green
import com.almightyalpaca.jetbrains.plugins.discord.plugin.utils.Color.greenTranslucent
import com.almightyalpaca.jetbrains.plugins.discord.plugin.utils.Color.whiteTranslucent60
import com.almightyalpaca.jetbrains.plugins.discord.plugin.utils.Color.whiteTranslucent80
import org.apache.commons.lang3.time.DurationFormatUtils
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.image.BufferedImage
import java.time.Duration
import java.time.OffsetDateTime
import com.almightyalpaca.jetbrains.plugins.discord.plugin.rpc.User as RPCUser

class PreviewRenderer {
    private val user = User()
    private val game = Game()

    private val width = 250
    private val height = 273

    var type = Renderer.Type.APPLICATION

    private var image: BufferedImage = createImage(width, height)

    val dummy by lazy lazy@{
        val image = createImage(width, height)

        image.withGraphics {
            color = Color(0, 0, 0, 0)
            fillRect(0, 0, image.width, image.height)
        }

        return@lazy image
    }

    val font16Bold: Font = Roboto.bold.deriveFont(16F)
    val font16Regular: Font = Roboto.regular.deriveFont(16F)
    val font14Bold: Font = Roboto.bold.deriveFont(14F)
    val font14Medium: Font = Roboto.medium.deriveFont(13F)
    val font11Black: Font = Roboto.black.deriveFont(11F)

    val font16BoldMetrics: FontMetrics = image.graphics.getFontMetrics(font16Bold)
    val font16RegularMetrics: FontMetrics = image.graphics.getFontMetrics(font16Regular)
    val font14BoldMetrics: FontMetrics = image.graphics.getFontMetrics(font14Bold)
    val font14MediumMetrics: FontMetrics = image.graphics.getFontMetrics(font14Medium)
    val font11BlackMetrics: FontMetrics = image.graphics.getFontMetrics(font11Black)

    val font16BoldHeight: Int = font16BoldMetrics.height
    val font16RegularHeight: Int = font16RegularMetrics.ascent
    val font14BoldHeight: Int = font14BoldMetrics.height
    val font14MediumHeight: Int = font14MediumMetrics.height
    val font11BlackHeight: Int = font11BlackMetrics.height

    val font16BoldBaseline: Int = font16BoldMetrics.maxAscent + font16BoldMetrics.leading
    val font16RegularBaseline: Int = font16RegularMetrics.maxAscent + font16RegularMetrics.leading
    val font14BoldBaseline: Int = font14BoldMetrics.maxAscent + font14BoldMetrics.leading
    val font14MediumBaseline: Int = font14MediumMetrics.maxAscent + font14MediumMetrics.leading
    val font11BlackBaseline: Int = font11BlackMetrics.maxAscent + font11BlackMetrics.leading

    val font16BoldMaxHeight: Int = font16BoldMetrics.maxAscent + font16BoldMetrics.leading + font16BoldMetrics.maxDescent
    val font16RegularMaxHeight: Int = font16RegularMetrics.maxAscent + font16RegularMetrics.leading + font16RegularMetrics.maxDescent
    val font14BoldMaxHeight: Int = font14BoldMetrics.maxAscent + font14BoldMetrics.leading + font14BoldMetrics.maxDescent
    val font14MediumMaxHeight: Int = font14MediumMetrics.maxAscent + font14MediumMetrics.leading + font14MediumMetrics.maxDescent
    val font11BlackMaxHeight: Int = font11BlackMetrics.maxAscent + font11BlackMetrics.leading + font11BlackMetrics.maxDescent

    @Synchronized
    fun draw(force: Boolean = false): ModifiedImage {
        val component = ApplicationComponent.instance
        val context = RenderContext(component.source, component.data, Renderer.Mode.PREVIEW)
        val renderer = type.createRenderer(context)
        val presence = renderer.forceRender()

        val modified = user.draw(image, force) or game.draw(image, presence, force)

        return ModifiedImage(modified, image)
    }

    private inner class User {
        private var lastUser: RPCUser? = null
        private var lastAvatarEmpty = true

        fun draw(image: BufferedImage, force: Boolean): Boolean {
            val user = RichPresenceService.instance.user

            val avatar = when {
                user != lastUser -> {
                    lastAvatarEmpty = true
                    getAvatar(user, 90)
                }
                lastAvatarEmpty -> getAvatar(user, 90)
                else -> null
            }

            var modified = false
            if (force || user != lastUser || (lastAvatarEmpty && avatar != null)) {
                modified = true

                image.withGraphics {
                    color = blurple
                    fill(roundRectangle(0.0, 0.0, 250.0, image.height * 0.6, 10.0, 10.0))

                    withTranslation(10, 20) {
                        val mid = (image.width - 10) / 2

                        // Avatar
                        color = Color.red
                        drawImage(avatar, mid - 45, 0, null)

                        // Online indicator
                        color = blurple
                        fillArc(mid - 45 + 65, 65, 26, 26, 0, 360)

                        color = greenTranslucent
                        fillArc(mid - 45 + 68, 68, 20, 20, 0, 360)

                        color = green
                        fillArc(mid - 45 + 70, 70, 16, 16, 0, 360)

                        val name = user.name
                        val tag = user.tag?.let { tag -> "#" + tag.padStart(4, '0') } ?: ""
                        val nameWidth = font16BoldMetrics.stringWidth(name)
                        val tagWidth = font16RegularMetrics.stringWidth(tag)

                        val textWidth = nameWidth + tagWidth

                        color = Color.white
                        font = font16Bold
                        drawString(name, mid - textWidth / 2, 100 + font16BoldBaseline)

                        color = whiteTranslucent60
                        font = font16Regular
                        drawString(tag, mid - textWidth / 2 + nameWidth, 100 + font16BoldBaseline + (font16BoldBaseline - font16RegularBaseline) / 2)
                    }
                }

                if (avatar != null) {
                    lastAvatarEmpty = false
                }

                lastUser = user
            }

            return modified
        }
    }

    private inner class Game {
        private val images = Images()
        private val text = Text()

        var lastImagesEmpty: Boolean? = null
        var first = true

        fun draw(image: BufferedImage, presence: RichPresence, force: Boolean): Boolean {

            val (imagesModified, imagesEmpty) = images.draw(image, presence, force)

            if (force || first) {

                // "Playing a game"
                image.withGraphics {
                    val sectionStart = (image.height * 0.6).toInt()

                    color = blurple
                    fillRect(0, sectionStart, image.width, 10 + font11BlackHeight + 8)
                    color = darkOverlay
                    fillRect(0, sectionStart, image.width, 10 + font11BlackHeight + 8)

                    font = font11Black
                    color = Color.white
                    drawString("PLAYING A GAME", 10, sectionStart + 10 + font11BlackBaseline)
                }
            }

            if (force || first || lastImagesEmpty != imagesEmpty) {
                // IDE name
                image.withGraphics {
                    val sectionStart = (image.height * 0.6).toInt() + 10 + font11BlackHeight + 8
                    val indentation = when (imagesEmpty) {
                        true -> 7
                        false -> 77
                    }

                    color = blurple
                    fillRect(indentation, sectionStart, image.width - indentation, font14BoldMaxHeight)
                    color = darkOverlay
                    fillRect(indentation, sectionStart, image.width - indentation, font14BoldMaxHeight)

                    font = font14Bold
                    color = whiteTranslucent80
                    drawString(ApplicationComponent.instance.data.name, indentation + 3, sectionStart + font14BoldBaseline)
                }
            }

            val textModified = text.draw(image, presence, imagesEmpty, force)

            first = false
            lastImagesEmpty = imagesEmpty

            return imagesModified || textModified
        }

        private inner class Images {
            private var lastLarge: BufferedImage? = null
            private var lastLargeKey: String? = null
            private var lastSmall: BufferedImage? = null
            private var lastAppId: Long? = null

            fun draw(image: BufferedImage, presence: RichPresence, force: Boolean): Pair<Boolean, Boolean> {
                val largeKey = presence.largeImage?.key
                val smallKey = presence.smallImage?.key
                val appId = presence.appId

                if (force || lastLargeKey != largeKey || lastSmallKey != smallKey || lastAppId != appId) {
                    val large = if (lastLargeKey != largeKey || lastAppId != appId) {
                        presence.largeImage?.asset?.getImage(60)?.toScaledImage(60)?.withRoundedCorners(8.0)
                    } else {
                        lastLarge
                    }
                    val small = if (lastSmallKey != smallKey || lastAppId != appId) {
                        presence.smallImage?.asset?.getImage(20)?.toScaledImage(20)?.toRoundImage()
                    } else {
                        lastSmall
                    }

                    lastLarge = large
                    lastLargeKey = largeKey
                    lastSmall = small
                    lastSmallKey = smallKey
                    lastAppId = appId

                    image.withGraphics {
                        val sectionStart = (image.height * 0.6).toInt() + 10 + font11BlackHeight + 8
                        val width = when (large) {
                            null -> 8.0
                            else -> 78.0
                        }

                        color = blurple
                        fill(roundRectangle(0.0, sectionStart.toDouble(), width, (image.height - sectionStart).toDouble(), radiusBottomLeft = 10.0))
                        color = darkOverlay
                        fill(roundRectangle(0.0, sectionStart.toDouble(), width, (image.height - sectionStart).toDouble(), radiusBottomLeft = 10.0))

                        if (large != null) {
                            drawImage(large, 10, sectionStart, null)

                            if (small != null) {
                                color = blurple
                                fillArc(10 + 45 - 2, sectionStart + 45 - 2, 24, 24, 0, 360)
                                color = darkOverlay
                                fillArc(10 + 45 - 2, sectionStart + 45 - 2, 24, 24, 0, 360)

                                drawImage(small, 10 + 45, sectionStart + 45, null)
                            }

                            return true to false
                        }

                        return true to true
                    }

                }

                return false to (lastLarge == null)
            }

            private var lastSmallKey: String? = null
        }

        private inner class Text {
            private val details = Details()
            private val state = State()
            private val time = Time()

            var lastDetailsEmpty: Boolean? = null
            var lastStateEmpty: Boolean? = null

            fun draw(image: BufferedImage, presence: RichPresence, imagesEmpty: Boolean, force: Boolean): Boolean {
                val (detailsModified, detailsEmpty) = details.draw(image, presence, imagesEmpty, force)
                val (stateModified, stateEmpty) = state.draw(image, presence, imagesEmpty, detailsEmpty, force)
                val timeModified = time.draw(image, presence, imagesEmpty, detailsEmpty, stateEmpty, force)

                lastDetailsEmpty = detailsEmpty
                lastStateEmpty = stateEmpty

                return detailsModified || stateModified || timeModified
            }

            private inner class Details {
                var lastLine: String? = null

                fun draw(image: BufferedImage, presence: RichPresence, imagesEmpty: Boolean, force: Boolean): Pair<Boolean, Boolean> {
                    val line = presence.details

                    if (force || lastImagesEmpty != imagesEmpty || lastLine != line) {
                        lastLine = line

                        image.withGraphics {
                            val sectionStart = (image.height * 0.6).toInt() + 10 + font11BlackHeight + 8 + font14BoldMaxHeight
                            val indentation = when (imagesEmpty) {
                                true -> 7
                                false -> 77
                            }

                            color = blurple
                            fillRect(indentation, sectionStart, image.width - indentation, font14MediumMaxHeight)
                            color = darkOverlay
                            fillRect(indentation, sectionStart, image.width - indentation, font14MediumMaxHeight)

                            return if (line.isNullOrBlank()) {
                                true to true
                            } else {
                                val lineCut = line.limitStringWidth(font14MediumMetrics, image.width - (indentation + 3 + 10))

                                font = font14Medium
                                color = whiteTranslucent80
                                drawString(lineCut, indentation + 3, sectionStart + font14MediumBaseline)

                                true to false
                            }
                        }

                        return true to line.isNullOrBlank()
                    }

                    return false to lastLine.isNullOrBlank()
                }
            }

            private inner class State {
                var lastLine: String? = null

                fun draw(image: BufferedImage, presence: RichPresence, imagesEmpty: Boolean, detailsEmpty: Boolean, force: Boolean): Pair<Boolean, Boolean> {
                    val line = presence.state

                    if (force || lastImagesEmpty != imagesEmpty || lastDetailsEmpty != detailsEmpty || lastLine != line) {
                        lastLine = line

                        image.withGraphics {
                            var sectionStart = (image.height * 0.6).toInt() + 10 + font11BlackHeight + 8 + font14BoldMaxHeight
                            if (!detailsEmpty) {
                                sectionStart += font14MediumMaxHeight
                            }

                            val indentation = when (imagesEmpty) {
                                true -> 7
                                false -> 77
                            }

                            color = blurple
                            fillRect(indentation, sectionStart, image.width - indentation, font14MediumMaxHeight)
                            color = darkOverlay
                            fillRect(indentation, sectionStart, image.width - indentation, font14MediumMaxHeight)

                            return if (line.isNullOrBlank()) {
                                true to true
                            } else {
                                val lineCut = line.limitStringWidth(font14MediumMetrics, image.width - (indentation + 3 + 10))

                                font = font14Medium
                                color = whiteTranslucent80
                                drawString(lineCut, indentation + 3, sectionStart + font14MediumBaseline)

                                true to false
                            }
                        }
                    }

                    return true to line.isNullOrBlank()
                }
            }

            private inner class Time {
                var lastTime: OffsetDateTime? = null
                var lastTimeNow: OffsetDateTime? = null

                fun draw(image: BufferedImage, presence: RichPresence, imagesEmpty: Boolean, detailsEmpty: Boolean, stateEmpty: Boolean, force: Boolean): Boolean {
                    val time = presence.startTimestamp
                    val timeNow = OffsetDateTime.now()

                    if (force || lastTime != time || lastTimeNow != timeNow || lastImagesEmpty != imagesEmpty || lastDetailsEmpty != detailsEmpty || lastStateEmpty != stateEmpty) {
                        lastTime = time
                        lastTimeNow = timeNow

                        image.withGraphics {
                            var sectionStart = (image.height * 0.6).toInt() + 10 + font11BlackHeight + 8 + font14BoldMaxHeight
                            if (!detailsEmpty) {
                                sectionStart += font14MediumMaxHeight
                            }

                            if (!stateEmpty) {
                                sectionStart += font14MediumMaxHeight
                            }

                            val indentation = when (imagesEmpty) {
                                true -> 7.0
                                false -> 77.0
                            }

                            color = blurple
                            fill(roundRectangle(indentation, sectionStart.toDouble(), image.width - indentation, (image.height - sectionStart).toDouble(), radiusBottomRight = 10.0))
                            color = darkOverlay
                            fill(roundRectangle(indentation, sectionStart.toDouble(), image.width - indentation, (image.height - sectionStart).toDouble(), radiusBottomRight = 10.0))

                            if (time != null) {
                                val millis = Duration.between(time, timeNow).toMillis()
                                val formatted = DurationFormatUtils.formatDuration(millis, "HH:mm:ss")

                                font = font14Medium
                                color = whiteTranslucent80
                                drawString("$formatted elapsed", indentation.toInt() + 3, sectionStart + font14MediumBaseline)
                            }
                        }

                        return true
                    }

                    return false
                }
            }
        }
    }
}
