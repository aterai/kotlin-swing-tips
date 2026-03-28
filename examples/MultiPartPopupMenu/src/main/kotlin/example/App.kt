package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.plaf.basic.BasicPopupMenuUI

fun makeUI(): Component {
  val popup = createCustomPopup()
  val table = JTable(16, 3)
  table.setComponentPopupMenu(popup)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createCustomPopup(): JPopupMenu {
  val popup = object : JPopupMenu() {
    override fun updateUI() {
      setUI(TransparentPopupMenuUI())
      setOpaque(false)
    }
  }
  popup.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  popup.setLayout(BorderLayout(0, 8))
  val toolbar = RoundedToolBar(15)
  toolbar.isFloatable = false
  toolbar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5))
  listOf("📋", "💾", "🔍", "🔖", "🔋", "🔔").forEach {
    toolbar.add(createIconButton(it))
  }
  val menuPanel = RoundPanel(15)
  menuPanel.setLayout(BoxLayout(menuPanel, BoxLayout.Y_AXIS))
  menuPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  mutableListOf<String?>("Properties", "Rename", "Save", "Delete").forEach {
    menuPanel.add(createMenuButton(it, popup))
  }
  popup.add(toolbar, BorderLayout.NORTH)
  popup.add(menuPanel, BorderLayout.WEST)
  return popup
}

private fun createIconButton(text: String?): JButton {
  val button = JButton(text)
  button.setFocusPainted(false)
  button.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8))
  button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
  return button
}

private fun createMenuButton(text: String?, parent: JPopupMenu): JButton {
  val button = object : JButton(text) {
    override fun updateUI() {
      super.updateUI()
      setAlignmentX(LEFT_ALIGNMENT)
      setHorizontalAlignment(LEFT)
      setContentAreaFilled(false)
      setFocusPainted(false)
      setBorder(BorderFactory.createEmptyBorder(5, 11, 5, 50))
    }

    override fun getMaximumSize(): Dimension {
      val d = super.getMaximumSize()
      d.width = 150
      return d
    }
  }
  button.addMouseListener(object : MouseAdapter() {
    override fun mouseEntered(e: MouseEvent?) {
      button.setOpaque(true)
      button.setBackground(Color(200, 220, 255))
      button.repaint()
    }

    override fun mouseExited(e: MouseEvent?) {
      button.setOpaque(false)
      button.repaint()
    }

    override fun mousePressed(e: MouseEvent?) {
      parent.setVisible(false)
    }
  })
  return button
}

private class TransparentPopupMenuUI : BasicPopupMenuUI() {
  override fun getPopup(popup: JPopupMenu, x: Int, y: Int): Popup? {
    val p = super.getPopup(popup, x, y)
    if (p != null) {
      EventQueue.invokeLater {
        val w = SwingUtilities.getWindowAncestor(popup)
        if (w is Window) {
          val isHeavyWeight = w.type == Window.Type.POPUP
          val gc = w.graphicsConfiguration
          if (gc != null && gc.isTranslucencyCapable && isHeavyWeight) {
            w.setBackground(Color(0x0, true))
          }
        }
        val c = SwingUtilities.getUnwrappedParent(popup)
        if (c is JComponent) {
          c.setOpaque(false)
        }
      }
    }
    return p
  }
}

private class RoundPanel(
  private val radius: Int,
) : JPanel() {
  override fun updateUI() {
    super.updateUI()
    setOpaque(false)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val w = getWidth().toDouble()
    val h = getHeight().toDouble()
    g2.color = getBackground()
    val r = radius.toDouble()
    g2.fill(RoundRectangle2D.Double(0.0, 0.0, w, h, r, r))
    g2.color = Color.LIGHT_GRAY
    g2.draw(RoundRectangle2D.Double(0.0, 0.0, w - 1.0, h - 1.0, r, r))
    g2.dispose()
    super.paintComponent(g)
  }
}

private class RoundedToolBar(
  private val radius: Int,
) : JToolBar() {
  override fun updateUI() {
    super.updateUI()
    setOpaque(false)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val w = getWidth().toDouble()
    val h = getHeight().toDouble()
    g2.color = getBackground()
    val r = radius.toDouble()
    g2.fill(RoundRectangle2D.Double(0.0, 0.0, w, h, r, r))
    g2.color = Color.LIGHT_GRAY
    g2.draw(RoundRectangle2D.Double(0.0, 0.0, w - 1.0, h - 1.0, r, r))
    g2.dispose()
    super.paintComponent(g)
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
