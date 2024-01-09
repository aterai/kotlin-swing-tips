package example

import com.sun.java.swing.plaf.windows.WindowsComboBoxUI
import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.BasicArrowButton
import javax.swing.plaf.basic.BasicComboBoxUI

fun makeUI(): Component {
  val combo = object : JComboBox<String>(makeModel()) {
    private var listener: PopupMenuListener? = null

    override fun updateUI() {
      removePopupMenuListener(listener)
      super.updateUI()
      val ui2 = if (ui is WindowsComboBoxUI) {
        RightPopupWindowsComboBoxUI()
      } else {
        RightPopupBasicComboBoxUI()
      }
      setUI(ui2)
      listener = RightPopupMenuListener()
      addPopupMenuListener(listener)
    }
  }

  val p = JPanel(GridLayout(2, 2, 5, 5))
  p.add(JComboBox(makeModel()))
  p.add(JLabel("<- default"))
  p.add(combo)
  p.add(JLabel("<- RightPopupMenuListener"))

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel() = DefaultComboBoxModel<String>().also {
  it.addElement("111")
  it.addElement("2222")
  it.addElement("33333")
  it.addElement("444444")
  it.addElement("5555555")
  it.addElement("66666666")
}

private class RightPopupMenuListener : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    EventQueue.invokeLater {
      val combo = e.source as? JComboBox<*>
      val a = combo?.accessibleContext?.getAccessibleChild(0)
      if (a is JPopupMenu) {
        val p = Point(combo.size.width, 0)
        SwingUtilities.convertPointToScreen(p, combo)
        a.location = p
      }
    }
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    // not needed
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    // not needed
  }
}

private class RightPopupWindowsComboBoxUI : WindowsComboBoxUI() {
  override fun createArrowButton(): JButton {
    val cl = Thread.currentThread().contextClassLoader
    val url = cl.getResource("example/14x14.png")
    val icon = url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) }
      ?: UIManager.getIcon("html.missingImage")
    val button = object : JButton(icon) {
      override fun getPreferredSize() = Dimension(14, 14)
    }
    button.rolloverIcon = makeRolloverIcon(icon)
    button.isFocusPainted = false
    button.isContentAreaFilled = false
    return button
  }

  private fun makeRolloverIcon(srcIcon: Icon): Icon {
    val w = srcIcon.iconWidth
    val h = srcIcon.iconHeight
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    srcIcon.paintIcon(null, g2, 0, 0)
    val scaleFactors = floatArrayOf(1.2f, 1.2f, 1.2f, 1f)
    val offsets = floatArrayOf(0f, 0f, 0f, 0f)
    val op = RescaleOp(scaleFactors, offsets, g2.renderingHints)
    g2.dispose()
    return ImageIcon(op.filter(img, null))
  }
}

private class RightPopupBasicComboBoxUI : BasicComboBoxUI() {
  override fun createArrowButton(): JButton {
    val button = super.createArrowButton()
    if (button is BasicArrowButton) {
      button.direction = SwingConstants.EAST
    }
    return button
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
