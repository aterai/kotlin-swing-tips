package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

class MainPanel : JPanel(BorderLayout()) {
  private val editor = JEditorPane()
  private val engine = createEngine()

  init {
    // https://github.com/google/code-prettify/blob/master/styles/desert.css
    val styleSheet = StyleSheet().also {
      it.addRule(".str {color:#ffa0a0}")
      it.addRule(".kwd {color:#f0e68c;font-weight:bold}")
      it.addRule(".com {color:#87ceeb0}")
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
    htmlEditorKit.setStyleSheet(styleSheet)
    editor.setEditorKit(htmlEditorKit)
    editor.setEditable(false)
    editor.setSelectedTextColor(null)
    editor.setSelectionColor(Color(0x64_88_AA_AA, true))
    editor.setBackground(Color(0x64_64_64)) // 0x33_33_33

    val button = JButton("open")
    button.addActionListener {
      val fileChooser = JFileChooser()
      val ret = fileChooser.showOpenDialog(getRootPane())
      if (ret == JFileChooser.APPROVE_OPTION) {
        loadFile(fileChooser.getSelectedFile().getAbsolutePath())
      }
    }

    val scroll = JScrollPane(editor)
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)

    val check = JCheckBox("HORIZONTAL_SCROLLBAR_NEVER", true)
    check.addActionListener { e ->
      val f = (e.getSource() as JCheckBox).isSelected()
      val p = if (f)
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      else
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
      scroll.setHorizontalScrollBarPolicy(p)
    }

    val box = Box.createHorizontalBox().also {
      it.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
      it.add(check)
      it.add(Box.createHorizontalGlue())
      it.add(button)
    }

    add(JLayer<JScrollPane>(scroll, ScrollPaneLayerUI()))
    add(box, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun loadFile(path: String) {
    try {
      val txt = File(path).useLines { it.toList() }.map {
        it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
      }.joinToString("\n")
      val html = "<pre>" + prettify(engine, txt) + "\n</pre>"
      editor.setText(html)
    } catch (ex: IOException) {
      ex.printStackTrace()
    }
  }

  private fun createEngine(): ScriptEngine? {
    val engine = ScriptEngineManager().getEngineByName("JavaScript")
    val url = MainPanel::class.java.getResource("prettify.js")
    return runCatching {
      BufferedReader(InputStreamReader(url.openStream(), StandardCharsets.UTF_8)).use { r ->
        engine.eval("var window={}, navigator=null;")
        engine.eval(r)
      }
      engine
    }.onFailure { it.printStackTrace() }.getOrNull()
  }

  private fun prettify(engine: ScriptEngine?, src: String) = runCatching {
    (engine as? Invocable)?.invokeMethod(engine.get("window"), "prettyPrintOne", src) as String
  }.onFailure { it.printStackTrace() }.getOrNull() ?: ""
}

internal class ScrollPaneLayerUI : LayerUI<JScrollPane>() {
  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val layer = c as? JLayer<*> ?: return
    val scroll = layer.getView() as JScrollPane
    val rect = scroll.getViewportBorderBounds()
    val m = scroll.getHorizontalScrollBar().getModel()
    val extent = m.getExtent()
    val maximum = m.getMaximum()
    val value = m.getValue()
    if (value + extent < maximum) {
      val w = rect.width
      val h = rect.height
      val shd = 6
      val g2 = g.create() as Graphics2D
      g2.translate(rect.x + w - shd, rect.y)
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setPaint(Color(0x08_00_00_00, true))
      for (i in 0 until shd) {
        g2.fillRect(i, 0, shd - i, h)
      }
      // g2.setPaint(Color.RED);
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
