package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tds1 = "border-right:1px solid green;border-top:1px solid blue"
  val tbs1 = "border-left:1px solid red;border-bottom:1px solid red;background:yellow"
  val padding1 = "cellspacing='0px' cellpadding='5px'"
  val html1 = makeHtml(tbs1, padding1, tds1)

  val tds2 = "border-right:1px solid red;border-bottom:1px solid blue"
  val tbs2 = "border-left:1px solid red;border-top:1px solid red;background:yellow"
  val padding2 = "cellspacing='0px' cellpadding='5px'"
  val html2 = makeHtml(tbs2, padding2, tds2)

  val style3 = "border:0px;background:red"
  val padding3 = "cellspacing='1px' cellpadding='5px'"
  val html3 = makeHtml(style3, padding3, "")

  return JPanel().also {
    it.add(makeTitledPanel("border-left, border-bottom", JLabel(html1)))
    it.add(makeTitledPanel("border-left, border-top", JLabel(html2)))
    it.add(makeTitledPanel("cellspacing", JLabel(html3)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeHtml(tbs: String, padding: String, tds: String): String {
  val txt = "123456789012345678901234567890"
  val tr = "<tr><td style='background:white;$tds'>$txt</td></tr>"
  return "<html><table style='$tbs' $padding>$tr$tr</table>"
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
