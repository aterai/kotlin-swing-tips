package example

import com.sun.java.swing.plaf.windows.WindowsFileChooserUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.File
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.ComponentUI
import javax.swing.plaf.basic.BasicDirectoryModel
import javax.swing.plaf.metal.MetalFileChooserUI

class MainPanel : JPanel(BorderLayout()) {
  init {
    val log = JTextArea()

    val readOnlyButton = JButton("readOnly")
    readOnlyButton.addActionListener {
      UIManager.put("FileChooser.readOnly", true)
      val fileChooser = JFileChooser()
      val retValue = fileChooser.showOpenDialog(rootPane)
      if (retValue == JFileChooser.APPROVE_OPTION) {
        log.text = fileChooser.selectedFile.absolutePath
      }
    }

    val writableButton = JButton("Rename only File#canWrite() == true")
    writableButton.addActionListener {
      UIManager.put("FileChooser.readOnly", false)
      val fileChooser = object : JFileChooser() {
        override fun setUI(ui: ComponentUI) {
          if (ui is WindowsFileChooserUI) {
            super.setUI(WindowsCanWriteFileChooserUI.createUI(this))
          } else {
            super.setUI(MetalCanWriteFileChooserUI.createUI(this))
          }
        }
      }
      val retValue = fileChooser.showOpenDialog(rootPane)
      if (retValue == JFileChooser.APPROVE_OPTION) {
        log.setText(fileChooser.getSelectedFile().getAbsolutePath())
      }
    }
    val p = JPanel(GridLayout(2, 1, 5, 5))
    p.border = BorderFactory.createTitledBorder("JFileChooser")
    p.add(readOnlyButton)
    p.add(writableButton)
    add(p, BorderLayout.NORTH)
    add(JScrollPane(log))
    preferredSize = Dimension(320, 240)
  }
}

class WindowsCanWriteFileChooserUI(chooser: JFileChooser) : WindowsFileChooserUI(chooser) {
  private var model2: BasicDirectoryModel? = null

  override fun createModel() {
    model2?.invalidateFileCache()
    model2 = object : BasicDirectoryModel(fileChooser) {
      override fun renameFile(oldFile: File, newFile: File) =
        oldFile.canWrite() && super.renameFile(oldFile, newFile)
    }
  }

  override fun getModel() = model2

  companion object {
    fun createUI(c: JComponent?): ComponentUI {
      if (c is JFileChooser) {
        return WindowsCanWriteFileChooserUI(c)
      }
      throw InternalError("Should never happen")
    }
  }
}

class MetalCanWriteFileChooserUI(chooser: JFileChooser) : MetalFileChooserUI(chooser) {
  private var model2: BasicDirectoryModel? = null

  override fun createModel() {
    model2?.invalidateFileCache()
    model2 = object : BasicDirectoryModel(fileChooser) {
      override fun renameFile(oldFile: File, newFile: File) =
        oldFile.canWrite() && super.renameFile(oldFile, newFile)
    }
  }

  override fun getModel() = model2

  companion object {
    fun createUI(c: JComponent?): ComponentUI {
      if (c is JFileChooser) {
        return MetalCanWriteFileChooserUI(c)
      }
      throw InternalError("Should never happen")
    }
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
