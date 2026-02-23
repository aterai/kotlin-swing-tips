package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.WindowConstants
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import kotlin.math.max

fun makeUI(): Component {
  val list = listOf(
    "-Duser.country=JP",
    "-Duser.language=ja",
    "-Dfile.encoding=UTF-8",
    "--add-opens=java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED",
    "--add-opens=java.desktop/javax.swing.plaf.basic=ALL-UNNAMED",
    "--add-opens=java.desktop/sun.awt.shell=ALL-UNNAMED",
  )
  val expandableField = ExpandableTextField()
  expandableField.setText(list.joinToString(" "))
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
  private val popup = object : JPopupMenu() {
    private var listener: PopupMenuListener? = null

    override fun updateUI() {
      removePopupMenuListener(listener)
      super.updateUI()
      val footer = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
      footer.add(makeResizeLabel(this))
      setLayout(BorderLayout())
      add(JScrollPane(textArea))
      add(footer, BorderLayout.SOUTH)
      listener = ExpandPopupMenuListener()
      addPopupMenuListener(listener)
    }
  }

  init {
    val icon = ExpandArrowIcon()
    val expandBtn = makeExpandButton(icon)
    expandBtn.addActionListener { popup.show(textField, 0, 0) }
    add(expandBtn)
    textField.setMargin(Insets(0, 0, 0, icon.getIconWidth()))
    textField.setAlignmentX(RIGHT_ALIGNMENT)
    textField.setAlignmentY(CENTER_ALIGNMENT)
    add(textField)
  }

  private inner class ExpandPopupMenuListener : PopupMenuListener {
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      textArea.text = textField.getText().replace(" ", "\n") + "\n"
      textArea.requestFocusInWindow()
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      textField.text = textArea.getText().replace("\n", " ").trim()
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
      // not need
    }
  }

  fun setText(text: String) {
    textField.text = text
  }

  companion object {
    private fun makeResizeLabel(popup: JPopupMenu): JLabel {
      val resizeLabel = JLabel("◢")
      resizeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR))
      val resizer: MouseAdapter = Resizer(popup)
      resizeLabel.addMouseListener(resizer)
      resizeLabel.addMouseMotionListener(resizer)
      return resizeLabel
    }

    private fun makeExpandButton(icon: ExpandArrowIcon?): JButton {
      val expandBtn = JButton(icon)
      expandBtn.setMargin(Insets(0, 0, 0, 0))
      expandBtn.setContentAreaFilled(false)
      expandBtn.setFocusable(false)
      expandBtn.setBorderPainted(false)
      expandBtn.setAlignmentX(RIGHT_ALIGNMENT)
      expandBtn.setAlignmentY(CENTER_ALIGNMENT)
      return expandBtn
    }
  }
}

private class Resizer(
  private val popup: JPopupMenu,
) : MouseAdapter() {
  private val startPt = Point()
  private val startSize = Dimension()

  override fun mousePressed(e: MouseEvent) {
    startPt.location = e.locationOnScreen
    startSize.size = popup.size
  }

  override fun mouseDragged(e: MouseEvent) {
    val dx = e.locationOnScreen.x - startPt.x
    val dy = e.locationOnScreen.y - startPt.y
    val minWidth = popup.getInvoker().getWidth()
    val width = max(minWidth, startSize.width + dx)
    val height = max(100, startSize.height + dy)
    val newSize = Dimension(width, height)
    popup.preferredSize = newSize
    val w = SwingUtilities.getWindowAncestor(popup)
    if (w != null && w.type == Window.Type.POPUP) {
      w.setSize(newSize.width, newSize.height)
    } else {
      popup.pack()
    }
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
