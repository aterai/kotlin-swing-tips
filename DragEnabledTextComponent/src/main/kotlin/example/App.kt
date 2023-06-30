package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val textField0 = JTextField("Initially has BasicTextUI\$TextTransferHandler")
  textField0.name = "default"

  val textField1 = JTextField("setEditable(false)")
  textField1.isEditable = false

  val textField2 = JTextField("setEnabled(false)")
  textField2.isEnabled = false

  val textField3 = JTextField("setTransferHandler(null)")
  textField3.transferHandler = null

  val textField4 = JTextField("setDropTarget(null)")
  textField4.dropTarget = null

  val textField5 = JTextField("TransferHandler#canImport(...): false")
  textField5.transferHandler = object : TransferHandler() {
    override fun canImport(info: TransferSupport) = false
  }

  EventQueue.invokeLater {
    (textField0.topLevelAncestor as? JFrame)?.also {
      it.transferHandler = object : TransferHandler() {
        override fun canImport(info: TransferSupport) = true

        override fun importData(support: TransferSupport) = true
      }
    }
  }

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  listOf(textField0, textField1, textField2, textField3, textField4, textField5).forEach {
    it.dragEnabled = true
    box.add(it)
    box.add(Box.createVerticalStrut(10))
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
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
