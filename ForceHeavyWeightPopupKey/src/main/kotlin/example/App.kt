package example

import java.awt.*
import java.security.AccessController
import java.security.PrivilegedAction
import javax.swing.*

fun makeUI(): Component {
  val label = makeLabel("FORCE_HEAVYWEIGHT_POPUP", Color.PINK)
  ToolTipManager.sharedInstance().isLightWeightPopupEnabled = false

  val action = PrivilegedAction<Void> {
    runCatching {
      val clazz = Class.forName("javax.swing.ClientPropertyKey")
      val field = clazz.getDeclaredField("PopupFactory_FORCE_HEAVYWEIGHT_POPUP")
      field.isAccessible = true
      label.putClientProperty(field[null], true)
    }
    null
  }
  AccessController.doPrivileged(action)

  val glass = object : JPanel(BorderLayout()) {
    private val backgroundColor = Color(0x64_64_64_C8, true)

    override fun paintComponent(g: Graphics) {
      g.color = backgroundColor
      g.fillRect(0, 0, width, height)
      super.paintComponent(g)
    }
  }
  glass.isOpaque = false
  glass.add(makeLabel("Default: ToolTipText", Color.ORANGE), BorderLayout.WEST)
  glass.add(label, BorderLayout.EAST)
  glass.add(Box.createVerticalStrut(60), BorderLayout.SOUTH)

  return JPanel().also {
    EventQueue.invokeLater {
      it.rootPane.glassPane = glass
      it.rootPane.glassPane.isVisible = true
    }
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeLabel(title: String, color: Color) = JLabel(title).also {
  it.isOpaque = true
  it.background = color
  it.toolTipText = "1234567890"
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
