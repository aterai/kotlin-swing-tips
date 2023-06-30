package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.*

fun makeUI(): Component {
  val log = JTextArea()
  val check = JCheckBox("isMultiSelection")
  val button1 = JButton("Default")
  button1.addActionListener {
    val chooser = JFileChooser()
    chooser.isMultiSelectionEnabled = check.isSelected
    val retValue = chooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = chooser.selectedFile.absolutePath
    }
  }

  val button2 = JButton("TransferHandler")
  button2.addActionListener {
    val chooser = JFileChooser()
    chooser.isMultiSelectionEnabled = check.isSelected
    chooser.transferHandler = FileChooserTransferHandler()
    val retValue = chooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = chooser.selectedFile.absolutePath
    }
  }
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("JFileChooser")
  p.add(button1)
  p.add(button2)
  p.add(check)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private class FileChooserTransferHandler : TransferHandler() {
  override fun canImport(support: TransferSupport) = support.isDrop &&
    support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) &&
    support.component is JFileChooser

  override fun importData(support: TransferSupport) = runCatching {
    val fc = support.component as? JFileChooser ?: return false
    val list = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
    val files = list?.filterIsInstance<File>()?.toTypedArray()
    if (fc.isMultiSelectionEnabled) {
      fc.selectedFiles = files
    } else {
      val f = files?.firstOrNull() ?: return false
      if (f.isDirectory) {
        fc.currentDirectory = f
      } else {
        fc.selectedFile = f
      }
    }
    true
  }.getOrNull() ?: false
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
