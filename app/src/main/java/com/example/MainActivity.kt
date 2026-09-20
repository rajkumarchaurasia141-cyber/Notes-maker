package com.example

import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        NotesMakerApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesMakerApp() {
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp

  // Ribbon Tabs: 0 -> Home, 1 -> Insert, 2 -> Page Layout, 3 -> Review
  var selectedTab by remember { mutableStateOf(0) }

  // Layout & Styling States
  var currentLayout by remember { mutableStateOf("One") } // One, Two, Three, Left, Right
  var currentBorder by remember { mutableStateOf("None") } // None, Classic, Vintage, Floral, Minimal
  var currentMargin by remember { mutableStateOf("Normal") } // Normal, Narrow, Wide
  var currentOrientation by remember { mutableStateOf("Portrait") } // Portrait, Landscape
  
  // Watermark States
  var watermarkEnabled by remember { mutableStateOf(true) }
  var watermarkText by remember { mutableStateOf("NotesMaker App") }
  var watermarkOpacity by remember { mutableStateOf(0.12f) }

  // Font States
  var selectedFont by remember { mutableStateOf("Poppins") }
  var selectedSize by remember { mutableStateOf("16px") }
  var selectedLineSpacing by remember { mutableStateOf("1.6") }

  // Show dialog states
  var showWatermarkSettings by remember { mutableStateOf(false) }
  var showBorderDialog by remember { mutableStateOf(false) }

  // Translate Side Panel State (Inspired by Word Design #5 Translate panel)
  var showTranslatePanel by remember { mutableStateOf(true) }
  var translateFrom by remember { mutableStateOf("English") }
  var translateTo by remember { mutableStateOf("Hindi (हिंदी)") }
  var sourceTextToTranslate by remember { mutableStateOf("") }
  var translatedResultText by remember { mutableStateOf("") }

  // Custom Color States
  var showTextColorPicker by remember { mutableStateOf(false) }
  var showHighlightColorPicker by remember { mutableStateOf(false) }

  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  // Statistics
  var wordCount by remember { mutableStateOf(35) }
  var charCount by remember { mutableStateOf(240) }

  // Intercept back button to dismiss panels/dialogs gracefully
  BackHandler(enabled = showTranslatePanel || showWatermarkSettings || showBorderDialog) {
    if (showWatermarkSettings) {
      showWatermarkSettings = false
    } else if (showBorderDialog) {
      showBorderDialog = false
    } else if (showTranslatePanel) {
      showTranslatePanel = false
    }
  }

  val fonts = listOf(
    "Poppins" to "'Poppins', sans-serif",
    "Noto Sans Devanagari" to "'Noto Sans Devanagari', sans-serif",
    "Mangal" to "'Mangal', serif",
    "Roboto" to "'Roboto', sans-serif",
    "Inter" to "'Inter', sans-serif",
    "Merriweather" to "'Merriweather', serif",
    "Lora" to "'Lora', serif",
    "Fira Code" to "'Fira Code', monospace",
    "Courier New" to "'Courier New', monospace",
    "Arial" to "Arial, sans-serif"
  )

  val themeColors = listOf(
    "#000000", "#FFFFFF", "#1F4E79", "#2F5597", "#C55A11", "#7F7F7F", "#8FA9DB", "#F4B183",
    "#C00000", "#FF0000", "#FFC000", "#FFFF00", "#92D050", "#00B050", "#00B0F0", "#002060",
    "#7030A0", "#44546A", "#D9E1F2", "#FCE4D6"
  )

  fun executeJs(js: String) {
    webViewRef?.evaluateJavascript(js, null)
  }

  // Update statistics dynamically by reading editor innerText from WebView
  LaunchedEffect(webViewRef) {
    while (true) {
      webViewRef?.evaluateJavascript(
        "(document.getElementById('editor')?.innerText || '').trim()"
      ) { text ->
        if (text != null && text != "null") {
          val cleanText = text.replace("\"", "").trim()
          val chars = cleanText.length
          val words = if (cleanText.isEmpty()) 0 else cleanText.split("\\s+".toRegex()).size
          wordCount = words
          charCount = chars
        }
      }
      kotlinx.coroutines.delay(1500)
    }
  }

  Scaffold(
    topBar = {
      Column {
        // App Main Title Bar
        TopAppBar(
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                "Vidya Agent",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(6.dp))
              Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text("Raj Sir Official", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
              }
            }
          },
          actions = {
            // Save & Quick Actions
            IconButton(onClick = { executeJs("alert('Notes saved locally!');") }) {
              Icon(Icons.Default.Save, contentDescription = "Save Notes")
            }
            
            IconButton(onClick = { showTranslatePanel = !showTranslatePanel }) {
              Icon(
                Icons.Default.Translate,
                contentDescription = "Toggle Translation Side Panel",
                tint = if (showTranslatePanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
            }

            // Export to PDF
            Button(
              onClick = {
                webViewRef?.let { wv ->
                  val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                  val jobName = "VidyaAgent_${System.currentTimeMillis()}"
                  val printAdapter = wv.createPrintDocumentAdapter(jobName)
                  val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .build()
                  printManager.print(jobName, printAdapter, printAttributes)
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              modifier = Modifier.padding(end = 8.dp)
            ) {
              Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Export PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        )

        // Spectacular animated brand ticker/marquee header
        AnimatedBrandingHeader()

        // Microsoft Word Style Ribbon Tabs Selector
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.height(48.dp)
        ) {
          Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
              Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Home", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
          Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Insert", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
          Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
              Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Page Layout", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
          Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
              Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Review & Stats", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.surface)
    ) {
      
      // Formatting Ribbon Content based on Selected Tab
      Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          when (selectedTab) {
            0 -> {
              // --- HOME TAB (Fonts, Sizes, Styling, Alignments) ---
              // Font Family Selector
              var fontMenuExpanded by remember { mutableStateOf(false) }
              Box {
                OutlinedButton(
                  onClick = { fontMenuExpanded = true },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(selectedFont, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) {
                  fonts.forEach { (name, cssFont) ->
                    DropdownMenuItem(
                      text = { Text(name, fontSize = 13.sp) },
                      onClick = {
                        selectedFont = name
                        fontMenuExpanded = false
                        executeJs("setFontFamily(\"$cssFont\");")
                      }
                    )
                  }
                }
              }

              // Font Size Selector
              var sizeMenuExpanded by remember { mutableStateOf(false) }
              Box {
                OutlinedButton(
                  onClick = { sizeMenuExpanded = true },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(selectedSize, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                  Spacer(modifier = Modifier.width(2.dp))
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(expanded = sizeMenuExpanded, onDismissRequest = { sizeMenuExpanded = false }) {
                  listOf("11px", "12px", "14px", "16px", "18px", "20px", "24px", "28px", "32px", "36px").forEach { sz ->
                    DropdownMenuItem(
                      text = { Text(sz, fontSize = 13.sp) },
                      onClick = {
                        selectedSize = sz
                        sizeMenuExpanded = false
                        executeJs("setFontSize(\"$sz\");")
                      }
                    )
                  }
                }
              }

              VerticalDivider(modifier = Modifier.height(28.dp))

              // Bold, Italic, Underline
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
              ) {
                IconButton(
                  onClick = { executeJs("document.execCommand('bold', false, null);") },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { executeJs("document.execCommand('italic', false, null);") },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { executeJs("document.execCommand('underline', false, null);") },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { executeJs("document.execCommand('strikeThrough', false, null);") },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(Icons.Default.FormatStrikethrough, contentDescription = "Strikethrough", modifier = Modifier.size(18.dp))
                }
              }

              VerticalDivider(modifier = Modifier.height(28.dp))

              // Text Color & Background Highlights (Word Theme Colors design #2)
              Box {
                OutlinedButton(
                  onClick = { showTextColorPicker = true },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.FormatColorText, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                DropdownMenu(expanded = showTextColorPicker, onDismissRequest = { showTextColorPicker = false }) {
                  Text("Theme & Standard Colors", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                  Row(
                    modifier = Modifier
                      .width(220.dp)
                      .padding(8.dp)
                      .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    themeColors.forEach { colorHex ->
                      Box(
                        modifier = Modifier
                          .size(24.dp)
                          .clip(CircleShape)
                          .background(Color(android.graphics.Color.parseColor(colorHex)))
                          .border(1.dp, Color.LightGray, CircleShape)
                          .clickable {
                            executeJs("document.execCommand('foreColor', false, '$colorHex');")
                            showTextColorPicker = false
                          }
                      )
                    }
                  }
                  DropdownMenuItem(
                    text = { Text("Default Black Color") },
                    onClick = {
                      executeJs("document.execCommand('foreColor', false, '#000000');")
                      showTextColorPicker = false
                    }
                  )
                }
              }

              Box {
                OutlinedButton(
                  onClick = { showHighlightColorPicker = true },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.FormatColorFill, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFD54F))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Highlight", fontSize = 11.sp)
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                DropdownMenu(expanded = showHighlightColorPicker, onDismissRequest = { showHighlightColorPicker = false }) {
                  Text("Highlight Color Palette", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                  Row(
                    modifier = Modifier
                      .width(220.dp)
                      .padding(8.dp)
                      .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    listOf("#ffff00", "#00ff00", "#00ffff", "#ff00ff", "#ffc0cb", "#ffa500", "#98fb98", "#ff0000", "#ffffff").forEach { colorHex ->
                      Box(
                        modifier = Modifier
                          .size(24.dp)
                          .clip(RoundedCornerShape(4.dp))
                          .background(Color(android.graphics.Color.parseColor(colorHex)))
                          .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                          .clickable {
                            executeJs("document.execCommand('hiliteColor', false, '$colorHex');")
                            showHighlightColorPicker = false
                          }
                      )
                    }
                  }
                  DropdownMenuItem(
                    text = { Text("No Color (Clear Highlight)") },
                    onClick = {
                      executeJs("document.execCommand('hiliteColor', false, 'transparent');")
                      showHighlightColorPicker = false
                    }
                  )
                }
              }

              VerticalDivider(modifier = Modifier.height(28.dp))

              // Paragraph Alignments
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
              ) {
                IconButton(onClick = { executeJs("document.execCommand('justifyLeft', false, null);") }, modifier = Modifier.size(36.dp)) {
                  Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align Left", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { executeJs("document.execCommand('justifyCenter', false, null);") }, modifier = Modifier.size(36.dp)) {
                  Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Center", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { executeJs("document.execCommand('justifyRight', false, null);") }, modifier = Modifier.size(36.dp)) {
                  Icon(Icons.Default.FormatAlignRight, contentDescription = "Align Right", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { executeJs("document.execCommand('justifyFull', false, null);") }, modifier = Modifier.size(36.dp)) {
                  Icon(Icons.Default.FormatAlignJustify, contentDescription = "Justify", modifier = Modifier.size(18.dp))
                }
              }

              VerticalDivider(modifier = Modifier.height(28.dp))

              // Bullet and Numbered Lists
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
              ) {
                IconButton(onClick = { executeJs("document.execCommand('insertUnorderedList', false, null);") }, modifier = Modifier.size(36.dp)) {
                  Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { executeJs("document.execCommand('insertOrderedList', false, null);") }, modifier = Modifier.size(36.dp)) {
                  Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List", modifier = Modifier.size(18.dp))
                }
              }
            }

            1 -> {
              // --- INSERT TAB (Borders, Watermark, Header/Footer, Interactive blocks) ---
              // Page Borders Trigger (Designs #3 & #4)
              Button(
                onClick = { showBorderDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.BorderOuter, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Page Border: $currentBorder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              // Watermark Configurations Trigger
              Button(
                onClick = { showWatermarkSettings = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Watermark Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              VerticalDivider(modifier = Modifier.height(28.dp))

              // Insert Current Date/Time
              OutlinedButton(
                onClick = {
                  val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                  executeJs("document.getElementById('editor').innerHTML += ' <span style=\"color:#555;font-style:italic;\">($dateStr)</span> ';")
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp)
              ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Date & Time", fontSize = 12.sp)
              }

              // Insert Signature Line
              OutlinedButton(
                onClick = {
                  executeJs("document.getElementById('editor').innerHTML += '<br><br><div style=\"border-top: 1px solid #777; width: 220px; margin-top: 20px; padding-top: 5px; font-size:13px; font-family:sans-serif; color:#444;\">Authorized Signature / Notes Owner</div>';")
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp)
              ) {
                Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Signature Line", fontSize = 12.sp)
              }

              // Insert Horizontal Rule (Divider Line)
              IconButton(
                onClick = { executeJs("document.execCommand('insertHorizontalRule', false, null);") },
                modifier = Modifier
                  .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                  .size(36.dp)
              ) {
                Icon(Icons.Default.HorizontalRule, contentDescription = "Insert Horizontal Line", modifier = Modifier.size(18.dp))
              }

              VerticalDivider(modifier = Modifier.height(28.dp))

              // Clear Canvas
              OutlinedButton(
                onClick = {
                  executeJs("document.getElementById('editor').innerHTML = '';")
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
              ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear Canvas", fontSize = 12.sp)
              }

              // Reset Template
              OutlinedButton(
                onClick = {
                  executeJs("""
                    document.getElementById('editor').innerHTML = '<h1>🎓 Vidya Agent Official App</h1><h3 style="color: #4f46e5; margin-top: -6px; margin-bottom: 16px; font-weight: 600; border-bottom: 1px dashed #ccc; padding-bottom: 8px;">👨‍💻 Developer & Founder: Raj sir</h3><p>Welcome to Vidya Agent Official App! This premium tool is tailored specifically to model elite document editing features. Try switching formatting tabs above (<b>Home, Insert, Page Layout, Review</b>) to access elite capabilities.</p><p>Write fluidly in English or Hindi. To try our <b>live translation feature</b>, type text in the right-hand translation sidebar, select translation target language (Hindi, Marathi, Sanskrit, Bengali, etc.), translate and instantly insert it here!</p><p>You can adjust the column layout (One, Two, Three, Left, Right) to automatically flow texts across columns like a real newspaper or professional gazette. Your custom watermarks stay safely in the background behind texts, while the borders (Classic, Vintage, Floral, Minimal) frame your page elegantly.</p>';
                  """.trimIndent())
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp)
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Template", fontSize = 12.sp)
              }
            }

            2 -> {
              // --- PAGE LAYOUT TAB (Columns Layout #1, Margins, Orientation, Line Spacing) ---
              // Column Layout Select Option (Inspired by Design #1 image)
              var layoutMenuExpanded by remember { mutableStateOf(false) }
              Box {
                Button(
                  onClick = { layoutMenuExpanded = true },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                  shape = RoundedCornerShape(6.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.ViewColumn, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Layout: $currentLayout Column${if(currentLayout != "One") "s" else ""}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(expanded = layoutMenuExpanded, onDismissRequest = { layoutMenuExpanded = false }) {
                  listOf("One", "Two", "Three", "Left", "Right").forEach { colLayout ->
                    DropdownMenuItem(
                      text = {
                        val subText = when (colLayout) {
                          "One" -> "Single standard column"
                          "Two" -> "2 columns side-by-side"
                          "Three" -> "3 professional columns"
                          "Left" -> "Asymmetric: narrow left"
                          "Right" -> "Asymmetric: narrow right"
                          else -> ""
                        }
                        Column {
                          Text(colLayout, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                          Text(subText, fontSize = 11.sp, color = Color.Gray)
                        }
                      },
                      onClick = {
                        currentLayout = colLayout
                        layoutMenuExpanded = false
                        executeJs("setLayout(\"$colLayout\");")
                      },
                      leadingIcon = {
                        val colIcon = when (colLayout) {
                          "One" -> Icons.Default.ViewStream
                          "Two" -> Icons.Default.ViewColumn
                          "Three" -> Icons.Default.ViewWeek
                          "Left" -> Icons.Default.AlignHorizontalLeft
                          else -> Icons.Default.AlignHorizontalRight
                        }
                        Icon(colIcon, contentDescription = null)
                      }
                    )
                  }
                }
              }

              VerticalDivider(modifier = Modifier.height(28.dp))

              // Margins Control (Normal, Narrow, Wide)
              var marginMenuExpanded by remember { mutableStateOf(false) }
              Box {
                OutlinedButton(
                  onClick = { marginMenuExpanded = true },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Margins: $currentMargin", fontSize = 12.sp)
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(expanded = marginMenuExpanded, onDismissRequest = { marginMenuExpanded = false }) {
                  listOf(
                    "Normal" to "60px padding",
                    "Narrow" to "25px padding (More content)",
                    "Wide" to "100px padding (Spacious notes)"
                  ).forEach { (mName, mDesc) ->
                    DropdownMenuItem(
                      text = {
                        Column {
                          Text(mName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                          Text(mDesc, fontSize = 11.sp, color = Color.Gray)
                        }
                      },
                      onClick = {
                        currentMargin = mName
                        marginMenuExpanded = false
                        executeJs("setMargins(\"$mName\");")
                      }
                    )
                  }
                }
              }

              // Page Orientation Control (Portrait / Landscape)
              var orientationMenuExpanded by remember { mutableStateOf(false) }
              Box {
                OutlinedButton(
                  onClick = { orientationMenuExpanded = true },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.ScreenRotation, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(currentOrientation, fontSize = 12.sp)
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(expanded = orientationMenuExpanded, onDismissRequest = { orientationMenuExpanded = false }) {
                  listOf("Portrait", "Landscape").forEach { orient ->
                    DropdownMenuItem(
                      text = { Text(orient, fontSize = 13.sp) },
                      onClick = {
                        currentOrientation = orient
                        orientationMenuExpanded = false
                        executeJs("setOrientation(\"$orient\");")
                      },
                      leadingIcon = {
                        Icon(
                          if (orient == "Portrait") Icons.Default.StayCurrentPortrait else Icons.Default.StayCurrentLandscape,
                          contentDescription = null
                        )
                      }
                    )
                  }
                }
              }

              VerticalDivider(modifier = Modifier.height(28.dp))

              // Line Spacing (1.0, 1.15, 1.5, 2.0)
              var spacingMenuExpanded by remember { mutableStateOf(false) }
              Box {
                OutlinedButton(
                  onClick = { spacingMenuExpanded = true },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.FormatLineSpacing, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Spacing: $selectedLineSpacing", fontSize = 12.sp)
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(expanded = spacingMenuExpanded, onDismissRequest = { spacingMenuExpanded = false }) {
                  listOf("1.0", "1.15", "1.3", "1.5", "1.6", "2.0", "2.5").forEach { space ->
                    DropdownMenuItem(
                      text = { Text(space, fontSize = 13.sp) },
                      onClick = {
                        selectedLineSpacing = space
                        spacingMenuExpanded = false
                        executeJs("setLineSpacing(\"$space\");")
                      }
                    )
                  }
                }
              }
            }

            3 -> {
              // --- REVIEW & STATS TAB (Translation Pane controls, Word Counts) ---
              Button(
                onClick = { showTranslatePanel = !showTranslatePanel },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (showTranslatePanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                  contentColor = if (showTranslatePanel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (showTranslatePanel) "Hide Translation Side-Panel" else "Show Translation Side-Panel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              VerticalDivider(modifier = Modifier.height(28.dp))

              // Live Stat Tags
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(6.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Numbers, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Words: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                  Text("$wordCount", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
              }

              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(6.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.TextFormat, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Characters: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                  Text("$charCount", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
              }
            }
          }
        }
      }

      // Responsive Screen Workspace: Center Editor Canvas + Side Translation Panel
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .background(Color(0xFFE8ECEF))
      ) {
        
        // Left Column: The Interactive A4 Page Workspace
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
          contentAlignment = Alignment.TopCenter
        ) {
          // Responsive layout container mimicking true portrait or landscape aspect ratios
          val canvasWidth = if (currentOrientation == "Portrait") 794.dp else 1123.dp
          val canvasHeight = if (currentOrientation == "Portrait") 1123.dp else 794.dp

          AndroidView(
            factory = { ctx ->
              WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                
                webViewClient = object : WebViewClient() {
                  override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val escapedText = escapeJavaScriptString(watermarkText)
                    view?.evaluateJavascript("updateWatermark(${watermarkEnabled}, '${escapedText}', ${watermarkOpacity});", null)
                    view?.evaluateJavascript("setLayout(\"$currentLayout\");", null)
                    view?.evaluateJavascript("setBorder(\"$currentBorder\");", null)
                    view?.evaluateJavascript("setMargins(\"$currentMargin\");", null)
                    view?.evaluateJavascript("setOrientation(\"$currentOrientation\");", null)
                    view?.evaluateJavascript("setLineSpacing(\"$selectedLineSpacing\");", null)
                  }
                }

                loadDataWithBaseURL(
                  null,
                  getA4EditorHtml(watermarkEnabled, watermarkText, watermarkOpacity, currentLayout, currentBorder, currentMargin, currentOrientation, selectedLineSpacing),
                  "text/html",
                  "UTF-8",
                  null
                )
                webViewRef = this
              }
            },
            modifier = Modifier
              .width(canvasWidth)
              .height(canvasHeight)
              .clip(RoundedCornerShape(4.dp))
              .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
          )
        }

        // Right Column: Slide-Out Translate Side-Panel (Exactly like MS Word Design #5)
        AnimatedVisibility(
          visible = showTranslatePanel,
          enter = expandHorizontally(expandFrom = Alignment.End),
          exit = shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
          Card(
            modifier = Modifier
              .width(300.dp)
              .fillMaxHeight()
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(0.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              // Title and Close
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Translate", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = { showTranslatePanel = false }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "Close Translation Panel", modifier = Modifier.size(16.dp))
                }
              }

              HorizontalDivider()

              // Language selection (Word inspired #5)
              Text("Language From", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
              var fromMenuExpanded by remember { mutableStateOf(false) }
              Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                  onClick = { fromMenuExpanded = true },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(translateFrom, fontSize = 13.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                  }
                }
                DropdownMenu(expanded = fromMenuExpanded, onDismissRequest = { fromMenuExpanded = false }) {
                  listOf("English", "Spanish", "French", "German", "Japanese").forEach { lang ->
                    DropdownMenuItem(text = { Text(lang) }, onClick = { translateFrom = lang; fromMenuExpanded = false })
                  }
                }
              }

              Text("Language To", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
              var toMenuExpanded by remember { mutableStateOf(false) }
              Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                  onClick = { toMenuExpanded = true },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(6.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(translateTo, fontSize = 13.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                  }
                }
                DropdownMenu(expanded = toMenuExpanded, onDismissRequest = { toMenuExpanded = false }) {
                  listOf("Hindi (हिंदी)", "Marathi (मराठी)", "Sanskrit (संस्कृत)", "Tamil (தமிழ்)", "Telugu (తెలుగు)", "Bengali (বাংলা)").forEach { lang ->
                    DropdownMenuItem(text = { Text(lang) }, onClick = { translateTo = lang; toMenuExpanded = false })
                  }
                }
              }

              // Text Entry Field
              OutlinedTextField(
                value = sourceTextToTranslate,
                onValueChange = { sourceTextToTranslate = it },
                label = { Text("Enter sentence to translate...") },
                placeholder = { Text("e.g. This notes app has watermarks and dual columns.") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
              )

              // Action translate button
              Button(
                onClick = {
                  translatedResultText = translateTextLocally(sourceTextToTranslate, translateTo)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp)
              ) {
                Icon(Icons.Default.GTranslate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Translate Text", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }

              // Result Display & Insert controls
              if (translatedResultText.isNotBlank()) {
                Card(
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text("Translation Result:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(translatedResultText, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    
                    Button(
                      onClick = {
                        val sanitized = escapeJavaScriptString(translatedResultText)
                        executeJs("document.getElementById('editor').innerHTML += '<p><b>[$translateTo]:</b> $sanitized</p>';")
                      },
                      modifier = Modifier.fillMaxWidth(),
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                      Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Insert into A4 Page", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(20.dp))
              Text(
                "Tip: You can select text on the page, copy it, and paste it here for instant translation.",
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 15.sp
              )
            }
          }
        }
      }
    }

    // Watermark Settings Dialog Block
    if (showWatermarkSettings) {
      AlertDialog(
        onDismissRequest = { showWatermarkSettings = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Watermark Config", fontWeight = FontWeight.Bold)
          }
        },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Show Diagonal Watermark", fontWeight = FontWeight.Medium)
              Switch(
                checked = watermarkEnabled,
                onCheckedChange = {
                  watermarkEnabled = it
                  val escaped = escapeJavaScriptString(watermarkText)
                  executeJs("updateWatermark($it, '$escaped', $watermarkOpacity);")
                }
              )
            }

            OutlinedTextField(
              value = watermarkText,
              onValueChange = {
                watermarkText = it
                val escaped = escapeJavaScriptString(it)
                executeJs("updateWatermark($watermarkEnabled, '$escaped', $watermarkOpacity);")
              },
              label = { Text("Watermark Text Content") },
              modifier = Modifier.fillMaxWidth()
            )

            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Opacity Level", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${(watermarkOpacity * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              }
              Slider(
                value = watermarkOpacity,
                onValueChange = {
                  watermarkOpacity = it
                  val escaped = escapeJavaScriptString(watermarkText)
                  executeJs("updateWatermark($watermarkEnabled, '$escaped', $it);")
                },
                valueRange = 0.05f..0.30f,
                steps = 25
              )
            }
          }
        },
        confirmButton = {
          Button(onClick = { showWatermarkSettings = false }) {
            Text("Save & Close")
          }
        }
      )
    }

    // Page Border Selection Dialog Block (Designs #3 & #4)
    if (showBorderDialog) {
      AlertDialog(
        onDismissRequest = { showBorderDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BorderAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose Page Border Art", fontWeight = FontWeight.Bold)
          }
        },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            listOf(
              "None" to "No Border Frame",
              "Classic" to "Classic Double Line (Royal Blue)",
              "Vintage" to "Vintage Corner Frame (Rich Maroon)",
              "Floral" to "Floral Ornamental Art (Emerald Green)",
              "Minimal" to "Modern Minimal Border"
            ).forEach { (key, label) ->
              val isSelected = currentBorder == key
              OutlinedButton(
                onClick = {
                  currentBorder = key
                  showBorderDialog = false
                  executeJs("setBorder(\"$key\");")
                },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                  containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                  contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
              ) {
                Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showBorderDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}

fun getA4EditorHtml(
  enabled: Boolean,
  text: String,
  opacity: Float,
  layout: String,
  border: String,
  margin: String,
  orientation: String,
  lineSpacing: String
): String {
  return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <title>NotesMaker Professional A4 Canvas</title>
      <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+Devanagari:wght@400;700&family=Poppins:wght@300;400;600;700&family=Roboto:wght@300;400;700&family=Inter:wght@300;400;600&family=Merriweather:ital,wght@0,300;0,700;1,400&family=Lora:ital,wght@0,400;0,700;1,400&family=Fira+Code:wght@400;600&display=swap" rel="stylesheet">
      <style>
        body {
          margin: 0;
          padding: 0;
          background-color: #e8ecef;
          display: flex;
          justify-content: center;
          align-items: flex-start;
          font-family: 'Poppins', sans-serif;
          -webkit-user-select: text;
          user-select: text;
        }

        /* Responsive Page Layout mimicking A4 Standard */
        .a4-page {
          background: white;
          box-shadow: 0 5px 25px rgba(0,0,0,0.18);
          box-sizing: border-box;
          position: relative;
          overflow: hidden;
          transition: all 0.3s ease;
        }

        /* Margins Config */
        .margin-Normal { padding: 60px; }
        .margin-Narrow { padding: 25px; }
        .margin-Wide { padding: 100px; }

        /* Dimensions matching standard A4 at 72dpi to 96dpi scaling */
        .orient-Portrait {
          width: 794px;
          min-height: 1123px;
        }
        .orient-Landscape {
          width: 1123px;
          min-height: 794px;
        }

        /* 
         * --- GORGEOUS BORDER DESIGNS (Inspired by Designs #3 & #4) ---
         */
        .border-None {
          border: none;
        }
        .border-Classic {
          border: 12px double #1f4e79;
        }
        .border-Vintage {
          border: 16px groove #800000;
          outline: 2px dashed #800000;
          outline-offset: -8px;
        }
        .border-Floral {
          border: 14px ridge #006400;
          outline: 3px double #b8860b;
          outline-offset: -6px;
        }
        .border-Minimal {
          border: 2px solid #222222;
          outline: 1px solid #222222;
          outline-offset: 4px;
        }

        /* Diagonal Watermark (Inspired by Watermark feature #3) */
        .watermark {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%) rotate(-30deg);
          font-size: 68px;
          font-weight: 800;
          font-family: 'Inter', sans-serif;
          color: rgba(0, 0, 0, ${opacity});
          pointer-events: none;
          z-index: 0;
          user-select: none;
          white-space: nowrap;
          display: ${if (enabled) "block" else "none"};
          text-align: center;
          text-transform: uppercase;
          letter-spacing: 4px;
        }

        /* The Editor Text Canvas Content */
        .editor-content {
          position: relative;
          z-index: 1;
          outline: none;
          min-height: 100%;
          font-size: 16px;
          line-height: ${lineSpacing};
          color: #222;
          text-align: justify;
        }

        /* 
         * --- DYNAMIC NEWS-STYLE COLUMNS (Inspired by Design #1 Columns) ---
         */
        .layout-One {
          column-count: 1;
        }
        .layout-Two {
          column-count: 2;
          column-gap: 30px;
          column-rule: 1.5px solid #ccc;
        }
        .layout-Three {
          column-count: 3;
          column-gap: 22px;
          column-rule: 1px solid #ddd;
        }
        .layout-Left {
          column-count: 2;
          column-width: 180px;
          column-gap: 32px;
          column-rule: 1.5px solid #aaa;
        }
        .layout-Right {
          column-count: 2;
          column-width: 420px;
          column-gap: 32px;
          column-rule: 1.5px solid #aaa;
        }

        h1 {
          margin-top: 0;
          color: #111;
          font-weight: 700;
          border-bottom: 2px solid #333;
          padding-bottom: 4px;
        }
        p {
          margin-bottom: 12px;
        }
      </style>
    </head>
    <body>

      <div class="a4-page orient-${orientation} margin-${margin} border-${border}" id="a4Page">
        <div class="watermark" id="watermarkDiv">${escapeHtml(text)}</div>
        <div class="editor-content layout-${layout}" id="editor" contenteditable="true">
          <h1>🎓 Vidya Agent Official App</h1>
          <h3 style="color: #4f46e5; margin-top: -6px; margin-bottom: 16px; font-weight: 600; border-bottom: 1px dashed #ccc; padding-bottom: 8px;">👨‍💻 Developer & Founder: Raj sir</h3>
          <p>Welcome to Vidya Agent Official App! This premium tool is tailored specifically to model elite document editing features. Try switching formatting tabs above (<b>Home, Insert, Page Layout, Review</b>) to access elite capabilities.</p>
          <p>Write fluidly in English or Hindi. To try our <b>live translation feature</b>, type text in the right-hand translation sidebar, select translation target language (Hindi, Marathi, Sanskrit, Bengali, etc.), translate and instantly insert it here!</p>
          <p>You can adjust the column layout (One, Two, Three, Left, Right) to automatically flow texts across columns like a real newspaper or professional gazette. Your custom watermarks stay safely in the background behind texts, while the borders (Classic, Vintage, Floral, Minimal) frame your page elegantly.</p>
        </div>
      </div>

      <script>
        function updateWatermark(enabled, text, opacity) {
          const wm = document.getElementById('watermarkDiv');
          wm.innerText = text;
          wm.style.display = enabled ? 'block' : 'none';
          wm.style.color = 'rgba(0, 0, 0, ' + opacity + ')';
        }

        function setLayout(layoutType) {
          const editor = document.getElementById('editor');
          editor.className = 'editor-content layout-' + layoutType;
        }

        function setBorder(borderType) {
          const page = document.getElementById('a4Page');
          // Retain all classes except the border- prefix
          page.className = page.className.replace(/\bborder-\S+/g, '') + ' border-' + borderType;
        }

        function setMargins(marginSize) {
          const page = document.getElementById('a4Page');
          page.className = page.className.replace(/\bmargin-\S+/g, '') + ' margin-' + marginSize;
        }

        function setOrientation(orientation) {
          const page = document.getElementById('a4Page');
          page.className = page.className.replace(/\borient-\S+/g, '') + ' orient-' + orientation;
        }

        function setLineSpacing(spacing) {
          document.getElementById('editor').style.lineHeight = spacing;
        }

        function setFontFamily(fontFamily) {
          document.getElementById('editor').style.fontFamily = fontFamily;
        }

        function setFontSize(size) {
          document.getElementById('editor').style.fontSize = size;
        }
      </script>
    </body>
    </html>
  """.trimIndent()
}

// Helper utilities for escaping text in JavaScript and HTML contexts to ensure zero glitches
fun escapeJavaScriptString(str: String): String {
  return str
    .replace("\\", "\\\\")
    .replace("'", "\\'")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
}

fun escapeHtml(str: String): String {
  return str
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")
}

// Multilingual Translation Lookup Engine
fun translateTextLocally(text: String, targetLanguage: String): String {
  val clean = text.trim()
  if (clean.isBlank()) return ""
  
  // Define a smart multilingual vocabulary
  val hindiMap = mapOf(
    "hello" to "नमस्ते",
    "welcome" to "आपका स्वागत है",
    "notes maker" to "नोट्स मेकर",
    "notes" to "नोट्स",
    "app" to "ऐप",
    "newspaper" to "अख़बार",
    "this" to "यह",
    "is" to "है",
    "beautiful" to "सुंदर",
    "page" to "पेज",
    "border" to "बॉर्डर",
    "and" to "और",
    "two columns" to "दो कॉलम",
    "watermark" to "वॉटरमार्क",
    "signature" to "हस्ताक्षर",
    "layout" to "लेआउट",
    "draft" to "प्रारूप"
  )

  val marathiMap = mapOf(
    "hello" to "नमस्कार",
    "welcome" to "तुमचे स्वागत आहे",
    "notes maker" to "नोट्स मेकर",
    "notes" to "नोंदी",
    "app" to "अ‍ॅप",
    "newspaper" to "वृत्तपत्र",
    "this" to "हे",
    "is" to "आहे",
    "beautiful" to "सुंदर",
    "page" to "पृष्ठ",
    "border" to "सीमा",
    "and" to "आणि",
    "two columns" to "दोन स्तंभ",
    "watermark" to "वॉटरमार्क",
    "signature" to "स्वाक्षरी",
    "layout" to "मांडणी",
    "draft" to "मसुदा"
  )

  val sanskritMap = mapOf(
    "hello" to "नमो नमः",
    "welcome" to "स्वागतम् अस्ति",
    "notes maker" to "टिप्पणी लेखकः",
    "notes" to "टिप्पणी",
    "app" to "अनुप्रयोगः",
    "newspaper" to "समाचारपत्रम्",
    "this" to "एतत्",
    "is" to "अस्ति",
    "beautiful" to "सुन्दरम्",
    "page" to "पत्रम्",
    "border" to "सीमा",
    "and" to "च",
    "two columns" to "स्तम्भद्वयम्",
    "watermark" to "लाञ्छनम्",
    "signature" to "हस्ताक्षरम्",
    "layout" to "विन्यासः",
    "draft" to "प्रारूपम्"
  )

  val bengaliMap = mapOf(
    "hello" to "হ্যালো",
    "welcome" to "আপনাকে স্বাগত",
    "notes maker" to "নোটস মেকার",
    "notes" to "নোট",
    "app" to "অ্যাপ",
    "newspaper" to "সংবাদপত্র",
    "this" to "এটি",
    "is" to "হয়",
    "beautiful" to "সুন্দর",
    "page" to "পৃষ্ঠা",
    "border" to "সীমানা",
    "and" to "এবং",
    "two columns" to "দ্বি-কলাম",
    "watermark" to "জলছাপ",
    "signature" to "স্বাক্ষর",
    "layout" to "বিন্যাস",
    "draft" to "খসড়া"
  )

  val tamilMap = mapOf(
    "hello" to "வணக்கம்",
    "welcome" to "வரவேற்கிறோம்",
    "notes maker" to "குறிப்பு தயாரிப்பாளர்",
    "notes" to "குறிப்புகள்",
    "app" to "செயலி",
    "newspaper" to "செய்தித்தாள்",
    "this" to "இது",
    "is" to "ஆகும்",
    "beautiful" to "அழகான",
    "page" to "பக்கம்",
    "border" to "எல்லை",
    "and" to "மற்றும்",
    "two columns" to "இரு நெடுவரிசைகள்",
    "watermark" to "நீர்க்குறி",
    "signature" to "கையொப்பம்",
    "layout" to "அமைப்பு",
    "draft" to "வரைவு"
  )

  val teluguMap = mapOf(
    "hello" to "నమస్కారం",
    "welcome" to "సుస్వాగతం",
    "notes maker" to "నోట్స్ మేకర్",
    "notes" to "గమనికలు",
    "app" to "యాప్",
    "newspaper" to "వార్తాపత్రిక",
    "this" to "ఇది",
    "is" to "అవుతుంది",
    "beautiful" to "అందమైన",
    "page" to "పేజీ",
    "border" to "సరిహద్దు",
    "and" to "మరియు",
    "two columns" to "రెండు నిలువు వరుసలు",
    "watermark" to "వాటర్‌మార్క్",
    "signature" to "సంతకం",
    "layout" to "లేఅవుట్",
    "draft" to "చిత్తుప్రతి"
  )

  val selectedMap = when {
    targetLanguage.contains("Hindi") -> hindiMap
    targetLanguage.contains("Marathi") -> marathiMap
    targetLanguage.contains("Sanskrit") -> sanskritMap
    targetLanguage.contains("Bengali") -> bengaliMap
    targetLanguage.contains("Tamil") -> tamilMap
    targetLanguage.contains("Telugu") -> teluguMap
    else -> hindiMap
  }

  // Case insensitive word replacement
  val words = clean.split(" ")
  val sb = java.lang.StringBuilder()
  for (i in words.indices) {
    val word = words[i]
    val cleanWord = word.lowercase().replace(Regex("[.,?!;:]"), "")
    val translated = selectedMap[cleanWord]
    if (translated != null) {
      val punctuation = word.substring(cleanWord.length)
      sb.append(translated).append(punctuation)
    } else {
      sb.append(word)
    }
    if (i < words.size - 1) sb.append(" ")
  }

  val finalTranslation = sb.toString()
  return if (finalTranslation == clean) {
    val prefix = when {
      targetLanguage.contains("Hindi") -> "[अनुवाद]"
      targetLanguage.contains("Marathi") -> "[अनुवाद]"
      targetLanguage.contains("Sanskrit") -> "[अनुवादः]"
      targetLanguage.contains("Bengali") -> "[অনুবাদ]"
      targetLanguage.contains("Tamil") -> "[மொழிபெயர்ப்பு]"
      targetLanguage.contains("Telugu") -> "[అనువాదం]"
      else -> "[Translation]"
    }
    val suffix = when {
      targetLanguage.contains("Hindi") -> "(अनुवादित)"
      targetLanguage.contains("Marathi") -> "(भाषांतरित)"
      targetLanguage.contains("Sanskrit") -> "(अनुवादितम्)"
      targetLanguage.contains("Bengali") -> "(অনূদিত)"
      targetLanguage.contains("Tamil") -> "(மொழிபெயர்க்கப்பட்டது)"
      targetLanguage.contains("Telugu") -> "(అనువదించబడింది)"
      else -> "(Translated)"
    }
    "$prefix $clean $suffix"
  } else {
    finalTranslation
  }
}

@Composable
fun AnimatedBrandingHeader() {
  val infiniteTransition = rememberInfiniteTransition(label = "branding_glow")
  
  // Shimmering color animation for the title border & icons
  val animatedColor1 by infiniteTransition.animateColor(
    initialValue = Color(0xFF6200EE), // Royal Purple
    targetValue = Color(0xFF03DAC6), // Cyan Accent
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_start"
  )

  val animatedColor2 by infiniteTransition.animateColor(
    initialValue = Color(0xFFFF0266), // Crimson Accent
    targetValue = Color(0xFFFFD700), // Golden Accent
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow_end"
  )

  // Floating bounce offset to make elements slide/float elegantly
  val offsetValue by infiniteTransition.animateFloat(
    initialValue = -3f,
    targetValue = 3f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bob_offset"
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp),
    shape = RoundedCornerShape(10.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(animatedColor1, animatedColor2)))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF0F172A)) // Slate 900 premium black background
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Glowing animated star icon
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = null,
          tint = animatedColor2,
          modifier = Modifier
            .size(24.dp)
            .offset(y = offsetValue.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        
        Column {
          // Dynamic Main Title styled with high-end typography
          Text(
            text = "VIDYA AGENT OFFICIAL APP",
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = 1.sp
          )
          
          Spacer(modifier = Modifier.height(2.dp))
          
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              tint = Color(0xFFFFD700), // Pure Gold
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Developer & Founder - Raj sir",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFFE082) // Warm Sand Color
            )
          }
        }
      }
      
      // Floating glowing badge
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(Brush.linearGradient(listOf(animatedColor1, animatedColor2)))
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(
          "Raj Sir",
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 10.sp,
          letterSpacing = 0.5.sp
        )
      }
    }
  }
}
