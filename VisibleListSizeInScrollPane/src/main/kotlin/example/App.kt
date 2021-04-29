package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(GridLayout(2, 1)).also {
  it.add(makePanel(null, 4))
  it.add(makePanel("MMMMMMM", 4))
  it.preferredSize = Dimension(320, 240)
}

fun makePanel(prototypeValue: String?, visibleRowCount: Int): Component {
  val model1 = (0 until 20).map { it.toString() }.toTypedArray()
  val list1 = JList(model1)
  list1.visibleRowCount = visibleRowCount
  list1.prototypeCellValue = prototypeValue

  val model2 = arrayOf("looooooooooooooong")
  val list2 = JList(model2)
  list2.visibleRowCount = visibleRowCount
  list2.prototypeCellValue = prototypeValue

  val list3 = object : JList<String>(model2) {
    override fun getPreferredScrollableViewportSize(): Dimension {
      val d = super.getPreferredScrollableViewportSize()
      d.width = 60
      return d
    }
  }
  list3.visibleRowCount = visibleRowCount
  list3.prototypeCellValue = prototypeValue

  val p = JPanel(GridBagLayout())
  val c = GridBagConstraints()
  c.insets = Insets(2, 2, 0, 2)
  c.gridx = 0
  p.add(JScrollPane(list1), c)
  c.gridx = 1
  p.add(JScrollPane(list2), c)
  c.gridx = 2
  p.add(JScrollPane(list3), c)
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
