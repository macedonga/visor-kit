package com.macedonga.visor.kit.testing

import com.macedonga.visor.kit.KitException
import com.macedonga.visor.kit.protocol.ErrorCode
import com.macedonga.visor.kit.protocol.GlassesSetting
import com.macedonga.visor.kit.protocol.SettingKind
import com.macedonga.visor.kit.protocol.SettingOption
import com.macedonga.visor.kit.protocol.SettingRange
import com.macedonga.visor.kit.protocol.SettingsQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update

/**
 * A few of the glasses' settings, for an app that uses [com.macedonga.visor.kit.Settings]. A write settles at once:
 * the app gets the setting pending, and the settled one on its changes right after.
 */
public class FakeSettings internal constructor() {
    private val _all = MutableStateFlow(SAMPLES.associateBy { it.key })

    /** Every fake setting, by key. */
    public val all: StateFlow<Map<String, GlassesSetting>> = _all.asStateFlow()

    private val changed = MutableSharedFlow<GlassesSetting>(extraBufferCapacity = 64)

    /** Sets [key] to [value] as the wearer would on the glasses, writable or not. */
    public fun change(key: String, value: String) {
        val setting = find(key)
        if (!setting.accepts(value)) throw IllegalArgumentException("$key does not take $value")
        settle(setting.valued(value, pending = false))
    }

    internal fun list(query: SettingsQuery): List<GlassesSetting> = _all.value.values.filter { it.matches(query) }

    internal fun get(key: String): GlassesSetting = find(key)

    internal fun set(key: String, value: String): GlassesSetting {
        val setting = find(key)
        if (!setting.writable) throw KitException(ErrorCode.REFUSED, "Apps may not change $key.", reason = "read_only")
        if (!setting.accepts(value)) throw KitException(ErrorCode.INVALID_PARAMS, "$key does not take $value.", reason = "invalid_value")
        val pending = setting.valued(value, pending = true)
        changed.tryEmit(pending)
        settle(setting.valued(value, pending = false))
        return pending
    }

    internal fun changes(query: SettingsQuery): Flow<GlassesSetting> = changed.filter { it.matches(query) }

    private fun settle(setting: GlassesSetting) {
        _all.update { it + (setting.key to setting) }
        changed.tryEmit(setting)
    }

    private fun find(key: String): GlassesSetting =
        _all.value[key] ?: throw KitException(ErrorCode.INVALID_PARAMS, "No setting $key.", reason = "unknown_key")

    private fun GlassesSetting.matches(query: SettingsQuery) =
        (query.keys.isEmpty() || key in query.keys) && (query.group == null || group == query.group)

    private fun GlassesSetting.valued(value: String, pending: Boolean) =
        GlassesSetting(key, kind, value, default, label, group, options, range, writable, pending)

    private companion object {
        // Real keys, labels and defaults, from Visor's catalogue.
        val SAMPLES = listOf(
            GlassesSetting("wakeword_enabled", SettingKind.BOOL, "true", "true", "\"Hey Meta\"", "Meta AI", writable = true),
            GlassesSetting("head_nod_controls_enabled", SettingKind.BOOL, "false", "false", "Head nod controls", "Touch and gestures", writable = true),
            GlassesSetting(
                "notification_volume", SettingKind.INT, "90", "90", "Notification volume", "Sound",
                range = SettingRange(min = 55.0, max = 127.0, step = 1.0), writable = true,
            ),
            GlassesSetting(
                "resolution_and_framerate", SettingKind.INT, "0", "0", "Resolution and frame rate", "Camera",
                options = listOf(
                    SettingOption("0", "Default"),
                    SettingOption("1", "High resolution"),
                    SettingOption("2", "High frame rate"),
                ),
                writable = true,
            ),
            GlassesSetting("cloud_ota_enabled", SettingKind.BOOL, "false", "false", group = "Updates and system"),
        )
    }
}
