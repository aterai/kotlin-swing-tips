package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Objects
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

private val THUMB_COLOR = Color(0x32_00_00_FF, true)
private const val SCALE = .15

val engine = createEngine()
val editor = JEditorPane().also {
  it.setSelectedTextColor(null)
  it.setSelectionColor(Color(0x64_88_AA_AA, true))
  // TEST: it.setSelectionColor(null);
}
val scroll = JScrollPane(editor)
val label = object : JLabel() {
  @Transient
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
    val vport = SwingUtilities.getAncestorOfClass(JViewport::class.java, editor)
    if (vport !is JViewport) {
      return
    }
    val vrect = vport.getBounds() // scroll.getViewportBorderBounds();
    val erect = editor.getBounds()
    val crect = SwingUtilities.calculateInnerArea(this, Rectangle())

    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val sy = crect.getHeight() / erect.getHeight()
    val at = AffineTransform.getScaleInstance(1.0, sy)

    // paint Thumb
    val thumbRect = Rectangle(vrect)
    thumbRect.y = vport.getViewPosition().y
    val r = at.createTransformedShape(thumbRect).getBounds()
    val y = crect.y + r.y
    g2.setColor(THUMB_COLOR)
    g2.fillRect(0, y, crect.width, r.height)
    g2.setColor(THUMB_COLOR.darker())
    g2.drawRect(0, y, crect.width - 1, r.height - 1)
    g2.dispose()
  }
}

private class MiniMapHandler : MouseInputAdapter() {
  override fun mousePressed(e: MouseEvent) {
    processMiniMapMouseEvent(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    processMiniMapMouseEvent(e)
  }

  protected fun processMiniMapMouseEvent(e: MouseEvent) {
    val pt = e.getPoint()
    val c = e.getComponent()
    val m = scroll.getVerticalScrollBar().getModel()
    val vsm = m.getMaximum() - m.getMinimum()
    val v = .5 - m.getExtent() * .5 + pt.y * vsm / c.getHeight().toDouble()
    m.setValue(v.toInt())
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
  htmlEditorKit.setStyleSheet(styleSheet)

  editor.setEditorKit(htmlEditorKit)
  editor.setEditable(false)
  editor.setBackground(Color(0xEE_EE_EE))
  editor.setSelectedTextColor(null)
  editor.setSelectionColor(Color(0x64_88_AA_AA, true))

  val button = JButton("open")
  button.addActionListener {
    val fileChooser = JFileChooser()
    val ret = fileChooser.showOpenDialog(button.getRootPane())
    if (ret == JFileChooser.APPROVE_OPTION) {
      loadFile(fileChooser.getSelectedFile().getAbsolutePath())
      EventQueue.invokeLater {
        label.setIcon(makeMiniMap(editor))
        p.revalidate()
        p.repaint()
      }
    }
  }

  scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
  scroll.getVerticalScrollBar().getModel().addChangeListener { label.repaint() }

  val pp = JPanel(BorderLayout(0, 0))
  pp.add(label, BorderLayout.NORTH)
  val minimap = JScrollPane(pp)
  minimap.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
  minimap.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)

  val box = Box.createHorizontalBox()
  box.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
  box.add(Box.createHorizontalGlue())
  box.add(button)

  p.setLayout(object : BorderLayout(0, 0) {
    override fun layoutContainer(parent: Container) {
      synchronized(parent.getTreeLock()) {
        val insets = parent.getInsets()
        val width = parent.getWidth()
        val height = parent.getHeight()
        val top = insets.top
        val bottom = height - insets.bottom
        val left = insets.left
        val right = width - insets.right
        val ec = getLayoutComponent(parent, BorderLayout.EAST)
        if (Objects.nonNull(ec)) {
          val d = ec.getPreferredSize()
          val vsb = scroll.getVerticalScrollBar()
          val vsw = if (vsb.isVisible()) vsb.getSize().width else 0
          ec.setBounds(right - d.width - vsw, top, d.width, bottom - top)
        }
        val cc = getLayoutComponent(parent, BorderLayout.CENTER)
        if (Objects.nonNull(cc)) {
          cc.setBounds(left, top, right - left, bottom - top)
        }
      }
    }
  })
  p.add(minimap, BorderLayout.EAST)
  p.add(scroll)
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.setPreferredSize(Dimension(320, 240))
  }
}

fun loadFile(path: String) {
  val html = runCatching {
    File(path)
      .useLines { it.toList() }
      .map { it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") }
      .joinToString("\n")
  }.fold(
    onSuccess = { prettify(engine, it) },
    onFailure = { it.message }
  )
  editor.setText("<pre>$html\n</pre>")
  editor.moveCaretPosition(0)
}

fun createEngine(): ScriptEngine? {
  val engine = ScriptEngineManager().getEngineByName("JavaScript")
  val cl = Thread.currentThread().getContextClassLoader()
  val url = cl.getResource("example/prettify.js")
  return runCatching {
    BufferedReader(InputStreamReader(url.openStream(), StandardCharsets.UTF_8)).use { r ->
      engine.eval("var window={}, navigator=null;")
      engine.eval(r)
    }
    engine
  }.onFailure { it.printStackTrace() }.getOrNull()
}

fun prettify(engine: ScriptEngine?, src: String) = runCatching {
  (engine as? Invocable)?.invokeMethod(engine.get("window"), "prettyPrintOne", src) as? String
}.getOrNull() ?: "error"

private fun makeMiniMap(c: Component): Icon {
  val d = c.getSize()
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
