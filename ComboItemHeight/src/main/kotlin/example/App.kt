package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val p = Box.createVerticalBox()
  val items = arrayOf("JComboBox 11111:", "JComboBox 222:", "JComboBox 33:")
  val combo1 = object : JComboBox<String?>(items) {
    override fun updateUI() {
      super.updateUI()
      (getRenderer() as? Component)?.preferredSize = Dimension(0, 32)
    }
  }
  p.add(makeTitledPanel("setPreferredSize", combo1))
  p.add(Box.createVerticalStrut(5))

  val combo2 = JComboBox(items)
  combo2.renderer = object : DefaultListCellRenderer() {
    private var cellHeight = 0

    override fun getListCellRendererComponent(
      list: JList<*>,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      cellHeight = super.getPreferredSize()?.takeIf { index < 0 }?.height ?: 32
      return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
    }

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = cellHeight
      return d
    }
  }
  p.add(makeTitledPanel("getListCellRendererComponent", combo2))
  p.add(Box.createVerticalStrut(5))

  val combo3 = object : JComboBox<String?>(items) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      val r = getRenderer()
      setRenderer { list, value, index, isSelected, cellHasFocus ->
        r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
          if (index >= 0) {
            (it as? JLabel)?.text = "<html><table><td height='32'>$value"
          }
        }
      }
    }
  }
  p.add(makeTitledPanel("html", combo3))
  p.add(Box.createVerticalStrut(5))

  val combo4 = object : JComboBox<String?>(items) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      val r = getRenderer()
      setRenderer { list, value, index, isSelected, cellHasFocus ->
        r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
          (it as? JLabel)?.icon = if (index >= 0) H32Icon() else null
        }
      }
    }
  }
  p.add(makeTitledPanel("icon", combo4))
  p.add(Box.createVerticalStrut(5))

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class H32Icon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    // Empty icon
  }

  override fun getIconWidth() = 0

  override fun getIconHeight() = 32
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
