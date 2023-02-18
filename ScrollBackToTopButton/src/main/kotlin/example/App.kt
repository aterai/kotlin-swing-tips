package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val textArea = JTextArea()
  textArea.text = "1111111111111\n".repeat(2000)
  val scroll1 = JScrollPane(textArea)
  scroll1.setRowHeaderView(LineNumberView(textArea))
  textArea.border = BorderFactory.createEmptyBorder(0, 2, 0, 0)

  val table = object : JTable(500, 3) {
    override fun updateUI() {
      val reset = ColorUIResource(Color.RED)
      setSelectionForeground(reset)
      setSelectionBackground(reset)
      super.updateUI()
      val showGrid = UIManager.getLookAndFeelDefaults().get("Table.showGrid")
      setShowGrid(showGrid as? Boolean ?: true)
    }
  }
  val scroll2 = JScrollPane(table)
  SwingUtilities.invokeLater { table.scrollRectToVisible(table.getCellRect(500, 0, true)) }

  val tabbedPane = JTabbedPane().also {
    it.addTab("JTextArea", JLayer(scroll1, ScrollBackToTopLayerUI()))
    it.addTab("JTable", JLayer(scroll2, ScrollBackToTopLayerUI()))
  }

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ScrollBackToTopIcon : Icon {
  private val rolloverColor = Color(0xAA_FF_AF_64.toInt(), true)
  private val arrowColor = Color(0xAA_64_64_64.toInt(), true)
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.translate(x, y)
    if (c is AbstractButton && c.model.isRollover) {
      g2.paint = rolloverColor
    } else {
      g2.paint = arrowColor
    }
    val w2 = iconWidth / 2.0
    val h2 = iconHeight / 2.0
    val tw = w2 / 3.0
    val th = h2 / 6.0
    g2.stroke = BasicStroke(w2.toFloat() / 2f)
    val p = Path2D.Float()
    p.moveTo(w2 - tw, h2 + th)
    p.lineTo(w2, h2 - th)
    p.lineTo(w2 + tw, h2 + th)
    g2.draw(p)
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 32
}

private class ScrollBackToTopLayerUI<V : JScrollPane> : LayerUI<V>() {
  private val rubberStamp = JPanel()
  private val mousePt = Point()
  private val button = object : JButton(ScrollBackToTopIcon()) {
    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createEmptyBorder()
      isFocusPainted = false
      isBorderPainted = false
      isContentAreaFilled = false
      isRolloverEnabled = false
    }
  }
  private val buttonRect = Rectangle(button.preferredSize)

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val scroll = (c as? JLayer<*>)?.view as? JScrollPane ?: return
    updateButtonRect(scroll)
    if (scroll.viewport.viewRect.y > 0) {
      button.model.isRollover = buttonRect.contains(mousePt)
      SwingUtilities.paintComponent(g, button, rubberStamp, buttonRect)
    }
  }

  override fun updateUI(l: JLayer<out V>) {
    super.updateUI(l)
    SwingUtilities.updateComponentTreeUI(button)
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
      c.glassPane.cursor = Cursor.getDefaultCursor()
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out V>) {
    val scroll = l.view
    val r = scroll.viewport.viewRect
    val p = SwingUtilities.convertPoint(e.component, e.point, scroll)
    mousePt.location = p
    val id = e.id
    if (id == MouseEvent.MOUSE_CLICKED) {
      if (buttonRect.contains(mousePt)) {
        scrollBackToTop(l.view)
      }
    } else if (id == MouseEvent.MOUSE_PRESSED && r.y > 0 && buttonRect.contains(mousePt)) {
      e.consume()
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out V>) {
    val p = SwingUtilities.convertPoint(e.component, e.point, l.view)
    mousePt.location = p
    l.glassPane.isVisible = buttonRect.contains(mousePt)
    l.repaint(buttonRect)
  }

  private fun updateButtonRect(scroll: JScrollPane) {
    val viewport = scroll.viewport
    val x = viewport.x + viewport.width - buttonRect.width - GAP
    val y = viewport.y + viewport.height - buttonRect.height - GAP
    buttonRect.setLocation(x, y)
  }

  private fun scrollBackToTop(scroll: JScrollPane) {
    val c = scroll.viewport.view as? JComponent ?: return
    val current = scroll.viewport.viewRect
    Timer(20) { e ->
      (e.source as? Timer)?.also {
        if (0 < current.y && it.isRunning) {
          current.y -= 1.coerceAtLeast(current.y / 2)
          c.scrollRectToVisible(current)
        } else {
          it.stop()
        }
      }
    }.start()
  }

  companion object {
    private const val GAP = 5
  }
}

private class LineNumberView(private val textArea: JTextArea) : JComponent() {
  private val componentWidth: Int
    get() {
      val maxDigits = 3.coerceAtLeast(textArea.lineCount.toString().length)
      val fontMetrics = textArea.getFontMetrics(textArea.font)
      return maxDigits * fontMetrics.stringWidth("0") + insets.left + insets.right
    }

  init {
    val dl = object : DocumentListener {
      override fun insertUpdate(e: DocumentEvent) {
        repaint()
      }

      override fun removeUpdate(e: DocumentEvent) {
        repaint()
      }

      override fun changedUpdate(e: DocumentEvent) {
        /* not needed */
      }
    }
    textArea.document.addDocumentListener(dl)
    val cmpListener = object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        revalidate()
        repaint()
      }
    }
    textArea.addComponentListener(cmpListener)
    val i = textArea.insets
    border = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY),
      BorderFactory.createEmptyBorder(i.top, MARGIN, i.bottom, MARGIN - 1)
    )
    isOpaque = true
    background = Color.WHITE
    font = textArea.font
  }

  private fun getLineAtPoint(y: Int): Int {
    val root = textArea.document.defaultRootElement
    val pos = textArea.viewToModel(Point(0, y))
    // Java 9: val pos = textArea.viewToModel2D(Point(0, y))
    return root.getElementIndex(pos)
  }

  override fun updateUI() {
    super.updateUI()
    SwingUtilities.updateComponentTreeUI(textArea)
  }

  override fun getPreferredSize() = Dimension(componentWidth, textArea.height)

  override fun paintComponent(g: Graphics) {
    g.color = background
    val clip = g.clipBounds
    g.fillRect(clip.x, clip.y, clip.width, clip.height)
    val fontMetrics = textArea.getFontMetrics(textArea.font)
    val fontHeight = fontMetrics.height
    val fontAscent = fontMetrics.ascent
    val fontDescent = fontMetrics.descent
    val fontLeading = fontMetrics.leading
    g.color = foreground
    val base = clip.y
    val start = getLineAtPoint(base)
    val end = getLineAtPoint(base + clip.height)
    var y = start * fontHeight
    val rmg = insets.right
    for (i in start..end) {
      val text = (i + 1).toString()
      val x = componentWidth - rmg - fontMetrics.stringWidth(text)
      y += fontAscent
      g.drawString(text, x, y)
      y += fontDescent + fontLeading
    }
  }

  companion object {
    private const val MARGIN = 5
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
