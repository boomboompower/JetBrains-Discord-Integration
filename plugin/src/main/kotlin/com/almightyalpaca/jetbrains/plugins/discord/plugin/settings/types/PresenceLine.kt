package com.almightyalpaca.jetbrains.plugins.discord.plugin.settings.types

import com.almightyalpaca.jetbrains.plugins.discord.plugin.rpc.renderer.RenderContext
import com.almightyalpaca.jetbrains.plugins.discord.plugin.settings.options.types.SimpleValue
import com.almightyalpaca.jetbrains.plugins.discord.plugin.settings.settings

typealias LineValue = SimpleValue<PresenceLine>

enum class PresenceLine(val description: String) {
    NONE("Empty") {
        override fun get(context: RenderContext) = Result.Empty
    },
    PROJECT_DESCRIPTION("Project Description") {
        override fun get(context: RenderContext) = context.run { project?.platformProject?.settings?.description?.getValue().toResult() }
    },
    PROJECT_NAME("Project Name") {
        override fun get(context: RenderContext) = context.project?.name.toResult()
    },
    PROJECT_NAME_DESCRIPTION("Project Name - Description") {
        override fun get(context: RenderContext): Result {
            val project = context.project ?: return Result.Empty

            return when (val description = context.run { project.platformProject.settings.description.getValue() }) {
                "" -> project.name.toResult()
                else -> "${project.name} - $description".toResult()
            }
        }
    },
    FILE_NAME("File Name") {
        override fun get(context: RenderContext) = context.file?.name.toResult()
    },
    CUSTOM("Custom") {
        override fun get(context: RenderContext) = Result.Custom
    };

    abstract fun get(context: RenderContext): Result

    override fun toString() = description

    companion object {
        val Application1 = NONE to arrayOf(NONE, CUSTOM)
        val Application2 = NONE to arrayOf(NONE, CUSTOM)
        val Project1 = PROJECT_NAME to arrayOf(NONE, PROJECT_DESCRIPTION, PROJECT_NAME, PROJECT_NAME_DESCRIPTION, CUSTOM)
        val Project2 = PROJECT_DESCRIPTION to arrayOf(NONE, PROJECT_DESCRIPTION, PROJECT_NAME, PROJECT_NAME_DESCRIPTION, CUSTOM)
        val File1 = PROJECT_NAME_DESCRIPTION to arrayOf(NONE, PROJECT_DESCRIPTION, PROJECT_NAME, PROJECT_NAME_DESCRIPTION, FILE_NAME, CUSTOM)
        val File2 = FILE_NAME to arrayOf(NONE, PROJECT_DESCRIPTION, PROJECT_NAME, PROJECT_NAME_DESCRIPTION, FILE_NAME, CUSTOM)
    }

    fun String?.toResult() = when (this) {
        null -> Result.Empty
        else -> Result.String(this)
    }

    sealed class Result {
        object Empty : Result()
        object Custom : Result()
        data class String(val value: kotlin.String) : Result()
    }
}