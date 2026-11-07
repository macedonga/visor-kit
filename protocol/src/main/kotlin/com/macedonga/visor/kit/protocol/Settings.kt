package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** What a setting holds. New kinds come with new releases: treat one you do not know as [OTHER]. */
@Serializable
@JvmInline
public value class SettingKind(public val id: String) {
    override fun toString(): String = id

    public companion object {
        /** "true" or "false". */
        public val BOOL: SettingKind = SettingKind("bool")

        /** A whole number, such as "3". */
        public val INT: SettingKind = SettingKind("int")

        /** A number with a fraction, such as "1.5". */
        public val FLOAT: SettingKind = SettingKind("float")

        public val STRING: SettingKind = SettingKind("string")

        /** An encoded value apps may read but not write. */
        public val OTHER: SettingKind = SettingKind("other")
    }
}

/** One value of a setting that is a choice, with what the Meta AI app calls it. */
@Poko
@Serializable
public class SettingOption @KitInternal constructor(
    public val value: String,
    public val label: String,
)

/** The bounds of a number setting. Either bound is null when the setting has none. */
@Poko
@Serializable
public class SettingRange @KitInternal constructor(
    public val min: Double? = null,
    public val max: Double? = null,
    /** The step between values, null for any. */
    public val step: Double? = null,
    /** Such as "s", "%" or "dB", empty for none. */
    public val unit: String = "",
)

/** One of the glasses' settings, with its value as text in the form [kind] gives. */
@Poko
@Serializable
public class GlassesSetting @KitInternal constructor(
    /** The glasses' own name for it, such as `sg_wake_word_enabled`. Stable across releases. */
    public val key: String,
    public val kind: SettingKind,
    /** The value in force: what was set, else the firmware's default. Null when Visor knows neither. */
    public val value: String? = null,
    /** The firmware's default, null when unknown. */
    public val default: String? = null,
    /** What the Meta AI app calls it, null for a setting it does not show. */
    public val label: String? = null,
    /** Visor's group for it, such as "Camera" or "Sound". */
    public val group: String? = null,
    /** For a choice, its values; empty otherwise. */
    public val options: List<SettingOption> = emptyList(),
    /** For a number, its bounds. */
    public val range: SettingRange? = null,
    /** False for a setting apps may only read: updates, resets, heat protection, developer and encoded settings. */
    public val writable: Boolean = false,
    /** True from a write until the glasses report the setting again. */
    public val pending: Boolean = false,
) {
    /** Whether [value] fits this setting's kind, options and range. It says nothing of [writable]. */
    public fun accepts(value: String): Boolean {
        if (options.isNotEmpty()) return options.any { it.value == value }
        val number = when (kind) {
            SettingKind.BOOL -> return value == "true" || value == "false"
            SettingKind.INT -> value.toLongOrNull()?.toDouble()
            SettingKind.FLOAT -> value.toDoubleOrNull()?.takeIf { it.isFinite() }
            SettingKind.STRING -> return true
            else -> return false
        } ?: return false
        val r = range ?: return true
        return (r.min == null || number >= r.min) && (r.max == null || number <= r.max)
    }
}

/** The params of [Methods.SETTINGS_LIST] and [Topics.SETTINGS_CHANGES]. */
@Poko
@Serializable
public class SettingsQuery @KitInternal constructor(
    /** Only these keys; all when empty. */
    public val keys: List<String> = emptyList(),
    /** Only this group; all when null. */
    public val group: String? = null,
)

/** The params of [Methods.SETTINGS_GET]. */
@Poko
@Serializable
public class SettingKey @KitInternal constructor(public val key: String)

/** The params of [Methods.SETTINGS_SET]. */
@Poko
@Serializable
public class SettingWrite @KitInternal constructor(
    public val key: String,
    /** In the form the setting's kind gives, such as "true", "3" or one of its options' values. */
    public val value: String,
)
