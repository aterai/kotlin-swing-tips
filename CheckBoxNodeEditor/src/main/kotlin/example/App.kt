package example

import java.awt.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode

private fun makeUI(): Component {
  val tree = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      setCellEditor(null)
      super.updateUI()
      // ???#1: JDK 1.6.0 bug??? Nimbus LnF
      setCellRenderer(CheckBoxNodeRenderer())
      setCellEditor(CheckBoxNodeEditor())
    }
  }
  val model = tree.model
  (model.root as? DefaultMutableTreeNode)?.also { root ->
    root.breadthFirstEnumeration().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .forEach {
        val title = it.userObject?.toString() ?: ""
        it.userObject = CheckBoxNode(title, Status.DESELECTED)
      }
  }

  model.addTreeModelListener(CheckBoxStatusUpdateListener())

  tree.isEditable = true
  tree.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)

  tree.expandRow(0)

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

open class TriStateCheckBox : JCheckBox() {
  override fun updateUI() {
    val currentIcon = icon
    icon = null
    super.updateUI()
    currentIcon?.also {
      icon = IndeterminateIcon()
    }
    isOpaque = false
  }
}

class IndeterminateIcon : Icon {
  private val icon = UIManager.getIcon("CheckBox.icon")

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    icon.paintIcon(c, g2, 0, 0)
    g2.paint = FOREGROUND
    g2.fillRect(MARGIN, (iconHeight - HEIGHT) / 2, iconWidth - MARGIN - MARGIN, HEIGHT)
    g2.dispose()
  }

  override fun getIconWidth() = icon.iconWidth

  override fun getIconHeight() = icon.iconHeight

  companion object {
    private val FOREGROUND = Color(0xC8_32_14_FF.toInt(), true)
    private const val MARGIN = 4
    private const val HEIGHT = 2
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
