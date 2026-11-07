package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** A pair of glasses set up in Visor, as Visor last knew it. */
@Poko
@Serializable
public class GlassesPair @KitInternal constructor(
    public val serial: String,
    /** The name the person gave the pair, in Visor or in the Meta app. */
    public val name: String? = null,
    /** True for the pair Visor connects to now: the one every other method acts on. */
    public val active: Boolean,
    /** The link to this pair. Only the active pair connects; the others read [Link.DISCONNECTED]. */
    public val link: Link,
    /** Meta's product id of the model, such as 266 for Ray-Ban Meta (Gen 2), from the build the glasses last gave. */
    public val product: Int? = null,
    /** True for glasses with a display. Null until Visor has seen the pair connected. */
    public val display: Boolean? = null,
    /** How the pair looks, from Meta's record of its serial. Null until Visor has asked Meta. */
    public val look: GlassesLook? = null,
    /** The battery, from 0 to 100, when Visor last heard from the pair. */
    public val battery: Int? = null,
    /** The charging case's battery, from 0 to 100, at the same time. */
    public val caseBattery: Int? = null,
    /** When Visor last heard from the pair, in milliseconds since 1970. */
    public val lastSeen: Long? = null,
    /** The serial of the Neural Band paired with it, if one is. */
    public val band: String? = null,
)

/** A pair's frame, colour and lenses, as Meta records them for its serial: a code and the name to show for each. */
@Poko
@Serializable
public class GlassesLook @KitInternal constructor(
    public val style: String? = null,
    public val styleName: String? = null,
    public val color: String? = null,
    public val colorName: String? = null,
    public val lens: String? = null,
    public val lensName: String? = null,
    public val size: String? = null,
)

/** The params of [Methods.GLASSES_SELECT]. */
@Poko
@Serializable
public class PairSelection @KitInternal constructor(
    /** A [GlassesPair.serial]. */
    public val serial: String,
)
