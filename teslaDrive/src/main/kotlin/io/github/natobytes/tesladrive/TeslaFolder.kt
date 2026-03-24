package io.github.natobytes.tesladrive

enum class TeslaFolder(
    val displayName: String,
    val path: String,
    val description: String,
) {
    TESLA_CAM("TeslaCam", "TeslaCam", "Dashcam recordings root folder"),
    SAVED_CLIPS("Saved Clips", "TeslaCam/SavedClips", "Manually saved dashcam clips"),
    SENTRY_CLIPS("Sentry Clips", "TeslaCam/SentryClips", "Sentry mode recordings"),
    RECENT_CLIPS("Recent Clips", "TeslaCam/RecentClips", "Recent unclassified recordings"),
    LIGHT_SHOW("Light Show", "LightShow", "Light show .fseq sequence files"),
    BOOMBOX("Boombox", "Boombox", "Custom audio files for boombox"),
    HOME_LINK("HomeLink", "HomeLink", "HomeLink configuration files"),
}
