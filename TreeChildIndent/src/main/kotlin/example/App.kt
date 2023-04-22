package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.IconUIResource

private const val EXPANDED_ICON_KEY = "Tree.expandedIcon"
private const val COLLAPSED_ICON_KEY = "Tree.collapsedIcon"

private val expandedIcon = JCheckBox(EXPANDED_ICON_KEY, true)
private val paintLines = JCheckBox("Tree.paintLines", true)

fun makeUI(): Component {
  val tree = JTree()
  tree.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  val lci = UIManager.getInt("Tree.leftChildIndent")
  val leftChildIndent = SpinnerNumberModel(lci, -32, 32, 1)
  val rci = UIManager.getInt("Tree.rightChildIndent")
  val rightChildIndent = SpinnerNumberModel(rci, -32, 32, 1)

  val box1 = Box.createHorizontalBox()
  box1.add(JLabel(" Tree.leftChildIndent:"))
  box1.add(JSpinner(leftChildIndent))
  box1.add(JLabel(" Tree.rightChildIndent:"))
  box1.add(JSpinner(rightChildIndent))

  val box2 = Box.createHorizontalBox()
  box2.add(Box.createHorizontalGlue())
  box2.add(paintLines)
  box2.add(expandedIcon)
  val update = JButton("update")
  box2.add(update)

  val emptyIcon = EmptyIcon()
  update.addActionListener {
    UIManager.put("Tree.leftChildIndent", leftChildIndent.number.toInt())
    UIManager.put("Tree.rightChildIndent", rightChildIndent.number.toInt())
    val ei: Icon
    val ci: Icon
    if (expandedIcon.isSelected) {
      val lnfDef = UIManager.getLookAndFeelDefaults()
      ei = lnfDef.getIcon(EXPANDED_ICON_KEY)
      ci = lnfDef.getIcon(COLLAPSED_ICON_KEY)
    } else {
      ei = emptyIcon
      ci = emptyIcon
    }
    UIManager.put(EXPANDED_ICON_KEY, IconUIResource(ei))
    UIManager.put(COLLAPSED_ICON_KEY, IconUIResource(ci))
    UIManager.put("Tree.paintLines", paintLines.isSelected)
    SwingUtilities.updateComponentTreeUI(tree.rootPane)
  }

  val p = JPanel(GridLayout(2, 1, 2, 2))
  p.add(box1)
  p.add(box2)

  return JPanel(BorderLayout(2, 2)).also {
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private class EmptyIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    // Empty icon
  }

  override fun getIconWidth() = 0

  override fun getIconHeight() = 0
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
