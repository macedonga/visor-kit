package com.macedonga.visor.kit

import com.macedonga.visor.kit.protocol.GlassesSetting
import com.macedonga.visor.kit.protocol.Methods
import com.macedonga.visor.kit.protocol.Scope
import com.macedonga.visor.kit.protocol.SettingKey
import com.macedonga.visor.kit.protocol.SettingWrite
import com.macedonga.visor.kit.protocol.SettingsQuery
import com.macedonga.visor.kit.protocol.Topics
import kotlinx.coroutines.flow.Flow

/**
 * The glasses' settings, the ones the Meta AI app shows and the rest. Needs [Scope.SETTINGS]. Values are text in
 * the form each setting's kind gives: "true", "3", "1.5", or one of its options' values.
 */
public class Settings internal constructor(private val visor: Visor) {
    /** Every setting Visor knows, as last read from the glasses. */
    public suspend fun list(): List<GlassesSetting> = visor.call(Methods.SETTINGS_LIST, SettingsQuery())

    /** The settings of one [group], such as "Camera". */
    public suspend fun list(group: String): List<GlassesSetting> = visor.call(Methods.SETTINGS_LIST, SettingsQuery(group = group))

    /** One setting. Fails with reason `unknown_key` for a key Visor does not know. */
    public suspend fun get(key: String): GlassesSetting = visor.call(Methods.SETTINGS_GET, SettingKey(key))

    /**
     * Changes a setting and returns it pending: [changes] brings it again once the glasses report the new value.
     * Fails with reason `read_only` for a setting apps may not change, and `invalid_value` for a value it does not take.
     */
    public suspend fun set(key: String, value: String): GlassesSetting = visor.call(Methods.SETTINGS_SET, SettingWrite(key, value))

    /** Changes a "bool" setting. */
    public suspend fun set(key: String, value: Boolean): GlassesSetting = set(key, value.toString())

    /** Changes an "int" setting. */
    public suspend fun set(key: String, value: Int): GlassesSetting = set(key, value.toString())

    /** Each change of any setting, whoever made it: the app, Visor, or the wearer on the glasses. */
    public val changes: Flow<GlassesSetting>
        get() = visor.subscribe(Topics.SETTINGS_CHANGES, SettingsQuery())

    /** Each change of the settings with these [keys]. */
    public fun changes(keys: List<String>): Flow<GlassesSetting> = visor.subscribe(Topics.SETTINGS_CHANGES, SettingsQuery(keys = keys))
}
