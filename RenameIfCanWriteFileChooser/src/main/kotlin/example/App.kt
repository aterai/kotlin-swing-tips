package example

import com.sun.java.swing.plaf.windows.WindowsFileChooserUI
import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.plaf.ComponentUI
import javax.swing.plaf.basic.BasicDirectoryModel
import javax.swing.plaf.metal.MetalFileChooserUI

fun makeUI(): Component {
  val log = JTextArea()

  val readOnlyButton = JButton("readOnly")
  readOnlyButton.addActionListener {
    UIManager.put("FileChooser.readOnly", true)
    val fileChooser = JFileChooser()
    val retValue = fileChooser.showOpenDialog(log.rootPane)
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
    val retValue = fileChooser.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      log.text = fileChooser.selectedFile.absolutePath
    }
  }
  val p = JPanel(GridLayout(2, 1, 5, 5))
  p.border = BorderFactory.createTitledBorder("JFileChooser")
  p.add(readOnlyButton)
  p.add(writableButton)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private class WindowsCanWriteFileChooserUI(chooser: JFileChooser) : WindowsFileChooserUI(chooser) {
  private var model2: BasicDirectoryModel? = null

  override fun createModel() {
    model2?.invalidateFileCache()
    model2 = object : BasicDirectoryModel(fileChooser) {
      override fun renameFile(
        oldFile: File,
        newFile: File,
      ) = oldFile.canWrite() && super.renameFile(oldFile, newFile)
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

private class MetalCanWriteFileChooserUI(chooser: JFileChooser) : MetalFileChooserUI(chooser) {
  private var model2: BasicDirectoryModel? = null

  override fun createModel() {
    model2?.invalidateFileCache()
    model2 = object : BasicDirectoryModel(fileChooser) {
      override fun renameFile(
        oldFile: File,
        newFile: File,
      ) = oldFile.canWrite() && super.renameFile(oldFile, newFile)
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
