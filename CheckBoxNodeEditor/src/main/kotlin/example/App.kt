package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode

class MainPanel : JPanel(BorderLayout()) {
  init {
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
    val model = tree.getModel()
    (model.getRoot() as? DefaultMutableTreeNode)?.also { root ->
      root.breadthFirstEnumeration().toList()
        .filterIsInstance(DefaultMutableTreeNode::class.java)
        .forEach {
          val title = it.getUserObject()?.toString() ?: ""
          it.setUserObject(CheckBoxNode(title, Status.DESELECTED))
        }
    }

    model.addTreeModelListener(CheckBoxStatusUpdateListener())

    tree.setEditable(true)
    tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4))

    tree.expandRow(0)

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(JScrollPane(tree))
    setPreferredSize(Dimension(320, 240))
  }
}

open class TriStateCheckBox : JCheckBox() {
  override fun updateUI() {
    val currentIcon = getIcon()
    setIcon(null)
    super.updateUI()
    currentIcon?.also {
      setIcon(IndeterminateIcon())
    }
    setOpaque(false)
  }
}

class IndeterminateIcon : Icon {
  private val icon = UIManager.getIcon("CheckBox.icon")

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    icon.paintIcon(c, g2, 0, 0)
    g2.setPaint(FOREGROUND)
    g2.fillRect(SIDE_MARGIN, (getIconHeight() - HEIGHT) / 2, getIconWidth() - SIDE_MARGIN - SIDE_MARGIN, HEIGHT)
    g2.dispose()
  }

  override fun getIconWidth() = icon.getIconWidth()

  override fun getIconHeight() = icon.getIconHeight()

  companion object {
    private val FOREGROUND = Color(0xC8_32_14_FF.toInt(), true)
    private const val SIDE_MARGIN = 4
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
