package com.resurface.app.data.monitored

/**
 * The kind of scrolling surface an app exposes. F4 detection branches its heuristic
 * on this: a feed emits `TYPE_VIEW_SCROLLED`; short-video pagers may emit little
 * (NOTES §8.3). [MIXED] apps host both (Instagram Reels, YouTube Shorts).
 */
enum class SurfaceType { FEED, SHORT_VIDEO, MIXED }
