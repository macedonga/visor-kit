package com.macedonga.visor.kit

import android.content.Intent
import com.macedonga.visor.kit.protocol.KitJson
import com.macedonga.visor.kit.protocol.VoiceRequest
import kotlinx.serialization.SerializationException

/**
 * An app started by voice, as DAT's voice invocations start one: the wearer says a phrase an activity declares, no
 * running app holds it, and Visor starts that activity with the question. Only an app the person granted
 * [com.macedonga.visor.kit.protocol.Scope.VOICE] is started. Declare the activity and its phrases:
 *
 * ```
 * <activity android:name=".GarageActivity" android:exported="true">
 *     <intent-filter>
 *         <action android:name="com.macedonga.visor.kit.action.VOICE" />
 *         <category android:name="android.intent.category.DEFAULT" />
 *     </intent-filter>
 *     <meta-data android:name="com.macedonga.visor.kit.VOICE_PHRASES" android:resource="@array/garage_phrases" />
 * </activity>
 * ```
 *
 * Then, in `onCreate` and `onNewIntent`, answer within about 20 seconds:
 *
 * ```
 * VoiceLaunch.request(intent)?.let { request -> lifecycleScope.launch { visor.voice.reply(request, "The garage is closed.") } }
 * ```
 */
public object VoiceLaunch {
    /** The action of the activity Visor starts. */
    public const val ACTION: String = "com.macedonga.visor.kit.action.VOICE"

    /** The activity's meta-data naming its phrases: a string-array resource. */
    public const val PHRASES: String = "com.macedonga.visor.kit.VOICE_PHRASES"

    /** The extra that carries the [VoiceRequest], as JSON. */
    public const val EXTRA_REQUEST: String = "com.macedonga.visor.kit.extra.VOICE_REQUEST"

    /**
     * The question Visor started this activity for, or null. Another app could start the activity with a made-up
     * request, but Visor takes an answer only to a question it asked.
     */
    public fun request(intent: Intent?): VoiceRequest? {
        if (intent?.action != ACTION) return null
        val json = intent.getStringExtra(EXTRA_REQUEST) ?: return null
        return try {
            KitJson.decodeFromString(VoiceRequest.serializer(), json)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
