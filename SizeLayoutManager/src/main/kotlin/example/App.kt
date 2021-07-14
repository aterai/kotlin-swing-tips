package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports

private fun makeUI1(): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder("Override JToggleButton#getPreferredSize(...)")
  val c = GridBagConstraints()
  c.insets = Insets(5, 5, 5, 5)
  val al = ActionListener { p.revalidate() }
  val bg = ButtonGroup()
  listOf("a1", "a2", "a3")
    .forEach {
      val b = object : JToggleButton(it) {
        override fun getPreferredSize(): Dimension {
          val v = if (isSelected) 80 else 50
          return Dimension(v, v)
        }
      }
      b.addActionListener(al)
      bg.add(b)
      p.add(b, c)
    }
  return p
}

private fun makeUI2(): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder("Override FlowLayout#layoutContainer(...)")
  p.layout = object : FlowLayout() {
    override fun layoutContainer(target: Container) {
      synchronized(target.treeLock) {
        if (target.componentCount <= 0) {
          return
        }
        val insets = target.insets
        val rowHeight = target.height
        var x = insets.left + hgap
        target.components
          .filterIsInstance<AbstractButton>()
          .filter { it.isVisible }
          .forEach {
            val v = if (it.isSelected) 80 else 50
            val d = Dimension(v, v)
            it.size = d
            val y = (rowHeight - v) / 2
            it.setLocation(x, y)
            x += d.width + hgap
          }
      }
    }
  }

  val al = ActionListener { p.revalidate() }
  val bg = ButtonGroup()
  listOf("b1", "b2", "b3")
    .forEach {
      val b = JToggleButton(it)
      b.addActionListener(al)
      bg.add(b)
      p.add(b)
    }
  return p
}

fun makeUI() = JPanel(GridLayout(2, 1)).also {
  it.add(makeUI1())
  it.add(makeUI2())
  it.preferredSize = Dimension(320, 240)
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
