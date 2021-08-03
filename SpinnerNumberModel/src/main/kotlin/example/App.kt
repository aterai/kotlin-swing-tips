package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val list1 = listOf(
    SpinnerNumberModel(Byte.MAX_VALUE, 0.toByte(), Byte.MAX_VALUE, 1.toByte()),
    SpinnerNumberModel(Short.MAX_VALUE, 0.toShort(), Short.MAX_VALUE, 1.toShort()),
    SpinnerNumberModel(Int.MAX_VALUE, 0, Int.MAX_VALUE, 1),
    SpinnerNumberModel(Long.MAX_VALUE, 0L, Long.MAX_VALUE, 1L)
  )
  val list2 = listOf(
    SpinnerNumberModel(Byte.MAX_VALUE.toLong(), 0.toLong(), Byte.MAX_VALUE.toLong(), 1.toLong()),
    SpinnerNumberModel(Short.MAX_VALUE.toLong(), 0.toLong(), Short.MAX_VALUE.toLong(), 1.toLong()),
    SpinnerNumberModel(Int.MAX_VALUE.toLong(), 0.toLong(), Int.MAX_VALUE.toLong(), 1),
    SpinnerNumberModel(Long.MAX_VALUE, 0.toLong(), Long.MAX_VALUE, 1.toLong())
  )

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("Byte, Short, Integer, Long", makeJSpinnerListPanel(list1)))
  box.add(makeTitledPanel("Long.valueOf", makeJSpinnerListPanel(list2)))
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeJSpinnerListPanel(list: List<SpinnerNumberModel>): Box {
  val box = Box.createVerticalBox()
  list.map { JSpinner(it) }.forEach {
    box.add(it)
    box.add(Box.createVerticalStrut(2))
  }
  return box
}

private fun makeTitledPanel(title: String, c: Component): Component {
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
