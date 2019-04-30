package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.io.File
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.filechooser.FileSystemView
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellEditor
import javax.swing.tree.TreeCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  init {

    val fileSystemView = FileSystemView.getFileSystemView()
    val root = DefaultMutableTreeNode()
    val treeModel = DefaultTreeModel(root)
    fileSystemView.getRoots().forEach { fileSystemRoot ->
      val node = DefaultMutableTreeNode(CheckBoxNode(fileSystemRoot, Status.DESELECTED))
      root.add(node)
      fileSystemView.getFiles(fileSystemRoot, true)
        .filter { it.isDirectory() }
        .map { CheckBoxNode(it, Status.DESELECTED) }
        .map { DefaultMutableTreeNode(it) }
        .forEach { node.add(it) }
    }
    treeModel.addTreeModelListener(CheckBoxStatusUpdateListener())

    val tree = object : JTree(treeModel) {
      override fun updateUI() {
        setCellRenderer(null)
        setCellEditor(null)
        super.updateUI()
        setCellRenderer(FileTreeCellRenderer(fileSystemView))
        setCellEditor(CheckBoxNodeEditor(fileSystemView))
      }
    }
    tree.setRootVisible(false)
    tree.addTreeSelectionListener(FolderSelectionListener(fileSystemView))

    tree.setEditable(true)
    tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4))

    tree.expandRow(0)

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(JScrollPane(tree))

    setPreferredSize(Dimension(320, 240))
  }

  companion object {

    @JvmStatic
    fun main(args: Array<String>) {
      EventQueue.invokeLater(object : Runnable {
        override fun run() {
          createAndShowGui()
        }
      })
    }

    fun createAndShowGui() {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      } catch (ex: ClassNotFoundException) {
        ex.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
      } catch (ex: InstantiationException) {
        ex.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
      } catch (ex: IllegalAccessException) {
        ex.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
      } catch (ex: UnsupportedLookAndFeelException) {
        ex.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
      }

      val frame = JFrame("@title@")
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
      // frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.getContentPane().add(MainPanel())
      frame.pack()
      frame.setLocationRelativeTo(null)
      frame.setVisible(true)
    }
  }
}

internal class TriStateCheckBox : JCheckBox() {
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

internal class IndeterminateIcon : Icon {
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
    private val FOREGROUND = Color(50, 20, 255, 200)
    private const val SIDE_MARGIN = 4
    private const val HEIGHT = 2
  }
}

enum class Status {
  SELECTED, DESELECTED, INDETERMINATE
}

class CheckBoxNode {
  private val file: File
  private val status: Status

  constructor(file: File) {
    this.file = file
    this.status = Status.INDETERMINATE
  }

  constructor(file: File, status: Status) {
    this.file = file
    this.status = status
  }

  fun getFile() = file

  fun getStatus() = status

  override fun toString() = file.getName()
}

internal class FileTreeCellRenderer(private val fileSystemView: FileSystemView) : TreeCellRenderer {
  private val checkBox = TriStateCheckBox().also { it.setOpaque(false) }
  private val renderer = DefaultTreeCellRenderer()
  private val panel = JPanel(BorderLayout()).also {
    it.setFocusable(false)
    it.setRequestFocusEnabled(false)
    it.setOpaque(false)
    it.add(checkBox, BorderLayout.WEST)
  }

  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    val c = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    val l = c as? JLabel ?: return c
    l.setFont(tree.getFont())
    return if (value is DefaultMutableTreeNode) {
      checkBox.setEnabled(tree.isEnabled())
      checkBox.setFont(tree.getFont())
      (value.getUserObject() as? CheckBoxNode)?.also {
        checkBox.setIcon(if (it.getStatus() == Status.INDETERMINATE) IndeterminateIcon() else null)
        val file = it.getFile()
        l.setIcon(fileSystemView.getSystemIcon(file))
        l.setText(fileSystemView.getSystemDisplayName(file))
        l.setToolTipText(file.getPath())
        checkBox.setSelected(it.getStatus() == Status.SELECTED)
      }
      panel.add(l)
      panel
    } else l
  }
}

internal class CheckBoxNodeEditor(val fileSystemView: FileSystemView) : AbstractCellEditor(), TreeCellEditor {
  private val checkBox = TriStateCheckBox().also {
    it.setOpaque(false)
    it.setFocusable(false)
    it.addActionListener { stopCellEditing() }
  }
  private val renderer = DefaultTreeCellRenderer()
  private val panel = JPanel(BorderLayout()).also {
    it.setFocusable(false)
    it.setRequestFocusEnabled(false)
    it.setOpaque(false)
    it.add(checkBox, BorderLayout.WEST)
  }
  private var file: File? = null

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int
  ): Component {
    val c = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, false)
    val l = c as? JLabel ?: return c
    l.setFont(tree.getFont())
    return if (value is DefaultMutableTreeNode) {
      checkBox.setEnabled(tree.isEnabled())
      checkBox.setFont(tree.getFont())
      (value.getUserObject() as? CheckBoxNode)?.also {
        checkBox.setIcon(if (it.getStatus() == Status.INDETERMINATE) IndeterminateIcon() else null)
        file = it.getFile()
        l.setIcon(fileSystemView.getSystemIcon(file))
        l.setText(fileSystemView.getSystemDisplayName(file))
        checkBox.setSelected(it.getStatus() == Status.SELECTED)
      }
      panel.add(l)
      panel
    } else l
  }

  override fun getCellEditorValue(): Any {
    val f = file ?: File("")
    return CheckBoxNode(f, if (checkBox.isSelected()) Status.SELECTED else Status.DESELECTED)
  }

  override fun isCellEditable(e: EventObject): Boolean {
    if (e is MouseEvent && e.getSource() is JTree) {
      val p = e.getPoint()
      val tree = e.getSource() as JTree
      val path = tree.getPathForLocation(p.x, p.y)
      return tree.getPathBounds(path)?.let {
        it.width = checkBox.getPreferredSize().width
        it.contains(p)
      } ?: false
    }
    return false
  }
}

internal class FolderSelectionListener(val fileSystemView: FileSystemView) : TreeSelectionListener {
  override fun valueChanged(e: TreeSelectionEvent) {
    val pnode = e.getPath().getLastPathComponent() as DefaultMutableTreeNode
    if (!pnode.isLeaf()) {
      return
    }
    val check = pnode.getUserObject() as CheckBoxNode // ?: return
    val parent = check.getFile()
    if (!parent.isDirectory()) {
      return
    }

    val parentStatus = if (check.getStatus() == Status.SELECTED) Status.SELECTED else Status.DESELECTED
    val model = (e.getSource() as JTree).getModel() as DefaultTreeModel
    val worker = object : BackgroundTask(fileSystemView, parent) {
      protected override fun process(chunks: List<File>) {
        chunks.map { CheckBoxNode(it, parentStatus) }
          .map { DefaultMutableTreeNode(it) }
          .forEach { model.insertNodeInto(it, pnode, pnode.getChildCount()) }
      }
    }
    worker.execute()
  }
}

open class BackgroundTask(val fileSystemView: FileSystemView, val parent: File) : SwingWorker<String, File>() {
  override fun doInBackground(): String {
    fileSystemView.getFiles(parent, true)
      .filter { it.isDirectory() }
      .forEach { this.publish(it) }
    return "done"
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
