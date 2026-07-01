package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet
import kotlin.math.roundToInt

private const val SCALE = .15

private val editor = JEditorPane().also {
  it.selectedTextColor = null
  it.selectionColor = Color(0x64_88_AA_AA, true)
}
private val scroll = JScrollPane(editor)
private val label = MiniMapLabel(scroll)
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
  loadHtml()

  val button = JCheckBox("minimap", true)
  button.addActionListener {
    toggleMiniMap(it)
  }

  val pp = JPanel(BorderLayout(0, 0))
  pp.add(label, BorderLayout.NORTH)

  val minimap = JScrollPane(pp)
  minimap.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  minimap.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  box.add(Box.createHorizontalGlue())
  box.add(button)

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
    editor.addPropertyChangeListener("page") {
      label.setIcon(createMiniMapImageIcon())
      label.rootPane.also {
        it.revalidate()
        it.repaint()
      }
    }
  }
}

private fun toggleMiniMap(e: ActionEvent) {
  val c = e.getSource() as? AbstractButton ?: return
  if (c.isSelected) {
    label.setIcon(createMiniMapImageIcon())
  } else {
    label.setIcon(null)
  }
  c.rootPane.also {
    it.revalidate()
    it.repaint()
  }
}

private fun createMiniMapImageIcon(): Icon {
  val d = editor.size
  val newW = (d.width * SCALE).toInt()
  val newH = (d.height * SCALE).toInt()
  val image = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
  val g2 = image.createGraphics()
  g2.setRenderingHint(
    RenderingHints.KEY_INTERPOLATION,
    RenderingHints.VALUE_INTERPOLATION_BILINEAR,
  )
  g2.scale(SCALE, SCALE)
  editor.paint(g2)
  g2.dispose()
  return ImageIcon(image)
}

private class MiniMapLabel(private val scroll: JScrollPane) : JLabel() {
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
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
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
    val er = viewport.view.bounds
    val cr = SwingUtilities.calculateInnerArea(this, null)
    val thumbRect = Rectangle(cr)
    if (cr.height > 0 && er.getHeight() > 0) {
      val sy = cr.getHeight() / er.getHeight()
      val at = AffineTransform.getScaleInstance(1.0, sy)
      val tr = Rectangle(viewport.bounds)
      tr.y = viewport.getViewPosition().y
      val r = at.createTransformedShape(tr).bounds
      thumbRect.y += r.y
      thumbRect.height = r.height
    } else {
      thumbRect.height = 0
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
      val pt = e.getPoint()
      val c = e.component
      val m = scroll.getVerticalScrollBar().getModel()
      val range = m.maximum - m.minimum
      val fv = (pt.y * range / c.getHeight().toFloat() - m.extent / 2f)
      m.value = fv.roundToInt() // Scroll main editor side

      if (c is JComponent) {
        // The display position of the minimap itself will also follow
        // the position where the thumb (window) can be seen.
        val thumbRect = computeThumbRect()
        c.scrollRectToVisible(thumbRect)
      }
    }
  }
  companion object {
    private val THUMB_COLOR = Color(0x32_00_00_FF, true)
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
