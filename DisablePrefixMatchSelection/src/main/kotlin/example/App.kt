package example

import java.awt.*
import javax.swing.*
import javax.swing.text.Position.Bias

fun makeUI(): Component {
  val model = makeModel()
  val list = object : JList<String>(model) {
    override fun getNextMatch(
      prefix: String,
      startIndex: Int,
      bias: Bias,
    ) = -1
  }
  return JPanel(GridLayout(1, 2, 5, 5)).also {
    it.add(makeTitledPanel("Default", JScrollPane(JList(model))))
    it.add(makeTitledPanel("Disable prefixMatchSelection", JScrollPane(list)))
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel() = DefaultListModel<String>().also {
  it.addElement("aaa aaa aaa aaa")
  it.addElement("abb bbb bbb bbb bbb bb bb")
  it.addElement("abc ccc ccc ccc")
  it.addElement("bbb bbb")
  it.addElement("ccc bbb")
  it.addElement("ddd ddd ddd ddd")
  it.addElement("eee eee eee eee eee")
  it.addElement("fff fff fff fff fff fff")
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
