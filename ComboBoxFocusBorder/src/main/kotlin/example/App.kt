package example

import java.awt.*
import javax.swing.*

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

fun makeUI(): Component {
  val model = DefaultComboBoxModel<String>().also {
    it.addElement("11111")
    it.addElement("222")
    it.addElement("3")
  }

  val combo1 = JComboBox(model)
  combo1.isFocusable = false

  val combo2 = FocusComboBox(model)

  val combo3 = object : FocusComboBox<String>(model) {
    override fun paintBorder(g: Graphics) {
      super.paintBorder(g)
      if (isFocusOwner && !isPopupVisible && isWindowsLnF) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = Color.DARK_GRAY
        g2.drawRect(0, 0, width - 1, height - 1)
        g2.dispose()
      }
    }
  }

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  box.add(makeTitledPanel("default", JComboBox(model)))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("setFocusable(false)", combo1))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("setRenderer(...)", combo2))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("paintBorder(...)", combo3))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class FocusComboBox<E> constructor(model: ComboBoxModel<E>) : JComboBox<E>(model) {
  val isWindowsLnF
    get() = ui.javaClass.name.contains("WindowsComboBoxUI")

  override fun updateUI() {
    setRenderer(null)
    super.updateUI()
    if (isWindowsLnF) {
      val renderer = getRenderer()
      setRenderer { list, value, index, isSelected, cellHasFocus ->
        renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
          if (index < 0 && it is JComponent) {
            it.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
          }
        }
      }
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
