package com.google.refereeschedule.domain.model

import java.util.UUID

enum class PrintComponentType {
    Text, Divider, Image, Barcode, Spacer, Shape, Pdf
}

enum class ShapeType {
    None, Rectangle, Circle, Line
}

enum class PrintOrientation {
    Portrait, Landscape
}

data class PrintComponent(
    val id: String = UUID.randomUUID().toString(),
    val type: PrintComponentType = PrintComponentType.Text,
    val content: String = "", // Text or placeholder
    val alignment: String = "Center", // Left, Center, Right
    val fontSize: String = "Normal", // Legacy
    val fontSizeValue: Float = 14f,
    val fontFamily: String = "Default", // Default, Serif, Monospace, Cursive, SansSerif
    val isBold: Boolean = false,
    val isUnderlined: Boolean = false,
    val rotation: Float = 0f,
    val shapeType: ShapeType = ShapeType.None,
    val width: Int = 100,
    val height: Int = 50,
    val isOutlineOnly: Boolean = true,
    val strokeWidth: Float = 2f,
    val isFloating: Boolean = true, // Default to floating for free placement
    val xOffset: Int = 0,
    val yOffset: Int = 0
)

data class PrintTemplate(
    val id: String = "",
    val name: String = "",
    val type: PrintJobType = PrintJobType.LunchVoucher,
    val orientation: PrintOrientation = PrintOrientation.Portrait,
    val components: List<PrintComponent> = emptyList(),
    val isDefault: Boolean = false,
    val description: String = "",
    val maxHeightInches: Float? = null // Optional height limit in inches
)
