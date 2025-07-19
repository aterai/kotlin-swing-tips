package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.TitledBorder

fun makeUI(): Component {
  val panel1 = object : JPanel() {
    private val label = JLabel()

    override fun getToolTipText(e: MouseEvent): String? {
      (border as? TitledBorder)?.also { titledBorder ->
        val i = titledBorder.getBorderInsets(this)
        val title = titledBorder.title
        label.font = titledBorder.titleFont
        label.text = title
        val size = label.preferredSize
        val labelX = i.left
        val labelY = 0
        val labelW = getSize().width - i.left - i.right
        val labelH = i.top
        if (size.width > labelW) {
          val r = Rectangle(labelX, labelY, labelW, labelH)
          return if (r.contains(e.point)) title else null
        }
      }
      return null // super.getToolTipText(e)
    }
  }
  val title1 = "Override JPanel#getToolTipText(...)"
  panel1.border = BorderFactory.createTitledBorder(title1)
  panel1.toolTipText = "JPanel: Sample ToolTipText"

  val panel2 = JPanel()
  val title2 = "Default TitledBorder on JPanel"
  panel2.border = BorderFactory.createTitledBorder(title2)
  panel2.toolTipText = "JPanel"

  return JPanel(GridLayout(1, 2, 5, 5)).also {
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.add(panel1)
    it.add(panel2)
    it.preferredSize = Dimension(320, 240)
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
