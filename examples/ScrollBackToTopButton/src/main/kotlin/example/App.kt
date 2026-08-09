package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.LayerUI

fun createUI(): Component {
  val textArea = JTextArea()
  textArea.text = "1111111111111\n".repeat(2000)
  val scroll1 = JScrollPane(textArea)
  scroll1.setRowHeaderView(LineNumberView(textArea))
  textArea.border = BorderFactory.createEmptyBorder(0, 2, 0, 0)

  val table = JTable(500, 3)
  val scroll2 = JScrollPane(table)
  SwingUtilities.invokeLater {
    val max = table.getRowCount() - 1
    table.scrollRectToVisible(table.getCellRect(max, 0, true))
  }

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

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.translate(x, y)
    if (c is AbstractButton && c.model.isRollover) {
      g2.paint = rolloverColor
    } else {
      g2.paint = arrowColor
    }
    val centerX = getIconWidth() / 2f
    val centerY = getIconHeight() / 2f
    val arrowHalfWidth = centerX / 3f
    val arrowHalfHeight = centerY / 6f
    g2.stroke = BasicStroke(centerX / 2f)
    val arrow = Path2D.Float()
    arrow.moveTo(
      (centerX - arrowHalfWidth).toDouble(),
      (centerY + arrowHalfHeight).toDouble(),
    )
    arrow.lineTo(centerX.toDouble(), (centerY - arrowHalfHeight).toDouble())
    arrow.lineTo(
      (centerX + arrowHalfWidth).toDouble(),
      (centerY + arrowHalfHeight).toDouble(),
    )
    g2.draw(arrow)
    g2.dispose()
  }

  override fun getIconWidth() = 32

  override fun getIconHeight() = 32
}

private class ScrollBackToTopLayerUI<V : JScrollPane> : LayerUI<V>() {
  private val rubberStamp = JPanel()
  private val mousePoint = Point()
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

  override fun updateUI(l: JLayer<out V>) {
    super.updateUI(l)
    SwingUtilities.updateComponentTreeUI(button)
  }

  private fun updateButtonRect(scroll: JScrollPane) {
    val viewport = scroll.viewport
    val x = viewport.x + viewport.width - buttonRect.width - GAP
    val y = viewport.y + viewport.height - buttonRect.height - GAP
    buttonRect.setLocation(x, y)
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    val scroll = (c as? JLayer<*>)?.view as? JScrollPane ?: return
    updateButtonRect(scroll)
    if (scroll.viewport.viewRect.y > 0) {
      button.model.isRollover = buttonRect.contains(mousePoint)
      SwingUtilities.paintComponent(g, button, rubberStamp, buttonRect)
    }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask =
        AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
      c.glassPane.cursor = Cursor.getDefaultCursor()
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
    val scroll = l.view
    val viewRect = scroll.viewport.viewRect
    val pt = SwingUtilities.convertPoint(e.component, e.point, scroll)
    mousePoint.location = pt
    val eventId = e.id
    val rollover = buttonRect.contains(mousePoint)
    if (eventId == MouseEvent.MOUSE_CLICKED) {
      if (rollover) {
        scrollBackToTop(l.view)
      }
    } else if (eventId == MouseEvent.MOUSE_PRESSED && viewRect.y > 0 && rollover) {
      e.consume()
    }
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
    val pt = SwingUtilities.convertPoint(e.component, e.point, l.view)
    mousePoint.location = pt
    l.glassPane.isVisible = buttonRect.contains(mousePoint)
    l.repaint(buttonRect)
  }

  private fun scrollBackToTop(scroll: JScrollPane) {
    val view = scroll.viewport.view as? JComponent ?: return
    val target = scroll.viewport.viewRect
    Timer(20) { e ->
      (e.source as? Timer)?.also {
        if (0 < target.y && it.isRunning) {
          target.y -= 1.coerceAtLeast(target.y / 2)
          view.scrollRectToVisible(target)
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

private class LineNumberView(
  private val textArea: JTextArea,
) : JComponent() {
  init {
    val dl = object : DocumentListener {
      override fun insertUpdate(e: DocumentEvent) {
        repaint()
      }

      override fun removeUpdate(e: DocumentEvent) {
        repaint()
      }

      override fun changedUpdate(e: DocumentEvent) {
        // not needed
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
  }

  override fun updateUI() {
    super.updateUI()
    setOpaque(true)
    EventQueue.invokeLater {
      val i = textArea.insets
      border = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY),
        BorderFactory.createEmptyBorder(i.top, MARGIN, i.bottom, MARGIN - 1),
      )
      isOpaque = true
      background = Color.WHITE
      font = textArea.font
    }
  }

  private fun getComponentWidth(): Int {
    val maxDigits = 3.coerceAtLeast(textArea.lineCount.toString().length)
    val fontMetrics = textArea.getFontMetrics(textArea.font)
    return maxDigits * fontMetrics.stringWidth("0") + insets.left + insets.right
  }

  private fun getLineAtPoint(y: Int): Int {
    val root = textArea.document.defaultRootElement
    val pos = textArea.viewToModel(Point(0, y))
    // Java 9: val pos = textArea.viewToModel2D(Point(0, y))
    return root.getElementIndex(pos)
  }

  override fun getPreferredSize() = Dimension(
    getComponentWidth(),
    textArea.height,
  )

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.color = textArea.getBackground()
    val clip = g2.clipBounds
    g2.fillRect(clip.x, clip.y, clip.width, clip.height)

    val font = textArea.getFont()
    g2.font = font
    g2.color = getForeground()
    val startLine = getLineAtPoint(clip.y)
    val endLine = getLineAtPoint(clip.y + clip.height)
    val fontMetrics = g2.getFontMetrics(font)
    val fontAscent = fontMetrics.ascent
    val fontDescent = fontMetrics.descent
    val fontLeading = fontMetrics.leading
    var y = startLine * fontMetrics.height
    val rightMargin = getInsets().right
    for (line in startLine..endLine) {
      val text = (line + 1).toString()
      val x = getComponentWidth() - rightMargin - fontMetrics.stringWidth(text)
      y += fontAscent
      g2.drawString(text, x, y)
      y += fontDescent + fontLeading
    }
    g2.dispose()
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
