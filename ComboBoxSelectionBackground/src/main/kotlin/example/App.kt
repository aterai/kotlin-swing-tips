package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.ComboPopup

fun makeUI(): Component {
  UIManager.put("ComboBox.selectionBackground", Color.PINK)
  UIManager.put("ComboBox.selectionForeground", Color.CYAN)
  val model = arrayOf("111", "2222", "33333")
  val combo0 = JComboBox(model)
  val combo1 = object : JComboBox<String>(model) {
    override fun updateUI() {
      super.updateUI()
      (getAccessibleContext().getAccessibleChild(0) as? ComboPopup)?.list?.also {
        it.selectionForeground = Color.WHITE
        it.selectionBackground = Color.ORANGE
      }
    }
  }
  val combo2 = object : JComboBox<String>(model) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      val renderer = getRenderer()
      setRenderer { list, value, index, isSelected, cellHasFocus ->
        renderer.getListCellRendererComponent(
          list,
          value,
          index,
          isSelected,
          cellHasFocus,
        ).also {
          if (isSelected) {
            it.foreground = Color.WHITE
            it.background = Color.ORANGE
          } else {
            it.foreground = Color.BLACK
            it.background = Color.WHITE
          }
        }
      }
    }
  }

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(makeTitledPanel("UIManager.put(ComboBox.selection*ground, ...)", combo0))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("ComboPopup.getList().setSelection*ground(...)", combo1))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("ListCellRenderer", combo2))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
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
