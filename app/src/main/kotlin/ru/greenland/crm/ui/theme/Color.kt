package ru.greenland.crm.ui.theme

import androidx.compose.ui.graphics.Color

// Строго чёрно-белая палитра: без цветных акцентов, иерархия — через оттенки серого,
// вес шрифта и обводки.
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val Ink90 = Color(0xFF121212)
val GraySurfaceLight = Color(0xFFF2F2F2)
val GrayTextLight = Color(0xFF4B4B4B)
val GrayOutlineLight = Color(0xFFD6D6D6)
val GraySurfaceDark = Color(0xFF1E1E1E)
val GrayTextDark = Color(0xFFC7C7C7)
val GrayOutlineDark = Color(0xFF4A4A4A)

// M3 заполняет любую неуказанную роль (container'ы, tertiary, surfaceContainer*)
// цветами из дефолтной фиолетовой baseline-схемы — поэтому ниже расписаны все роли явно.
val ContainerLight = Color(0xFFE7E7E7)
val OnContainerLight = Color(0xFF1A1A1A)
val SurfaceDim = Color(0xFFDBDBDB)
val SurfaceBright = Color(0xFFFFFFFF)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF7F7F7)
val SurfaceContainerLight = Color(0xFFF0F0F0)
val SurfaceContainerHighLight = Color(0xFFE9E9E9)
val SurfaceContainerHighestLight = Color(0xFFE2E2E2)

val ContainerDark = Color(0xFF2E2E2E)
val OnContainerDark = Color(0xFFE6E6E6)
val SurfaceDimDark = Color(0xFF121212)
val SurfaceBrightDark = Color(0xFF3A3A3A)
val SurfaceContainerLowestDark = Color(0xFF0D0D0D)
val SurfaceContainerLowDark = Color(0xFF191919)
val SurfaceContainerDark = Color(0xFF1D1D1D)
val SurfaceContainerHighDark = Color(0xFF272727)
val SurfaceContainerHighestDark = Color(0xFF323232)
