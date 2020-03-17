package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val log = JTextArea()
    val check = JCheckBox("isMultiSelection")
    val button1 = JButton("Default")
    button1.addActionListener {
      val chooser = JFileChooser()
      chooser.setMultiSelectionEnabled(check.isSelected)
      val retValue = chooser.showOpenDialog(log.getRootPane())
      if (retValue == JFileChooser.APPROVE_OPTION) {
        log.setText(chooser.getSelectedFile().getAbsolutePath())
      }
    }

    val button2 = JButton("TransferHandler")
    button2.addActionListener {
      val chooser = JFileChooser()
      chooser.setMultiSelectionEnabled(check.isSelected)
      chooser.setTransferHandler(FileChooserTransferHandler())
      val retValue = chooser.showOpenDialog(log.getRootPane())
      if (retValue == JFileChooser.APPROVE_OPTION) {
        log.setText(chooser.getSelectedFile().getAbsolutePath())
      }
    }
    val p = JPanel()
    p.setBorder(BorderFactory.createTitledBorder("JFileChooser"))
    p.add(button1)
    p.add(button2)
    p.add(check)
    add(p, BorderLayout.NORTH)
    add(JScrollPane(log))
    setPreferredSize(Dimension(320, 240))
  }
}

class FileChooserTransferHandler : TransferHandler() {
  override fun canImport(support: TransferSupport) =
    support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) &&
    support.getComponent() is JFileChooser &&
    support.isDrop()

  override fun importData(support: TransferSupport) = runCatching {
    val fc = support.component as JFileChooser
    val list = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor) as List<*>
    val files = list.filterIsInstance<File>().toTypedArray()
    if (fc.isMultiSelectionEnabled()) {
      fc.setSelectedFiles(files)
    } else {
      val f = files.firstOrNull() ?: return false
      if (f.isDirectory()) {
        fc.setCurrentDirectory(f)
      } else {
        fc.setSelectedFile(f)
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
