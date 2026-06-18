package example

import java.awt.*
import javax.swing.*

fun createUI(): Component {
  val list1 = listOf(
    SpinnerNumberModel(Byte.MAX_VALUE, 0.toByte(), Byte.MAX_VALUE, 1.toByte()),
    SpinnerNumberModel(Short.MAX_VALUE, 0.toShort(), Short.MAX_VALUE, 1.toShort()),
    SpinnerNumberModel(Int.MAX_VALUE, 0, Int.MAX_VALUE, 1),
    SpinnerNumberModel(Long.MAX_VALUE, 0L, Long.MAX_VALUE, 1L),
  )
  val list2 = listOf(
    SpinnerNumberModel(
      Byte.MAX_VALUE.toLong(),
      0.toLong(),
      Byte.MAX_VALUE.toLong(),
      1.toLong(),
    ),
    SpinnerNumberModel(
      Short.MAX_VALUE.toLong(),
      0.toLong(),
      Short.MAX_VALUE.toLong(),
      1.toLong(),
    ),
    SpinnerNumberModel(
      Int.MAX_VALUE.toLong(),
      0.toLong(),
      Int.MAX_VALUE.toLong(),
      1,
    ),
    SpinnerNumberModel(
      Long.MAX_VALUE,
      0.toLong(),
      Long.MAX_VALUE,
      1.toLong(),
    ),
  )

  val box = Box.createVerticalBox()
  val title1 = "Byte, Short, Integer, Long"
  box.add(createTitledPanel(title1, createSpinnerListPanel(list1)))
  val title2 = "Long.valueOf"
  box.add(createTitledPanel(title2, createSpinnerListPanel(list2)))
  box.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createSpinnerListPanel(list: List<SpinnerNumberModel>): Box {
  val box = Box.createVerticalBox()
  list.map { JSpinner(it) }.forEach {
    box.add(it)
    box.add(Box.createVerticalStrut(2))
  }
  return box
}

private fun createTitledPanel(
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
