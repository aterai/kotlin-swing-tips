package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet
import kotlin.math.roundToInt

private val editor = JEditorPane().also {
  it.selectedTextColor = null
  it.selectionColor = Color(0x64_88_AA_AA, true)
}
private val scroll = JScrollPane(editor)
private val label = MiniMapLabel(scroll)
private val check = JCheckBox("minimap", true)
private val p = object : JPanel() {
  override fun isOptimizedDrawingEnabled() = false
}

fun createUI(): Component {
  val htmlEditorKit = HTMLEditorKit()
  htmlEditorKit.styleSheet = createStyleSheet()

  editor.editorKit = htmlEditorKit
  editor.isEditable = false
  editor.background = Color(0xEE_EE_EE)
  editor.selectedTextColor = null
  editor.selectionColor = Color(0x64_88_AA_AA, true)
  editor.addPropertyChangeListener("page") { updateMiniMap() }
  editor.addComponentListener(object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
      // The HTML is reflowed when the editor width changes,
      // so the minimap image must be regenerated
      updateMiniMap()
    }
  })
  loadHtml()

  check.addActionListener { updateMiniMap() }

  val pp = JPanel(BorderLayout(0, 0))
  pp.add(label, BorderLayout.NORTH)

  val minimap = JScrollPane(pp)
  minimap.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  minimap.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  box.add(Box.createHorizontalGlue())
  box.add(check)

  val verticalScrollBar = scroll.getVerticalScrollBar()
  verticalScrollBar.model.addChangeListener { label.repaint() }

  p.layout = MiniMapLayout()
  p.add(minimap, BorderLayout.EAST)
  p.add(scroll)
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createStyleSheet(): StyleSheet {
  val styleSheet = StyleSheet().also {
    it.addRule(".str{color:#008800}")
    it.addRule(".kwd{color:#000088}")
    it.addRule(".com{color:#880000}")
    it.addRule(".typ{color:#660066}")
    it.addRule(".lit{color:#006666}")
    it.addRule(".pun{color:#666600}")
    it.addRule(".pln{color:#000000}")
    it.addRule(".tag{color:#000088}")
    it.addRule(".atn{color:#660066}")
    it.addRule(".atv{color:#008800}")
    it.addRule(".dec{color:#660066}")
  }
  return styleSheet
}

private fun loadHtml() {
  val cl = Thread.currentThread().contextClassLoader
  cl.getResource("example/test.html")?.also { url ->
    runCatching {
      editor.page = url
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(editor)
      editor.text = it.message
    }
  }
}

private fun updateMiniMap() {
  label.setIcon(if (check.isSelected) MiniMapLabel.createMiniMapIcon(editor) else null)
  p.rootPane.also {
    it.revalidate()
    it.repaint()
  }
}

private class MiniMapLabel(
  private val scroll: JScrollPane,
) : JLabel() {
  private var handler: MouseAdapter? = null

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
    val r = computeThumbRect()
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = THUMB_COLOR
    g2.fillRect(r.x, r.y, r.width, r.height)
    g2.color = THUMB_COLOR.darker()
    g2.drawRect(r.x, r.y, r.width - 1, r.height - 1)
    g2.dispose()
  }

  // Calculating the thumb rectangle shared
  // by paintComponent and processMiniMapMouseEvent
  private fun computeThumbRect(): Rectangle {
    val viewport = scroll.getViewport()
    val innerRect = SwingUtilities.calculateInnerArea(this, null)
    val thumbRect = Rectangle(innerRect)
    thumbRect.height = 0
    val viewHeight = viewport.view.height
    if (innerRect.height > 0 && viewHeight > 0) {
      // Scale factor from the editor (view) height to the minimap label height
      val sy = innerRect.getHeight() / viewHeight
      val viewY = viewport.getViewPosition().y
      val extent = viewport.extentSize.height
      val y = (viewY * sy).roundToInt()
      thumbRect.y += y
      thumbRect.height = ((viewY + extent) * sy).roundToInt() - y
    }
    return thumbRect
  }

  inner class MiniMapHandler : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      processMiniMapMouseEvent(e)
    }

    override fun mouseDragged(e: MouseEvent) {
      processMiniMapMouseEvent(e)
    }

    fun processMiniMapMouseEvent(e: MouseEvent) {
      val innerRect = SwingUtilities.calculateInnerArea(this@MiniMapLabel, null)
      if (innerRect.height > 0) {
        // Center the visible area (thumb) on the clicked position
        val m = scroll.getVerticalScrollBar().getModel()
        val range = m.maximum - m.minimum
        val y = (e.y - innerRect.y) * range / innerRect.height.toFloat()
        val value = m.minimum + (y - m.extent / 2f).roundToInt()
        m.value = value // Scroll main editor side

        // The display position of the minimap itself will also follow
        // the position where the thumb (window) can be seen.
        scrollRectToVisible(computeThumbRect())
      }
    }
  }

  companion object {
    private const val SCALE = .15
    private val THUMB_COLOR = Color(0x32_00_00_FF, true)

    fun createMiniMapIcon(c: Component): Icon {
      val size = c.size
      val width = maxOf(1, (size.width * SCALE).roundToInt())
      val height = maxOf(1, (size.height * SCALE).roundToInt())
      val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      val g2 = image.createGraphics()
      g2.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR,
      )
      g2.scale(SCALE, SCALE)
      c.print(g2)
      g2.dispose()
      return ImageIcon(image)
    }
  }
}

private class MiniMapLayout : BorderLayout(0, 0) {
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
      val c = getLayoutComponent(parent, CENTER)
      c?.setBounds(left, top, right - left, bottom - top)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
