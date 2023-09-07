package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

private val THUMB_COLOR = Color(0x32_00_00_FF, true)
private const val SCALE = .15

val engine = createEngine()
val editor = JEditorPane().also {
  it.selectedTextColor = null
  it.selectionColor = Color(0x64_88_AA_AA, true)
  // TEST: it.setSelectionColor(null)
}
val scroll = JScrollPane(editor)
val label = object : JLabel() {
  private var handler: MouseInputListener? = null

  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    super.updateUI()
    handler = MiniMapHandler()
    addMouseListener(handler)
    addMouseMotionListener(handler)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val c = SwingUtilities.getAncestorOfClass(JViewport::class.java, editor)
    val viewport = c as? JViewport ?: return
    val rect = SwingUtilities.calculateInnerArea(this, Rectangle())

    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val sy = rect.getHeight() / editor.bounds.getHeight()
    val at = AffineTransform.getScaleInstance(1.0, sy)

    // paint Thumb
    val thumbRect = Rectangle(viewport.bounds)
    thumbRect.y = viewport.viewPosition.y
    val r = at.createTransformedShape(thumbRect).bounds
    val y = rect.y + r.y
    g2.color = THUMB_COLOR
    g2.fillRect(0, y, rect.width, r.height)
    g2.color = THUMB_COLOR.darker()
    g2.drawRect(0, y, rect.width - 1, r.height - 1)
    g2.dispose()
  }
}

private open class MiniMapHandler : MouseInputAdapter() {
  override fun mousePressed(e: MouseEvent) {
    processMiniMapMouseEvent(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    processMiniMapMouseEvent(e)
  }

  fun processMiniMapMouseEvent(e: MouseEvent) {
    val pt = e.point
    val c = e.component
    val m = scroll.verticalScrollBar.model
    val vsm = m.maximum - m.minimum
    val v = .5 - m.extent * .5 + pt.y * vsm / c.height.toDouble()
    m.value = v.toInt()
  }
}

val p = object : JPanel() {
  override fun isOptimizedDrawingEnabled() = false
}

fun makeUI(): Component {
  val styleSheet = StyleSheet().also {
    it.addRule(".str {color:#008800}")
    it.addRule(".kwd {color:#000088}")
    it.addRule(".com {color:#880000}")
    it.addRule(".typ {color:#660066}")
    it.addRule(".lit {color:#006666}")
    it.addRule(".pun {color:#666600}")
    it.addRule(".pln {color:#000000}")
    it.addRule(".tag {color:#000088}")
    it.addRule(".atn {color:#660066}")
    it.addRule(".atv {color:#008800}")
    it.addRule(".dec {color:#660066}")
  }

  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.styleSheet = styleSheet

  editor.editorKit = htmlEditorKit
  editor.isEditable = false
  editor.background = Color(0xEE_EE_EE)
  editor.selectedTextColor = null
  editor.selectionColor = Color(0x64_88_AA_AA, true)

  val button = JButton("open")
  button.addActionListener {
    val fileChooser = JFileChooser()
    val ret = fileChooser.showOpenDialog(button.rootPane)
    if (ret == JFileChooser.APPROVE_OPTION) {
      loadFile(fileChooser.selectedFile.absolutePath)
      EventQueue.invokeLater {
        label.icon = makeMiniMap(editor)
        p.revalidate()
        p.repaint()
      }
    }
  }

  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  scroll.verticalScrollBar.model.addChangeListener { label.repaint() }

  val pp = JPanel(BorderLayout(0, 0))
  pp.add(label, BorderLayout.NORTH)
  val minimap = JScrollPane(pp)
  minimap.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  minimap.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  box.add(Box.createHorizontalGlue())
  box.add(button)

  p.layout = object : BorderLayout(0, 0) {
    override fun layoutContainer(parent: Container) {
      synchronized(parent.treeLock) {
        val insets = parent.insets
        val width = parent.width
        val height = parent.height
        val top = insets.top
        val bottom = height - insets.bottom
        val left = insets.left
        val right = width - insets.right
        getLayoutComponent(parent, EAST)?.also {
          val d = it.preferredSize
          val vsb = scroll.verticalScrollBar
          val vsw = if (vsb.isVisible) vsb.size.width else 0
          it.setBounds(right - d.width - vsw, top, d.width, bottom - top)
        }
        getLayoutComponent(parent, CENTER)?.setBounds(left, top, right - left, bottom - top)
      }
    }
  }
  p.add(minimap, BorderLayout.EAST)
  p.add(scroll)
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun loadFile(path: String) {
  val html = runCatching {
    File(path).useLines { it.toList() }.joinToString("\n") {
      it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
  }.fold(
    onSuccess = { prettify(engine, it) },
    onFailure = { it.message }
  )
  editor.text = "<pre>$html\n</pre>"
  editor.caretPosition = 0
}

fun createEngine(): ScriptEngine? {
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

private fun makeMiniMap(c: Component): Icon {
  val d = c.size
  val newW = (d.width * SCALE).toInt()
  val newH = (d.height * SCALE).toInt()
  val image = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
  val g2 = image.createGraphics()
  g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
  g2.scale(SCALE, SCALE)
  c.paint(g2)
  g2.dispose()
  return ImageIcon(image)
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
