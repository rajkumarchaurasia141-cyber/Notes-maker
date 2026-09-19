package com.example

import android.content.Context
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
  
  var currentLayout by remember { mutableStateOf("One") } // One, Two, Three, Left, Right
  var currentBorder by remember { mutableStateOf("None") } // None, Classic, Vintage, Floral, Minimal
  var showWatermarkSettings by remember { mutableStateOf(false) }
  var showTranslateDialog by remember { mutableStateOf(false) }
  var showBorderDialog by remember { mutableStateOf(false) }
  
  var watermarkEnabled by remember { mutableStateOf(true) }
  var watermarkText by remember { mutableStateOf("NotesMaker App") }
  var watermarkOpacity by remember { mutableStateOf(0.12f) }
  
  var selectedFont by remember { mutableStateOf("Poppins") }
  var selectedSize by remember { mutableStateOf("16px") }
  
  // Translation state
  var translateFrom by remember { mutableStateOf("English") }
  var translateTo by remember { mutableStateOf("Hindi (हिंदी)") }
  var sourceTextToTranslate by remember { mutableStateOf("") }
  var translatedResultText by remember { mutableStateOf("") }

  var webViewRef by remember { mutableStateOf<WebView?>(null) }

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

  fun executeJs(js: String) {
    webViewRef?.evaluateJavascript(js, null)
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("NotesMaker App", fontWeight = FontWeight.Bold, fontSize = 18.sp)
          }
        },
        actions = {
          // Layout / Columns Menu (Inspired by Word design #1)
          var layoutMenuExpanded by remember { mutableStateOf(false) }
          Box {
            OutlinedButton(
              onClick = { layoutMenuExpanded = true },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.ViewColumn, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Columns: $currentLayout", fontSize = 12.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
              expanded = layoutMenuExpanded,
              onDismissRequest = { layoutMenuExpanded = false }
            ) {
              listOf("One", "Two", "Three", "Left", "Right").forEach { layout ->
                DropdownMenuItem(
                  text = { Text("$layout Column${if(layout!="One" && layout!="Three") " (Asymmetric)" else if(layout=="Three") "s" else ""}") },
                  onClick = {
                    currentLayout = layout
                    layoutMenuExpanded = false
                    executeJs("setLayout(\"$layout\");")
                  },
                  leadingIcon = { Icon(Icons.Default.ViewStream, contentDescription = null) }
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Page Border Button (Inspired by Word designs #3 & #4)
          IconButton(onClick = { showBorderDialog = true }) {
            Icon(Icons.Default.BorderAll, contentDescription = "Page Borders", tint = MaterialTheme.colorScheme.primary)
          }

          // Translate Button (Inspired by Word design #5)
          IconButton(onClick = { showTranslateDialog = true }) {
            Icon(Icons.Default.Translate, contentDescription = "Translate", tint = MaterialTheme.colorScheme.primary)
          }

          // Watermark Settings
          IconButton(onClick = { showWatermarkSettings = true }) {
            Icon(Icons.Default.WaterDrop, contentDescription = "Watermark", tint = MaterialTheme.colorScheme.primary)
          }

          // Export PDF
          Button(
            onClick = {
              webViewRef?.let { wv ->
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = "NotesMaker_A4_Note"
                val printAdapter = wv.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
              }
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Export PDF", fontSize = 12.sp)
          }
          Spacer(modifier = Modifier.width(8.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Formatting Toolbar (Fonts, Sizes, Styles, Highlight Colors - Inspired by Word design #2)
      Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          var fontMenuExpanded by remember { mutableStateOf(false) }
          Box {
            OutlinedButton(onClick = { fontMenuExpanded = true }, contentPadding = PaddingValues(8.dp)) {
              Text(selectedFont, fontSize = 12.sp)
              Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) {
              fonts.forEach { (name, cssFont) ->
                DropdownMenuItem(
                  text = { Text(name, fontFamily = androidx.compose.ui.text.font.FontFamily.Default) },
                  onClick = {
                    selectedFont = name
                    fontMenuExpanded = false
                    executeJs("setFontFamily(\"$cssFont\");")
                  }
                )
              }
            }
          }

          var sizeMenuExpanded by remember { mutableStateOf(false) }
          Box {
            OutlinedButton(onClick = { sizeMenuExpanded = true }, contentPadding = PaddingValues(8.dp)) {
              Text(selectedSize, fontSize = 12.sp)
              Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = sizeMenuExpanded, onDismissRequest = { sizeMenuExpanded = false }) {
              listOf("12px", "14px", "16px", "18px", "24px", "32px").forEach { sz ->
                DropdownMenuItem(
                  text = { Text(sz) },
                  onClick = {
                    selectedSize = sz
                    sizeMenuExpanded = false
                    executeJs("setFontSize(\"$sz\");")
                  }
                )
              }
            }
          }

          Divider(modifier = Modifier.height(24.dp).width(1.dp))

          IconButton(onClick = { executeJs("document.execCommand('bold', false, null);") }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(20.dp))
          }
          IconButton(onClick = { executeJs("document.execCommand('italic', false, null);") }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(20.dp))
          }
          IconButton(onClick = { executeJs("document.execCommand('underline', false, null);") }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", modifier = Modifier.size(20.dp))
          }

          Divider(modifier = Modifier.height(24.dp).width(1.dp))

          // Highlight color swatches (Inspired by Word Design #2 Theme Colors)
          val highlightColors = listOf("#ffff00", "#00ff00", "#00ffff", "#ff00ff", "#ffc0cb", "#ffa500")
          highlightColors.forEach { colorHex ->
            Box(
              modifier = Modifier
                .size(24.dp)
                .background(Color(android.graphics.Color.parseColor(colorHex)))
            ) {
              IconButton(
                onClick = { executeJs("document.execCommand('hiliteColor', false, '$colorHex');") },
                modifier = Modifier.fillMaxSize()
              ) {}
            }
          }

          Divider(modifier = Modifier.height(24.dp).width(1.dp))

          IconButton(onClick = { executeJs("document.execCommand('justifyLeft', false, null);") }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.FormatAlignLeft, contentDescription = "Left", modifier = Modifier.size(20.dp))
          }
          IconButton(onClick = { executeJs("document.execCommand('justifyCenter', false, null);") }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.FormatAlignCenter, contentDescription = "Center", modifier = Modifier.size(20.dp))
          }
          IconButton(onClick = { executeJs("document.execCommand('justifyRight', false, null);") }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.FormatAlignRight, contentDescription = "Right", modifier = Modifier.size(20.dp))
          }
          IconButton(onClick = { executeJs("document.execCommand('justifyFull', false, null);") }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.FormatAlignJustify, contentDescription = "Justify", modifier = Modifier.size(20.dp))
          }

          Divider(modifier = Modifier.height(24.dp).width(1.dp))

          IconButton(onClick = { executeJs("document.execCommand('insertUnorderedList', false, null);") }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", modifier = Modifier.size(20.dp))
          }
          IconButton(onClick = { executeJs("document.execCommand('insertOrderedList', false, null);") }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List", modifier = Modifier.size(20.dp))
          }
        }
      }

      // A4 Editor Canvas
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFFEFEFEF))
          .horizontalScroll(rememberScrollState())
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
        contentAlignment = Alignment.TopCenter
      ) {
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
                  view?.evaluateJavascript("updateWatermark(${watermarkEnabled}, '${watermarkText}', ${watermarkOpacity});", null)
                  view?.evaluateJavascript("setLayout(\"$currentLayout\");", null)
                  view?.evaluateJavascript("setBorder(\"$currentBorder\");", null)
                }
              }

              loadDataWithBaseURL(
                null,
                getA4EditorHtml(watermarkEnabled, watermarkText, watermarkOpacity, currentLayout, currentBorder),
                "text/html",
                "UTF-8",
                null
              )
              webViewRef = this
            }
          },
          modifier = Modifier
            .width(794.dp)
            .height(1123.dp)
        )
      }
    }

    // Watermark Settings Dialog
    if (showWatermarkSettings) {
      AlertDialog(
        onDismissRequest = { showWatermarkSettings = false },
        title = { Text("Watermark Settings", fontWeight = FontWeight.Bold) },
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
              Text("Enable Watermark")
              Switch(
                checked = watermarkEnabled,
                onCheckedChange = {
                  watermarkEnabled = it
                  executeJs("updateWatermark($it, '$watermarkText', $watermarkOpacity);")
                }
              )
            }

            OutlinedTextField(
              value = watermarkText,
              onValueChange = {
                watermarkText = it
                executeJs("updateWatermark($watermarkEnabled, '$it', $watermarkOpacity);")
              },
              label = { Text("Watermark Text") },
              modifier = Modifier.fillMaxWidth()
            )

            Column {
              Text("Opacity: ${(watermarkOpacity * 100).toInt()}%", fontSize = 12.sp)
              Slider(
                value = watermarkOpacity,
                onValueChange = {
                  watermarkOpacity = it
                  executeJs("updateWatermark($watermarkEnabled, '$watermarkText', $it);")
                },
                valueRange = 0.05f..0.30f,
                steps = 25
              )
            }
          }
        },
        confirmButton = {
          Button(onClick = { showWatermarkSettings = false }) {
            Text("Done")
          }
        }
      )
    }

    // Page Border Selection Dialog (Inspired by Word Design #3 & #4)
    if (showBorderDialog) {
      AlertDialog(
        onDismissRequest = { showBorderDialog = false },
        title = { Text("Choose Page Border", fontWeight = FontWeight.Bold) },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            listOf(
              "None" to "No Border",
              "Classic" to "Classic Double Line (Blue)",
              "Vintage" to "Vintage Corner Frame",
              "Floral" to "Floral / Ornamental Art",
              "Minimal" to "Modern Minimal Dark Border"
            ).forEach { (key, label) ->
              OutlinedButton(
                onClick = {
                  currentBorder = key
                  showBorderDialog = false
                  executeJs("setBorder(\"$key\");")
                },
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(label)
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

    // Translate Panel Dialog (Inspired by Word Design #5)
    if (showTranslateDialog) {
      AlertDialog(
        onDismissRequest = { showTranslateDialog = false },
        title = { Text("Translate Notes", fontWeight = FontWeight.Bold) },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text("Language From: English", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("Language To: Hindi (हिंदी)", fontSize = 14.sp, fontWeight = FontWeight.Medium)

            OutlinedTextField(
              value = sourceTextToTranslate,
              onValueChange = { sourceTextToTranslate = it },
              label = { Text("Enter text to translate") },
              modifier = Modifier.fillMaxWidth()
            )

            Button(
              onClick = {
                // Simulated robust translation mapping for demo
                translatedResultText = if (sourceTextToTranslate.isNotBlank()) {
                  "[Translated to Hindi]: " + sourceTextToTranslate
                    .replace("Hello", "नमस्ते")
                    .replace("Welcome", "स्वागत है")
                    .replace("Notes", "नोट्स")
                    .replace("App", "ऐप")
                    .replace("Newspaper", "अख़बार")
                } else {
                  "कृपया अनुवाद के लिए कुछ टेक्स्ट लिखें।"
                }
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Translate Now")
            }

            if (translatedResultText.isNotBlank()) {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text("Result:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(translatedResultText, fontSize = 14.sp)
                  Spacer(modifier = Modifier.height(8.dp))
                  Button(
                    onClick = {
                      executeJs("document.getElementById('editor').innerHTML += '<p><b>[Translation]:</b> " + translatedResultText + "</p>';")
                      showTranslateDialog = false
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Text("Insert into Page", fontSize = 12.sp)
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showTranslateDialog = false }) {
            Text("Close")
          }
        }
      )
    }
  }
}

fun getA4EditorHtml(enabled: Boolean, text: String, opacity: Float, layout: String, border: String): String {
  return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=794px, initial-scale=1.0">
      <title>NotesMaker A4 Page</title>
      <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+Devanagari:wght@400;700&family=Poppins:wght@300;400;600;700&family=Roboto:wght@300;400;700&family=Inter:wght@300;400;600&family=Merriweather:ital,wght@0,300;0,700;1,400&family=Lora:ital,wght@0,400;0,700;1,400&family=Fira+Code:wght@400;600&display=swap" rel="stylesheet">
      <style>
        body {
          margin: 0;
          padding: 0;
          background-color: #f5f5f5;
          display: flex;
          justify-content: center;
          align-items: flex-start;
          font-family: 'Poppins', sans-serif;
        }
        .a4-page {
          width: 794px;
          min-height: 1123px;
          background: white;
          box-shadow: 0 4px 20px rgba(0,0,0,0.15);
          padding: 60px;
          box-sizing: border-box;
          position: relative;
          overflow: hidden;
          border: 10px solid transparent;
        }
        
        /* Page Border Styles */
        .border-None { border: none; }
        .border-Classic { border: 6px double #1a73e8; }
        .border-Vintage { border: 8px groove #444; }
        .border-Floral { border: 8px ridge #b8860b; }
        .border-Minimal { border: 2px solid #222; }

        .watermark {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%) rotate(-30deg);
          font-size: 64px;
          font-weight: 700;
          color: rgba(0, 0, 0, ${opacity});
          pointer-events: none;
          z-index: 0;
          user-select: none;
          white-space: nowrap;
          display: ${if (enabled) "block" else "none"};
        }
        .editor-content {
          position: relative;
          z-index: 1;
          outline: none;
          min-height: 1000px;
          font-size: 16px;
          line-height: 1.6;
          color: #222;
        }
        
        /* Column Layout Options (Inspired by Word Design #1) */
        .layout-One {
          column-count: 1;
        }
        .layout-Two {
          column-count: 2;
          column-gap: 30px;
          column-rule: 1px solid #ccc;
        }
        .layout-Three {
          column-count: 3;
          column-gap: 20px;
          column-rule: 1px solid #ccc;
        }
        .layout-Left {
          column-count: 2;
          column-width: 150px;
          column-gap: 30px;
          column-rule: 1px solid #ccc;
        }
        .layout-Right {
          column-count: 2;
          column-width: 350px;
          column-gap: 30px;
          column-rule: 1px solid #ccc;
        }
      </style>
    </head>
    <body>
      <div class="a4-page border-${border}" id="a4Page">
        <div class="watermark" id="watermarkDiv">${text}</div>
        <div class="editor-content layout-${layout}" id="editor" contenteditable="true">
          <h1>📰 NotesMaker Newspaper Edition</h1>
          <p>Welcome to NotesMaker App! Type your notes here. You can switch columns (One, Two, Three, Left, Right), add page borders, highlight text, translate notes into Hindi/English, and add watermarks!</p>
          <p>हिंदी में भी आसानी से टाइप करें। Noto Sans Devanagari और Mangal फोंट्स के साथ आपके नोट्स बिल्कुल एक अख़बार या पेशेवर दस्तावेज़ की तरह दिखेंगे।</p>
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
          page.className = 'a4-page border-' + borderType;
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
