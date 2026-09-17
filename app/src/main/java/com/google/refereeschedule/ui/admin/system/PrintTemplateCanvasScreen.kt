package com.google.refereeschedule.ui.admin.system

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.refereeschedule.domain.model.*
import java.util.Collections
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintTemplateCanvasScreen(
    templateId: String?,
    isReadOnly: Boolean = false,
    viewModel: PrintTemplateViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var currentComponents by remember { mutableStateOf<List<PrintComponent>>(emptyList()) }
    var templateName by remember { mutableStateOf("") }
    var orientation by remember { mutableStateOf(PrintOrientation.Portrait) }
    var maxHeight by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }

    val cloudTemplate = remember(uiState.templates, templateId) {
        uiState.templates.find { it.id == templateId }
    }

    LaunchedEffect(cloudTemplate) {
        if (cloudTemplate != null && !isInitialized) {
            currentComponents = cloudTemplate.components
            templateName = cloudTemplate.name
            orientation = cloudTemplate.orientation
            maxHeight = cloudTemplate.maxHeightInches?.toString() ?: ""
            isInitialized = true
        }
    }

    var selectedComponentId by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            currentComponents = currentComponents + PrintComponent(
                type = PrintComponentType.Image,
                content = it.toString(),
                isFloating = true,
                width = 100,
                height = 100
            )
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            currentComponents = currentComponents + PrintComponent(
                type = PrintComponentType.Pdf,
                content = it.toString(),
                isFloating = true,
                width = 200,
                height = 50
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isReadOnly) "Preview: $templateName" else (if (templateId == null) "New Template Canvas" else "Editing: $templateName")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isReadOnly) {
                        Button(onClick = { 
                            val finalTemplate = (cloudTemplate ?: PrintTemplate()).copy(
                                id = templateId ?: "",
                                name = templateName, 
                                components = currentComponents,
                                orientation = orientation,
                                maxHeightInches = maxHeight.toFloatOrNull()
                            )
                            viewModel.updateTemplate(finalTemplate)
                            onNavigateBack()
                        }) {
                            Text("Save Cloud")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .background(Color.LightGray.copy(alpha = 0.2f))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LIVE RECEIPT PREVIEW", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    if (!isReadOnly) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Portrait", style = MaterialTheme.typography.labelSmall)
                            Switch(
                                checked = orientation == PrintOrientation.Landscape,
                                onCheckedChange = { orientation = if (it) PrintOrientation.Landscape else PrintOrientation.Portrait },
                                modifier = Modifier.scale(0.7f)
                            )
                            Text("Landscape", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val paperWidth = if (orientation == PrintOrientation.Portrait) 300.dp else 450.dp
                    val maxH = maxHeight.toFloatOrNull()
                    val totalHeight = if (maxH != null) (maxH * 100).dp else (if (orientation == PrintOrientation.Portrait) 500.dp else 300.dp)

                    Surface(
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(paperWidth, totalHeight)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                currentComponents.filter { !it.isFloating }.forEach { component ->
                                    PreviewComponent(
                                        component = component,
                                        isSelected = selectedComponentId == component.id,
                                        onClick = { if (!isReadOnly) selectedComponentId = component.id }
                                    )
                                }
                            }
                            
                            currentComponents.filter { it.isFloating }.forEach { component ->
                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(component.xOffset.dp.roundToPx(), component.yOffset.dp.roundToPx()) }
                                        .then(if (!isReadOnly) {
                                            Modifier.pointerInput(component.id) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val newX = component.xOffset + (dragAmount.x / density).roundToInt()
                                                    val newY = component.yOffset + (dragAmount.y / density).roundToInt()
                                                    currentComponents = currentComponents.map { 
                                                        if (it.id == component.id) it.copy(xOffset = newX, yOffset = newY) else it 
                                                    }
                                                }
                                            }
                                        } else Modifier)
                                        .wrapContentSize()
                                ) {
                                    PreviewComponent(
                                        component = component,
                                        isSelected = selectedComponentId == component.id,
                                        onClick = { if (!isReadOnly) selectedComponentId = component.id }
                                    )
                                }
                            }
                            
                            if (maxH != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Red.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }

            if (!isReadOnly) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Design Tools", style = MaterialTheme.typography.titleMedium)
                    
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text("Template Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = maxHeight,
                        onValueChange = { maxHeight = it },
                        label = { Text("Max Page Height (Inches)") },
                        placeholder = { Text("Optional limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    Text("Add Elements", style = MaterialTheme.typography.labelLarge)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AddElementButton(Icons.Rounded.Title, "Text") { 
                            currentComponents = currentComponents + PrintComponent(type = PrintComponentType.Text, content = "New Text", isFloating = true)
                        }
                        AddElementButton(Icons.Rounded.HorizontalRule, "Line") {
                            currentComponents = currentComponents + PrintComponent(type = PrintComponentType.Divider, isFloating = false)
                        }
                        AddElementButton(Icons.Rounded.QrCode, "Code") {
                            currentComponents = currentComponents + PrintComponent(type = PrintComponentType.Barcode, content = "123456789", isFloating = true)
                        }
                        AddElementButton(Icons.Rounded.Category, "Shape") {
                            currentComponents = currentComponents + PrintComponent(type = PrintComponentType.Shape, shapeType = ShapeType.Rectangle, isFloating = true)
                        }
                        AddElementButton(Icons.Rounded.Image, "Image") {
                            imagePicker.launch("image/*")
                        }
                        AddElementButton(Icons.Rounded.PictureAsPdf, "PDF") {
                            pdfPicker.launch("application/pdf")
                        }
                    }

                    HorizontalDivider()

                    val selected = currentComponents.find { it.id == selectedComponentId }
                    if (selected != null) {
                        ComponentEditor(
                            component = selected,
                            onUpdate = { updated ->
                                currentComponents = currentComponents.map { if (it.id == updated.id) updated else it }
                            },
                            onDelete = {
                                currentComponents = currentComponents.filter { it.id != selectedComponentId }
                                selectedComponentId = null
                            },
                            onMoveUp = {
                                val index = currentComponents.indexOf(selected)
                                if (index > 0) {
                                    val list = currentComponents.toMutableList()
                                    Collections.swap(list, index, index - 1)
                                    currentComponents = list
                                }
                            },
                            onMoveDown = {
                                val index = currentComponents.indexOf(selected)
                                if (index < currentComponents.size - 1) {
                                    val list = currentComponents.toMutableList()
                                    Collections.swap(list, index, index + 1)
                                    currentComponents = list
                                }
                            }
                        )
                    } else {
                        Text("Select an element on the receipt to edit its properties.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "PREVIEW MODE\n(View Only)",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewComponent(component: PrintComponent, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .then(if (!component.isFloating) Modifier.fillMaxWidth() else Modifier.wrapContentSize())
            .clickable { onClick() }
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .graphicsLayer { rotationZ = component.rotation }
    ) {
        when (component.type) {
            PrintComponentType.Text -> {
                Text(
                    text = component.content,
                    fontSize = component.fontSizeValue.sp,
                    fontFamily = when(component.fontFamily) {
                        "Serif" -> FontFamily.Serif
                        "Monospace" -> FontFamily.Monospace
                        "Cursive" -> FontFamily.Cursive
                        "SansSerif" -> FontFamily.SansSerif
                        else -> FontFamily.Default
                    },
                    fontWeight = if (component.isBold) FontWeight.Bold else FontWeight.Normal,
                    textDecoration = if (component.isUnderlined) TextDecoration.Underline else TextDecoration.None,
                    textAlign = when(component.alignment) {
                        "Left" -> TextAlign.Left
                        "Right" -> TextAlign.Right
                        else -> TextAlign.Center
                    },
                    modifier = if (!component.isFloating) Modifier.fillMaxWidth() else Modifier
                )
            }
            PrintComponentType.Divider -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Black)
            }
            PrintComponentType.Image -> {
                AsyncImage(
                    model = component.content,
                    contentDescription = null,
                    modifier = Modifier.size(component.width.dp, component.height.dp),
                    contentScale = ContentScale.Fit
                )
            }
            PrintComponentType.Pdf -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(Color.LightGray.copy(alpha = 0.5f)).padding(4.dp).widthIn(max = component.width.dp)
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = Color.Red)
                    Spacer(Modifier.width(4.dp))
                    Text("PDF Document", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            PrintComponentType.Barcode -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.wrapContentSize()) {
                    Icon(Icons.Rounded.QrCode, contentDescription = null, modifier = Modifier.size(64.dp))
                    Text(component.content, style = MaterialTheme.typography.labelSmall)
                }
            }
            PrintComponentType.Spacer -> {
                Spacer(Modifier.height(16.dp))
            }
            PrintComponentType.Shape -> {
                Box(modifier = Modifier.wrapContentSize().height(component.height.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(component.width.dp, component.height.dp)) {
                        val style = if (component.isOutlineOnly) Stroke(width = component.strokeWidth.dp.toPx()) else Fill
                        when (component.shapeType) {
                            ShapeType.Rectangle -> drawRect(color = Color.Black, style = style)
                            ShapeType.Circle -> drawCircle(color = Color.Black, style = style)
                            ShapeType.Line -> drawLine(color = Color.Black, start = Offset(0f, size.height/2), end = Offset(size.width, size.height/2), strokeWidth = component.strokeWidth.dp.toPx())
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddElementButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentEditor(
    component: PrintComponent,
    onUpdate: (PrintComponent) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Editing ${component.type}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onMoveUp) { Icon(Icons.Rounded.ArrowUpward, contentDescription = null) }
                IconButton(onClick = onMoveDown) { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
            }

            if (component.type == PrintComponentType.Text || component.type == PrintComponentType.Barcode || component.type == PrintComponentType.Image || component.type == PrintComponentType.Pdf) {
                OutlinedTextField(
                    value = component.content,
                    onValueChange = { onUpdate(component.copy(content = it)) },
                    label = { Text("Content / URL / Uri") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (component.type == PrintComponentType.Text || component.type == PrintComponentType.Barcode) {
                    Text("Available Placeholders (Tap to insert):", style = MaterialTheme.typography.labelSmall)
                    val placeholders = listOf(
                        "refereeName" to "Referee's full name",
                        "date" to "Today's date",
                        "orgName" to "Organization name",
                        "schedule" to "Summary of today's assignments",
                        "available" to "Summary of today's open slots",
                        "gameTitle" to "Match (Home v Away)",
                        "homeTeam" to "Home team name",
                        "awayTeam" to "Away team name",
                        "division" to "Division name",
                        "gameNumber" to "Game reference number",
                        "location" to "Location/Complex name",
                        "field" to "Field number",
                        "time" to "Kickoff time",
                        "voucherCode" to "Unique lunch voucher ID",
                        "expiryTime" to "Voucher expiration time"
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        placeholders.forEach { (tag, _) ->
                            AssistChip(
                                onClick = { 
                                    val newContent = if (component.content.isEmpty()) "{{$tag}}" else "${component.content} {{$tag}}"
                                    onUpdate(component.copy(content = newContent)) 
                                },
                                label = { Text("{{$tag}}", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    
                    var showHelp by remember { mutableStateOf(false) }
                    TextButton(onClick = { showHelp = !showHelp }) {
                        Text(if (showHelp) "Hide Placeholder Info" else "Show Placeholder Info", style = MaterialTheme.typography.labelSmall)
                    }
                    if (showHelp) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                placeholders.forEach { (tag, desc) ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("{{$tag}}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text(desc, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(2f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text("Rotation: ${component.rotation.toInt()}°", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = component.rotation,
                onValueChange = { onUpdate(component.copy(rotation = it)) },
                valueRange = 0f..360f,
                steps = 7,
                modifier = Modifier.fillMaxWidth()
            )

            if (component.type == PrintComponentType.Text) {
                Text("Font Family", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Default", "Serif", "Monospace", "Cursive", "SansSerif").forEach { font ->
                        FilterChip(
                            selected = component.fontFamily == font,
                            onClick = { onUpdate(component.copy(fontFamily = font)) },
                            label = { Text(font) }
                        )
                    }
                }

                Text("Alignment", style = MaterialTheme.typography.labelSmall)
                Row {
                    listOf("Left", "Center", "Right").forEach { align ->
                        FilterChip(
                            selected = component.alignment == align,
                            onClick = { onUpdate(component.copy(alignment = align)) },
                            label = { Text(align) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                Text("Font Size: ${component.fontSizeValue.toInt()}sp", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = component.fontSizeValue,
                    onValueChange = { onUpdate(component.copy(fontSizeValue = it)) },
                    valueRange = 8f..32f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = component.isBold, onCheckedChange = { onUpdate(component.copy(isBold = it)) })
                    Text("Bold", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(16.dp))
                    Checkbox(checked = component.isUnderlined, onCheckedChange = { onUpdate(component.copy(isUnderlined = it)) })
                    Text("Underline", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("Positioning", style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = component.isFloating, onCheckedChange = { onUpdate(component.copy(isFloating = it)) })
                Text("Floating (Overlay Mode)", style = MaterialTheme.typography.bodySmall)
            }

            if (component.isFloating) {
                Text("Manual Placement Active (Touch & Drag on receipt)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
            }

            if (component.type == PrintComponentType.Shape || component.type == PrintComponentType.Image || component.type == PrintComponentType.Pdf) {
                if (component.type == PrintComponentType.Shape) {
                    Text("Shape Style", style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = component.isOutlineOnly, onCheckedChange = { onUpdate(component.copy(isOutlineOnly = it)) })
                        Text("Outline Only (No Fill)", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Text("Stroke Thickness: ${component.strokeWidth.toInt()}", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = component.strokeWidth,
                        onValueChange = { onUpdate(component.copy(strokeWidth = it)) },
                        valueRange = 1f..10f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Shape Type", style = MaterialTheme.typography.labelSmall)
                    Row {
                        ShapeType.entries.filter { it != ShapeType.None }.forEach { shape ->
                            FilterChip(
                                selected = component.shapeType == shape,
                                onClick = { onUpdate(component.copy(shapeType = shape)) },
                                label = { Text(shape.name) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = component.width.toString(),
                        onValueChange = { val w = it.toIntOrNull() ?: 0; onUpdate(component.copy(width = w)) },
                        label = { Text("Width (dp)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = component.height.toString(),
                        onValueChange = { val h = it.toIntOrNull() ?: 0; onUpdate(component.copy(height = h)) },
                        label = { Text("Height (dp)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
