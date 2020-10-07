package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

const val TXT = "123456789012345678901234567890"
const val TD1 = "<td style='background-color:white;border-right:1px solid green;border-top:1px solid blue'>%s</td>"
const val TABLE_STYLE1 = "style='border-left:1px solid red;border-bottom:1px solid red;background-color:yellow'"
const val TABLE_CELL_PD1 = " cellspacing='0px' cellpadding='5px'"
const val TABLE_STYLE2 = "style='border-left:1px solid red;border-top:1px solid red;background-color:yellow'"
const val TABLE_CELL_PD2 = " cellspacing='0px' cellpadding='5px'"
const val TABLE_STYLE3 = "style='border:0px;background-color:red'"
const val TABLE_CELL_PD3 = " cellspacing='1px' cellpadding='5px'"

fun makeUI(): Component {
  val td01 = String.format(TD1, TXT)
  val td02 = String.format(TD1, TXT)
  val td03 = String.format(TD1, TXT)
  val html1 = "<html><table $TABLE_STYLE1$TABLE_CELL_PD1><tr>$td01</tr><tr>$td01</tr></table>"
  val html2 = "<html><table $TABLE_STYLE2$TABLE_CELL_PD2><tr>$td02</tr><tr>$td02</tr></table>"
  val html3 = "<html><table $TABLE_STYLE3$TABLE_CELL_PD3><tr>$td03</tr><tr>$td03</tr></table>"
  return JPanel().also {
    it.add(makeTitledPanel("border-left, border-bottom", JLabel(html1)))
    it.add(makeTitledPanel("border-left, border-top", JLabel(html2)))
    it.add(makeTitledPanel("cellspacing", JLabel(html3)))
    it.preferredSize = Dimension(320, 240)
  }
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
