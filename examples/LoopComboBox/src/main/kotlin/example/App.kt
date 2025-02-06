package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*

fun makeUI(): Component {
  val combo = JComboBox(makeModel())

  val up = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      (e.source as? JComboBox<*>)?.also {
        val i = it.selectedIndex
        val size = it.itemCount
        it.selectedIndex = (i - 1 + size) % size
      }
    }
  }

  val down = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      (e.source as? JComboBox<*>)?.also {
        val i = it.selectedIndex
        val size = it.itemCount
        it.selectedIndex = (i + 1) % size
      }
    }
  }

  val am = combo.actionMap
  am.put("loopUp", up)
  am.put("loopDown", down)

  val im = combo.inputMap
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "loopUp")
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "loopDown")

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("default:", JComboBox(makeModel())))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("loop:", combo))
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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

private fun makeModel(): ComboBoxModel<String> {
  val model = DefaultComboBoxModel<String>()
  for (i in 0..<10) {
    model.addElement("item: $i")
  }
  return model
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
