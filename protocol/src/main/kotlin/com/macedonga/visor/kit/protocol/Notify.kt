package com.macedonga.visor.kit.protocol

import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

/** The params of [Methods.NOTIFY_POST]. */
@Poko
@Serializable
public class Notice @KitInternal constructor(
    /** The app's own id for it: a second post with the same id replaces the first. */
    public val id: String,
    public val title: String,
    public val text: String,
)

/** The params of [Methods.NOTIFY_REMOVE]. */
@Poko
@Serializable
public class NoticeRemoval @KitInternal constructor(public val id: String)
