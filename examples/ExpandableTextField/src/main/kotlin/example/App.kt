package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.WindowConstants
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import kotlin.math.max

fun createUI(): Component {
  val options = listOf(
    "-Duser.country=JP",
    "-Duser.language=ja",
    "-Dfile.encoding=UTF-8",
    "--add-opens=java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED",
    "--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED",
    "--add-opens=java.desktop/sun.awt.shell=ALL-UNNAMED",
  )
  val expandableField = ExpandableTextField()
  expandableField.setText(options.joinToString(" "))
  val p = JPanel(FlowLayout(FlowLayout.CENTER, 5, 20))
  p.add(JLabel("VM Options:"))
  p.add(expandableField)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class OverlayLayoutPanel : JPanel() {
  override fun updateUI() {
    super.updateUI()
    setLayout(OverlayLayout(this))
  }

  override fun isOptimizedDrawingEnabled() = false
}

private class ExpandableTextField : OverlayLayoutPanel() {
  private val textField = JTextField(20)
  private val textArea = JTextArea()
  private val popup = ExpandPopupMenu()

  init {
    val icon = ExpandArrowIcon()
    val expandButton = createExpandButton(icon)
    expandButton.addActionListener { popup.show(textField, 0, 0) }
    add(expandButton)
    textField.setMargin(Insets(0, 0, 0, icon.getIconWidth()))
    textField.setAlignmentX(RIGHT_ALIGNMENT)
    textField.setAlignmentY(CENTER_ALIGNMENT)
    add(textField)
  }

  private inner class ExpandPopupMenu : JPopupMenu() {
    private var listener: PopupMenuListener? = null

    override fun updateUI() {
      removePopupMenuListener(listener)
      super.updateUI()
      removeAll() // Avoid leaving the previous components when the UI is updated again
      val footer = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
      footer.add(createResizeLabel(this))
      setLayout(BorderLayout())
      add(JScrollPane(textArea))
      add(footer, BorderLayout.SOUTH)
      listener = ExpandPopupMenuListener()
      addPopupMenuListener(listener)
    }

    // Keep the popup at least as wide as the text field, even for short text
    override fun getPreferredSize() = super.getPreferredSize().also {
      it.width = max(it.width, textField.width)
    }
  }

  private inner class ExpandPopupMenuListener : PopupMenuListener {
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      textArea.text = textField.getText().replace(" ", "\n") + "\n"
      // The popup is not showing yet, so request the focus after it is displayed
      EventQueue.invokeLater { textArea.requestFocusInWindow() }
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      textField.text = textArea.getText().replace("\n", " ").trim()
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
      // not needed
    }
  }

  fun setText(text: String) {
    textField.text = text
  }

  companion object {
    private fun createResizeLabel(popup: JPopupMenu): JLabel {
      val resizeLabel = JLabel("◢")
      resizeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR))
      val resizer = PopupMenuResizeHandler(popup)
      resizeLabel.addMouseListener(resizer)
      resizeLabel.addMouseMotionListener(resizer)
      return resizeLabel
    }

    private fun createExpandButton(icon: ExpandArrowIcon?): JButton {
      val expandButton = JButton(icon)
      expandButton.setMargin(Insets(0, 0, 0, 0))
      expandButton.setContentAreaFilled(false)
      expandButton.setFocusable(false)
      expandButton.setBorderPainted(false)
      expandButton.setAlignmentX(RIGHT_ALIGNMENT)
      expandButton.setAlignmentY(CENTER_ALIGNMENT)
      return expandButton
    }
  }
}

private class PopupMenuResizeHandler(
  private val popup: JPopupMenu,
) : MouseAdapter() {
  private val dragStartPoint = Point()
  private val dragStartSize = Dimension()

  override fun mousePressed(e: MouseEvent) {
    dragStartPoint.location = e.locationOnScreen
    dragStartSize.size = popup.size
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.locationOnScreen
    val minWidth = popup.getInvoker().getWidth()
    val width = max(minWidth, dragStartSize.width + pt.x - dragStartPoint.x)
    val height = max(MIN_HEIGHT, dragStartSize.height + pt.y - dragStartPoint.y)
    val size = Dimension(width, height)
    popup.preferredSize = size
    val window = SwingUtilities.getWindowAncestor(popup)
    if (window != null && window.type == Window.Type.POPUP) {
      // Popup$HeavyWeightWindow
      window.size = size
    } else {
      // Popup$LightWeightWindow
      popup.pack()
    }
  }

  companion object {
    private const val MIN_HEIGHT = 100
  }
}

private class ExpandArrowIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = c.getForeground()
    val cx = x + iconWidth / 2
    val cy = y + iconHeight / 2
    g2.rotate(Math.toRadians(45.0), cx.toDouble(), cy.toDouble())
    g2.font = c.getFont().deriveFont(14f)
    val fm = g2.fontMetrics
    val tx = cx - fm.stringWidth(ARROW) / 2
    val ty = cy + (fm.ascent - fm.descent) / 2
    g2.drawString(ARROW, tx, ty)
    g2.dispose()
  }

  override fun getIconWidth() = 18

  override fun getIconHeight() = 18

  companion object {
    private const val ARROW = "⇵" // "↕";
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
