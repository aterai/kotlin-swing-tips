package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

private val editor = JEditorPane()
private val engine = createEngine()

fun makeUI(): Component {
  // https://github.com/google/code-prettify/blob/master/styles/desert.css
  val styleSheet = StyleSheet().also {
    it.addRule(".str {color:#ffa0a0}")
    it.addRule(".kwd {color:#f0e68c;font-weight:bold}")
    it.addRule(".com {color:#87ceeb}")
    it.addRule(".typ {color:#98fb98}")
    it.addRule(".lit {color:#cd5c5c}")
    it.addRule(".pun {color:#ffffff}")
    it.addRule(".pln {color:#ffffff}")
    it.addRule(".tag {color:#f0e68c;font-weight:bold}")
    it.addRule(".atn {color:#bdb76b;font-weight:bold}")
    it.addRule(".atv {color:#ffa0a0}")
    it.addRule(".dec {color:#98fb98}")
  }

  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.styleSheet = styleSheet
  editor.editorKit = htmlEditorKit
  editor.isEditable = false
  editor.selectedTextColor = null
  editor.selectionColor = Color(0x64_88_AA_AA, true)
  editor.background = Color(0x64_64_64) // 0x33_33_33

  val button = JButton("open")
  button.addActionListener {
    val fileChooser = JFileChooser()
    val ret = fileChooser.showOpenDialog(button.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      loadFile(fileChooser.selectedFile.absolutePath)
    }
  }

  val scroll = JScrollPane(editor)
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  val check = JCheckBox("HORIZONTAL_SCROLLBAR_NEVER", true)
  check.addActionListener { e ->
    scroll.horizontalScrollBarPolicy = if ((e.source as? JCheckBox)?.isSelected == true) {
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    } else {
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    }
  }

  val box = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.add(check)
    it.add(Box.createHorizontalGlue())
    it.add(button)
  }

  return JPanel(BorderLayout()).also {
    it.add(JLayer(scroll, ScrollPaneLayerUI()))
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun loadFile(path: String) {
  val html = runCatching {
    File(path).useLines { it.toList() }.joinToString("\n") {
      it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
  }.fold(
    onSuccess = { prettify(engine, it) },
    onFailure = { it.message }
  )
  editor.text = "<pre>$html\n</pre>"
}

private fun createEngine(): ScriptEngine? {
  val engine = ScriptEngineManager().getEngineByName("JavaScript")
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/prettify.js") ?: return null
  return runCatching {
    Files.newBufferedReader(Paths.get(url.toURI())).use {
      engine.eval("var window={}, navigator=null;")
      engine.eval(it)
    }
    engine
  }.onFailure { it.printStackTrace() }.getOrNull()
}

fun prettify(engine: ScriptEngine?, src: String) = runCatching {
  (engine as? Invocable)?.invokeMethod(engine.get("window"), "prettyPrintOne", src) as? String
}.getOrNull() ?: "error"

private class ScrollPaneLayerUI : LayerUI<JScrollPane>() {
  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val scroll = (c as? JLayer<*>)?.view as? JScrollPane ?: return
    val rect = scroll.viewportBorderBounds
    val m = scroll.horizontalScrollBar.model
    val extent = m.extent
    val maximum = m.maximum
    val value = m.value
    if (value + extent < maximum) {
      val w = rect.width
      val h = rect.height
      val shd = 6
      val g2 = g.create() as? Graphics2D ?: return
      g2.translate(rect.x + w - shd, rect.y)
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = Color(0x08_00_00_00, true)
      for (i in 0 until shd) {
        g2.fillRect(i, 0, shd - i, h)
      }
      // g2.setPaint(Color.RED)
      g2.fillRect(shd - 2, 0, 2, h) // Make the edge a bit darker
      g2.dispose()
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
