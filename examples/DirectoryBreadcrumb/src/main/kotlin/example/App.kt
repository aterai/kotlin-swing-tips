package example

import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.DosFileAttributes
import java.util.EnumSet
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.logging.Logger
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.filechooser.FileSystemView
import javax.swing.plaf.basic.BasicGraphicsUtils

private var worker: EntryLoader? = null

fun createUI(): Component {
  val home = Paths.get(System.getProperty("user.home"))
  val breadcrumb = DirectoryBreadcrumb(home)

  val model = DefaultListModel<DirEntry>()
  val list = JList(model)
  list.cellRenderer = DirEntryRenderer(null)
  // Fixed cell size: JList would otherwise call the renderer
  // for every item on each layout
  list.prototypeCellValue = DirEntry(home, true)
  list.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      // Ignore a double click on the empty area below the last item
      val index = list.locationToIndex(e.point)
      val r = list.getCellBounds(index, index)
      if (e.clickCount == 2 && r?.contains(e.point) == true) {
        val entry = list.model.getElementAt(index)
        if (entry.isDirectory) {
          breadcrumb.setCurrentDirectory(entry.path)
        }
      }
    }
  })
  breadcrumb.addPropertyChangeListener(DirectoryBreadcrumb.CURRENT_DIRECTORY) {
    updateList(model, it.newValue as Path)
  }
  updateList(model, breadcrumb.currentDirectory)

  return JPanel(BorderLayout()).also {
    it.add(breadcrumb, BorderLayout.NORTH)
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateList(model: DefaultListModel<DirEntry>, dir: Path) {
  worker?.cancel(true)
  model.clear()
  worker = EntryLoader(dir, model).also { it.execute() }
}

// The entries are created on a worker thread and published in chunks
// because FileSystemView#getSystemIcon(File) is slow on Windows
private class EntryLoader(
  private val dir: Path,
  private val model: DefaultListModel<DirEntry>,
) : SwingWorker<Unit, DirEntry>() {
  override fun doInBackground() {
    val attrs = DirEntry.listAttributes(dir)
    for (p in sortPaths(attrs)) {
      if (isCancelled) {
        break
      }
      val a = attrs.getValue(p)
      if (!DirEntry.isHidden(p, a)) {
        publish(DirEntry(p, a.isDirectory))
      }
    }
  }

  override fun process(chunks: List<DirEntry>) {
    if (!isCancelled) {
      chunks.forEach { model.addElement(it) }
    }
  }

  override fun done() {
    logError(this)
  }
}

// Directories first, then case-insensitive file names
private fun sortPaths(attrs: Map<Path, BasicFileAttributes>) = attrs.keys.sortedWith(
  compareBy<Path> { !attrs.getValue(it).isDirectory }
    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.fileName.toString() },
)

private fun logError(worker: SwingWorker<*, *>) {
  try {
    if (!worker.isCancelled) {
      worker.get()
    }
  } catch (ex: InterruptedException) {
    Thread.currentThread().interrupt()
  } catch (ex: ExecutionException) {
    Logger.getGlobal().warning { ex.message }
  }
}

// Breadcrumb bar: [Icon][>][PC][>][...][>][Dir][>]...[Current][>]
// Clicking on the empty area switches to a JTextField for editing the path.
class DirectoryBreadcrumb(
  dir: Path,
) : JPanel() {
  private val cards = CardLayout()
  private val crumbLayout = BreadcrumbLayout()
  private val crumbs = JPanel(crumbLayout)
  private val field = JTextField()

  // The root icon and the field icon show the icon of the current directory
  private val rootIcon = CrumbButton(null, null)
  private val fieldIcon = JLabel()
  private val placesSeparator = CrumbButton(null, ArrowIcon())
  private val rootLabel = CrumbButton("PC", null)
  private val ellipsis = CrumbButton("...", null)
  private val chain = mutableListOf<Path>()
  private var current: Path? = null

  val currentDirectory: Path
    get() = current ?: error("current directory is not set")

  init {
    setLayout(cards)
    rootIcon.addActionListener { startEditing() }
    placesSeparator.addPopupToggle {
      DirectoryPopupMenu.ofPlaces(current) { setCurrentDirectory(it) }
    }
    // The "PC" button lists the drives, like the separator next to it
    rootLabel.addPopupToggle {
      DirectoryPopupMenu.ofChildren(null, current) { setCurrentDirectory(it) }
    }
    ellipsis.toolTipText = "Hidden folders"
    ellipsis.addPopupToggle {
      DirectoryPopupMenu.ofPaths(chain.subList(0, crumbLayout.hiddenCount)) {
        setCurrentDirectory(it)
      }
    }

    crumbs.isOpaque = false
    crumbs.cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)
    crumbs.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        if (e.isPopupTrigger) {
          showContextMenu(e)
        }
      }

      override fun mouseReleased(e: MouseEvent) {
        if (e.isPopupTrigger) {
          showContextMenu(e)
        }
      }

      override fun mouseClicked(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) {
          startEditing()
        }
      }
    })

    // The icon at the left end of the JTextField is aligned with the root icon
    fieldIcon.border = BorderFactory.createEmptyBorder(2, 4, 2, 0)
    fieldIcon.cursor = Cursor.getDefaultCursor()
    field.border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
    field.addActionListener { commitEditing() }
    field.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel")
    field.actionMap.put(
      "cancel",
      object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
          stopEditing()
        }
      },
    )
    field.addFocusListener(object : FocusAdapter() {
      override fun focusLost(e: FocusEvent) {
        if (!e.isTemporary) {
          stopEditing()
        }
      }
    })

    val editor = JPanel(BorderLayout())
    editor.isOpaque = false
    editor.add(fieldIcon, BorderLayout.WEST)
    editor.add(field)

    add(crumbs, CARD_CRUMBS)
    add(editor, CARD_EDIT)
    initKeyBindings()
    setCurrentDirectory(dir)
  }

  override fun updateUI() {
    super.updateUI()
    isOpaque = true
    border = UIManager.getBorder("TextField.border")
    background = UIManager.getColor("TextField.background")
  }

  private fun initKeyBindings() {
    val im = getInputMap(WHEN_IN_FOCUSED_WINDOW)
    val am = actionMap
    im.put(KeyStroke.getKeyStroke("F4"), ACTION_EDIT)
    im.put(KeyStroke.getKeyStroke("alt D"), ACTION_EDIT)
    am.put(
      ACTION_EDIT,
      object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
          startEditing()
        }
      },
    )
    im.put(KeyStroke.getKeyStroke("alt UP"), "up")
    am.put(
      "up",
      object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
          current?.parent?.also { setCurrentDirectory(it) }
        }
      },
    )
  }

  // Falls back to the nearest existing ancestor when the directory does not exist
  fun setCurrentDirectory(dir: Path) {
    var p: Path? = dir.toAbsolutePath().normalize()
    while (p != null && !Files.isDirectory(p)) {
      p = p.parent
    }
    if (p == null || p == current) {
      return
    }
    val old = current
    current = p
    rebuild(p)
    firePropertyChange(CURRENT_DIRECTORY, old, p)
  }

  private fun rebuild(dir: Path) {
    chain.clear()
    var p: Path? = dir
    while (p != null) {
      chain.add(0, p)
      p = p.parent
    }
    val icon = DirEntry.getSystemIcon(dir)
    rootIcon.icon = icon
    fieldIcon.icon = icon
    crumbs.removeAll()
    crumbs.add(rootIcon)
    crumbs.add(placesSeparator)
    crumbs.add(rootLabel)
    crumbs.add(createSeparator(null))
    crumbs.add(ellipsis)
    for (path in chain) {
      crumbs.add(createDirButton(path, dir))
      crumbs.add(createSeparator(path))
    }
    crumbs.revalidate()
    crumbs.repaint()
  }

  private fun createDirButton(p: Path, dir: Path): AbstractButton {
    val b = CrumbButton(DirEntry.getDisplayName(p), null)
    if (p == dir) {
      b.font = b.font.deriveFont(Font.BOLD)
    }
    b.toolTipText = p.toString()
    b.addActionListener { setCurrentDirectory(p) }
    return b
  }

  // dir == null: lists the root directories (drives)
  private fun createSeparator(dir: Path?): AbstractButton {
    val b = CrumbButton(null, ArrowIcon())
    b.addPopupToggle {
      DirectoryPopupMenu.ofChildren(dir, current) { setCurrentDirectory(it) }
    }
    return b
  }

  private fun showContextMenu(e: MouseEvent) {
    val popup = JPopupMenu()
    popup.add("Copy address as text").addActionListener {
      val ss = StringSelection(currentDirectory.toString())
      Toolkit.getDefaultToolkit().systemClipboard.setContents(ss, ss)
    }
    popup.add("Edit address").addActionListener { startEditing() }
    popup.show(e.component, e.x, e.y)
  }

  fun startEditing() {
    field.text = currentDirectory.toString()
    cards.show(this, CARD_EDIT)
    field.requestFocusInWindow()
    field.selectAll()
  }

  private fun stopEditing() {
    cards.show(this, CARD_CRUMBS)
  }

  private fun commitEditing() {
    val p = parseInput(currentDirectory, field.text)
    if (p == null) {
      Toolkit.getDefaultToolkit().beep()
      field.selectAll()
    } else {
      stopEditing()
      setCurrentDirectory(p)
    }
  }

  companion object {
    const val CURRENT_DIRECTORY = "currentDirectory"
    private const val CARD_CRUMBS = "crumbs"
    private const val CARD_EDIT = "edit"
    private const val ACTION_EDIT = "editAddress"

    // Expands a leading "~" to the user's home and resolves a relative path against
    // the base directory. Returns null when the text is not an existing directory
    private fun parseInput(base: Path, text: String): Path? {
      var s = text.trim()
      if (s == "~" || s.startsWith("~/") || s.startsWith("~" + File.separator)) {
        s = System.getProperty("user.home") + s.substring(1)
      }
      if (s.isEmpty()) {
        return null
      }
      var p = try {
        base.resolve(s).toAbsolutePath().normalize()
      } catch (ex: InvalidPathException) {
        return null
      }
      if (Files.isRegularFile(p)) {
        p = p.parent
      }
      return p?.takeIf { Files.isDirectory(it) }
    }
  }
}

// Child order: [root icon][places separator][root label][root separator][ellipsis]
//   ([directory][separator])*
// Hides the leftmost directory buttons that do not fit and shows the ellipsis instead.
private class BreadcrumbLayout : LayoutManager {
  var hiddenCount = 0
    private set

  override fun addLayoutComponent(name: String?, comp: Component?) {
    // not needed
  }

  override fun removeLayoutComponent(comp: Component?) {
    // not needed
  }

  override fun preferredLayoutSize(parent: Container) = getLayoutSize(parent, 0)

  override fun minimumLayoutSize(parent: Container) =
    getLayoutSize(parent, getPairCount(parent) - 1)

  override fun layoutContainer(parent: Container) {
    val ins = parent.insets
    val available = parent.width - ins.left - ins.right
    val height = parent.height - ins.top - ins.bottom
    hiddenCount = computeHiddenCount(parent, available)
    var x = ins.left
    for (i in 0..<parent.componentCount) {
      val c = parent.getComponent(i)
      val shown = isShown(i, hiddenCount)
      c.isVisible = shown
      if (shown) {
        val d = c.preferredSize
        // Clip the last components instead of overflowing the container
        val w = d.width.coerceAtMost(ins.left + available - x).coerceAtLeast(0)
        c.setBounds(x, ins.top + (height - d.height) / 2, w, d.height)
        x += w
      }
    }
  }

  companion object {
    private const val FIXED_COUNT = 5
    private const val ELLIPSIS_INDEX = 4

    private fun getPairCount(parent: Container) =
      (parent.componentCount - FIXED_COUNT).coerceAtLeast(0) / 2

    // hidden: number of hidden directory buttons (counted from the left)
    private fun isShown(index: Int, hidden: Int): Boolean {
      if (index < ELLIPSIS_INDEX) {
        return true
      } else if (index == ELLIPSIS_INDEX) {
        return hidden > 0
      }
      val pair = (index - FIXED_COUNT) / 2
      val isButton = (index - FIXED_COUNT) % 2 == 0
      // The separator of the last hidden pair stays visible next to the ellipsis
      return if (isButton) pair >= hidden else pair >= hidden - 1
    }

    private fun getLayoutSize(parent: Container, hidden: Int): Dimension {
      val ins = parent.insets
      var w = 0
      var h = 0
      for (i in 0..<parent.componentCount) {
        val d = parent.getComponent(i).preferredSize
        h = maxOf(h, d.height)
        if (isShown(i, hidden)) {
          w += d.width
        }
      }
      return Dimension(w + ins.left + ins.right, h + ins.top + ins.bottom)
    }

    private fun computeHiddenCount(parent: Container, available: Int): Int {
      val ins = parent.insets
      val pairs = getPairCount(parent)
      return (0..<pairs).firstOrNull {
        getLayoutSize(parent, it).width - ins.left - ins.right <= available
      } ?: (pairs - 1).coerceAtLeast(0)
    }
  }
}

// Flat button: paints a highlight only on rollover, press or while its popup is open
private class CrumbButton(
  text: String?,
  icon: Icon?,
) : JButton(text, icon) {
  private var popupVisible = false

  // The popup was just closed by pressing this button:
  // the following action must not reopen it
  private var skipNextAction = false

  override fun updateUI() {
    super.updateUI()
    isContentAreaFilled = false
    isBorderPainted = false
    isFocusPainted = false
    isRolloverEnabled = true
    isOpaque = false
    iconTextGap = 2
    border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
    cursor = Cursor.getDefaultCursor()
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.width = minOf(d.width, MAX_WIDTH)
    return d
  }

  // Clicking the button shows the popup created by the factory below the button,
  // and clicking it again while the popup is open closes the popup
  fun addPopupToggle(factory: () -> JPopupMenu) {
    addActionListener { togglePopup(factory) }
    addMouseListener(object : MouseAdapter() {
      override fun mouseExited(e: MouseEvent) {
        skipNextAction = false
      }
    })
  }

  private fun togglePopup(factory: () -> JPopupMenu) {
    if (skipNextAction) {
      skipNextAction = false
      return
    }
    val popup = factory()
    popup.addPopupMenuListener(object : PopupMenuListener {
      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        popupVisible = true
        repaint()
      }

      override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
        popupVisible = false
        skipNextAction = isHandlingMousePress()
        repaint()
      }

      override fun popupMenuCanceled(e: PopupMenuEvent) {
        // not needed
      }
    })
    popup.show(this, 0, height)
  }

  // True while a mouse press on this button is dispatched, e.g. when the press
  // closes the popup of this button (a popup closed by a key returns false)
  private fun isHandlingMousePress(): Boolean {
    val e = EventQueue.getCurrentEvent()
    return e is MouseEvent && e.id == MouseEvent.MOUSE_PRESSED && e.component == this
  }

  override fun paintComponent(g: Graphics) {
    val m = getModel()
    if (m.isArmed || m.isRollover || popupVisible) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = if (m.isArmed || popupVisible) PRESSED else ROLLOVER
      g2.fillRoundRect(0, 0, width - 1, height - 1, 4, 4)
      g2.dispose()
    }
    super.paintComponent(g)
    if (isFocusOwner) {
      g.color = Color.GRAY
      BasicGraphicsUtils.drawDashedRect(g, 1, 1, width - 2, height - 2)
    }
  }

  companion object {
    private const val MAX_WIDTH = 160
    private val ROLLOVER = Color(0x20_00_00_00, true)
    private val PRESSED = Color(0x40_00_00_00, true)
  }
}

private class ArrowIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    val aa = RenderingHints.VALUE_ANTIALIAS_ON
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa)
    g2.paint = c.foreground
    g2.stroke = STROKE
    val p = Path2D.Double()
    p.moveTo(x + 3.0, y + 2.0)
    p.lineTo(x + 6.5, y + SIZE / 2.0)
    p.lineTo(x + 3.0, y + SIZE - 2.0)
    g2.draw(p)
    g2.dispose()
  }

  override fun getIconWidth() = SIZE

  override fun getIconHeight() = SIZE

  companion object {
    private const val SIZE = 10
    private val STROKE = BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
  }
}

class DirEntry private constructor(
  val path: Path,
  val name: String,
  val icon: Icon?,
  val isDirectory: Boolean,
) {
  // FileSystemView#getSystemDisplayName(File) and #getSystemIcon(File) take a few
  // milliseconds per file on Windows, so they are used only where they matter:
  // the system name and icon for the root directories (drives), the system icon
  // for regular files, and the file name with the generic folder icon for directories
  constructor(path: Path, directory: Boolean) : this(
    path,
    path.fileName?.toString() ?: getDisplayName(path),
    if (path.fileName == null) {
      FileSystemView.getFileSystemView().getSystemIcon(path.toFile())
    } else if (directory) {
      UIManager.getIcon(DIR_ICON)
    } else {
      getFileIcon(path)
    },
    directory,
  )

  companion object {
    // Icons of regular files are cached by extension, except for the types
    // whose icon is embedded in each file
    private val ICON_CACHE = ConcurrentHashMap<String, Icon>()
    private val OWN_ICON = setOf("", ".exe", ".lnk", ".ico", ".url")
    private const val DIR_ICON = "FileView.directoryIcon"
    private val HOME_FOLDERS = listOf(
      "",
      "Desktop",
      "Documents",
      "Downloads",
      "Music",
      "Pictures",
      "Videos",
    )

    // A directory with its system display name, as the breadcrumb buttons show it
    private fun withDisplayName(dir: Path): DirEntry {
      val icon = if (dir.fileName == null) {
        FileSystemView.getFileSystemView().getSystemIcon(dir.toFile())
      } else {
        UIManager.getIcon(DIR_ICON)
      }
      return DirEntry(dir, getDisplayName(dir), icon, true)
    }

    fun getDisplayName(p: Path): String {
      val name = FileSystemView.getFileSystemView().getSystemDisplayName(p.toFile())
      return name.ifEmpty { p.toString() }
    }

    // The system icon, e.g. the special icon of the Desktop or Downloads folder
    fun getSystemIcon(p: Path): Icon? =
      FileSystemView.getFileSystemView().getSystemIcon(p.toFile())
        ?: UIManager.getIcon(DIR_ICON)

    // The root directories (drives)
    fun listRoots() = FileSystems
      .getDefault()
      .rootDirectories
      .filter { Files.isDirectory(it) }
      .map { DirEntry(it, true) }

    // The subdirectories that are not hidden, sorted by name
    fun listSubdirectories(dir: Path) = listAttributes(dir)
      .filter { (p, attrs) -> attrs.isDirectory && !isHidden(p, attrs) }
      .map { (p, _) -> DirEntry(p, true) }
      .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    // The given directories with their system display names
    fun listWithDisplayNames(dirs: List<Path>) = dirs.map { withDisplayName(it) }

    // A place shown with the system name and icon of the file (e.g. a shortcut)
    // that opens dir
    private fun ofPlace(file: File, dir: Path): DirEntry {
      val fsv = FileSystemView.getFileSystemView()
      val name = fsv.getSystemDisplayName(file)
      val icon = fsv.getSystemIcon(file)
      return DirEntry(
        dir,
        name.ifEmpty { dir.toString() },
        icon ?: UIManager.getIcon(DIR_ICON),
        true,
      )
    }

    // The Desktop and its items that are directories on the file system, like the list
    // next to the address bar icon of Windows Explorer. The virtual folders such as
    // PC, Libraries and Network have no Path and are skipped. Where the root of
    // FileSystemView is not the Desktop but the file system root (e.g. "/"), the user's
    // home and its common subdirectories are listed instead.
    fun listPlaces(): List<DirEntry> {
      val fsv = FileSystemView.getFileSystemView()
      val roots = fsv.roots
      return if (roots.isEmpty() || fsv.isFileSystemRoot(roots[0])) {
        listHomeFolders()
      } else {
        listDesktopItems(fsv, roots[0])
      }
    }

    private fun listDesktopItems(fsv: FileSystemView, desktop: File): List<DirEntry> {
      val places = LinkedHashMap<Path, DirEntry>()
      places[desktop.toPath()] = ofPlace(desktop, desktop.toPath())
      for (f in fsv.getFiles(desktop, true)) {
        // Shortcuts are skipped: the path of a shortcut file is not a directory.
        // Java 9+ can resolve them with FileSystemView#isLink(File)
        // and #getLinkLocation(File)
        if (fsv.isFileSystem(f) && Files.isDirectory(f.toPath())) {
          places.putIfAbsent(f.toPath(), ofPlace(f, f.toPath()))
        }
      }
      return places.values.toList()
    }

    private fun listHomeFolders(): List<DirEntry> {
      val home = Paths.get(System.getProperty("user.home"))
      return HOME_FOLDERS
        .map { home.resolve(it) }
        .filter { Files.isDirectory(it) }
        .map { ofPlace(it.toFile(), it) }
    }

    private fun getFileIcon(path: Path): Icon? {
      val n = path.fileName.toString().lowercase(Locale.ROOT)
      val i = n.lastIndexOf('.')
      val ext = if (i < 0) "" else n.substring(i)
      val fsv = FileSystemView.getFileSystemView()
      return if (ext in OWN_ICON) {
        fsv.getSystemIcon(path.toFile())
      } else {
        ICON_CACHE.computeIfAbsent(ext) { fsv.getSystemIcon(path.toFile()) }
      }
    }

    // Lists the direct children with their attributes. On Windows the attributes
    // come from the directory scan, which is much faster than Files#isDirectory(Path)
    // per child; the returned attributes also report the hidden flag of directories
    // correctly, unlike Files#isHidden(Path)
    fun listAttributes(dir: Path): Map<Path, BasicFileAttributes> {
      val map = LinkedHashMap<Path, BasicFileAttributes>()
      val options = EnumSet.noneOf(FileVisitOption::class.java)
      Files.walkFileTree(
        dir,
        options,
        1,
        object : SimpleFileVisitor<Path>() {
          override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes,
          ): FileVisitResult {
            map[file] = attrs
            return FileVisitResult.CONTINUE
          }

          // Skip unreadable children, but report an unreadable dir itself
          override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
            if (file == dir) {
              throw exc
            }
            return FileVisitResult.CONTINUE
          }
        },
      )
      return map
    }

    fun isHidden(path: Path, attrs: BasicFileAttributes) =
      if (attrs is DosFileAttributes) attrs.isHidden else path.toFile().isHidden
  }
}

// Lists directories: the subdirectories of a directory (or the root directories),
// or the given directories such as the hidden breadcrumb ancestors.
// The entries are loaded on a worker thread while a "Loading..." item is shown,
// and shown in a scrollable JList instead of JMenuItems.
// The popup is not focusable, like the JComboBox popup: PopupFactory makes the
// heavyweight popup window focusable when a JPopupMenu contains a component other
// than a MenuElement, and Windows then activates that window for a moment, which
// makes the title bar of the frame flicker. The invoker keeps the focus instead
// and forwards the keys to the list while the popup is open.
private class DirectoryPopupMenu(
  private val loader: () -> List<DirEntry>,
  private val current: Path?,
  private val navigator: (Path) -> Unit,
) : JPopupMenu() {
  private val loading = createInfoItem("Loading...")
  private val focusHandler = object : FocusAdapter() {
    override fun focusLost(e: FocusEvent) {
      if (!e.isTemporary) {
        isVisible = false
      }
    }
  }
  private var list: DirectoryList? = null

  init {
    isFocusable = false
    add(loading)
    addPopupMenuListener(object : PopupMenuListener {
      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        installKeyBindings()
      }

      override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
        uninstallKeyBindings()
      }

      override fun popupMenuCanceled(e: PopupMenuEvent) {
        // not needed
      }
    })
  }

  // Keeps the popup at least as wide as the "Loading..." item
  // so that the width does not shrink (flicker) when the items are replaced
  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    val i = insets
    d.width = maxOf(d.width, loading.preferredSize.width + i.left + i.right)
    return d
  }

  override fun show(invoker: Component?, x: Int, y: Int) {
    super.show(invoker, x, y)
    object : SwingWorker<List<DirEntry>, Unit>() {
      override fun doInBackground() = loader()

      override fun done() {
        // Skip when the popup was closed before loading finished
        if (this@DirectoryPopupMenu.isVisible) {
          updateItems(this)
        }
      }
    }.execute()
  }

  private fun updateItems(worker: SwingWorker<List<DirEntry>, *>) {
    removeAll()
    try {
      addEntries(worker.get())
    } catch (ex: InterruptedException) {
      Thread.currentThread().interrupt()
    } catch (ex: ExecutionException) {
      add(createInfoItem("(Access denied)"))
    }
    pack()
  }

  private fun addEntries(entries: List<DirEntry>) {
    if (entries.isEmpty()) {
      add(createInfoItem("(No subfolders)"))
      return
    }
    val l = DirectoryList(entries, current, navigator)
    list = l
    val scroll = JScrollPane(l)
    scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    scroll.verticalScrollBar.unitIncrement = l.fixedCellHeight
    scroll.verticalScrollBar.isFocusable = false
    scroll.isFocusable = false
    scroll.border = BorderFactory.createEmptyBorder()
    scroll.viewportBorder = BorderFactory.createEmptyBorder()
    add(scroll)
  }

  private fun installKeyBindings() {
    val c = invoker
    if (c is JComponent) {
      KEY_ACTIONS.forEach { (key, name) ->
        c.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), PREFIX + name)
        c.actionMap.put(
          PREFIX + name,
          object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
              performKeyAction(name)
            }
          },
        )
      }
      // Closes the popup when the focus moves elsewhere, e.g. with the Tab key
      c.addFocusListener(focusHandler)
    }
  }

  private fun uninstallKeyBindings() {
    val c = invoker
    if (c is JComponent) {
      KEY_ACTIONS.forEach { (key, name) ->
        c.getInputMap(WHEN_FOCUSED).remove(KeyStroke.getKeyStroke(key))
        c.actionMap.remove(PREFIX + name)
      }
      c.removeFocusListener(focusHandler)
    }
  }

  private fun performKeyAction(name: String) {
    if (name == CLOSE) {
      isVisible = false
    } else {
      list?.performAction(name)
    }
  }

  companion object {
    const val NAVIGATE = "navigate"
    private const val PREFIX = "DirectoryPopupMenu."
    private const val CLOSE = "close"

    // Key -> JList action name, or NAVIGATE / CLOSE
    private val KEY_ACTIONS = mapOf(
      "UP" to "selectPreviousRow",
      "DOWN" to "selectNextRow",
      "PAGE_UP" to "scrollUp",
      "PAGE_DOWN" to "scrollDown",
      "HOME" to "selectFirstRow",
      "END" to "selectLastRow",
      "ENTER" to NAVIGATE,
      "ESCAPE" to CLOSE,
      "SPACE" to CLOSE,
    )

    // Subdirectories of dir, or the root directories (drives) when dir is null;
    // ancestors of current are shown in bold
    fun ofChildren(dir: Path?, current: Path?, navigator: (Path) -> Unit) =
      DirectoryPopupMenu(
        { if (dir == null) DirEntry.listRoots() else DirEntry.listSubdirectories(dir) },
        current,
        navigator,
      )

    // The given directories with their system display names
    fun ofPaths(dirs: List<Path>, navigator: (Path) -> Unit): DirectoryPopupMenu {
      val copy = dirs.toList()
      return DirectoryPopupMenu({ DirEntry.listWithDisplayNames(copy) }, null, navigator)
    }

    // The places such as the Desktop; ancestors of current are shown in bold
    fun ofPlaces(current: Path?, navigator: (Path) -> Unit) =
      DirectoryPopupMenu({ DirEntry.listPlaces() }, current, navigator)

    private fun createInfoItem(text: String) = JMenuItem(text).also {
      it.isEnabled = false
    }
  }
}

// Scrollable list of directories shown in DirectoryPopupMenu.
// The row under the mouse is selected like a menu item. The list never gets the focus;
// the keys are forwarded by DirectoryPopupMenu from the invoker.
private class DirectoryList(
  entries: List<DirEntry>,
  current: Path?,
  private val navigator: (Path) -> Unit,
) : JList<DirEntry>(createModel(entries)) {
  init {
    val renderer = DirEntryRenderer(current)
    cellRenderer = renderer
    val c = renderer.getListCellRendererComponent(this, entries[0], 0, false, false)
    fixedCellHeight = c.preferredSize.height
    visibleRowCount = minOf(entries.size, MAX_ROWS)
  }

  override fun updateUI() {
    // The handler is looked up by type: a property initializer would run
    // after JList's constructor has already called updateUI()
    mouseListeners.filterIsInstance<RolloverHandler>().forEach {
      removeMouseListener(it)
      removeMouseMotionListener(it)
    }
    super.updateUI()
    isFocusable = false
    selectionMode = ListSelectionModel.SINGLE_SELECTION
    selectionBackground = UIManager.getColor("MenuItem.selectionBackground")
    selectionForeground = UIManager.getColor("MenuItem.selectionForeground")
    val h = RolloverHandler()
    addMouseListener(h)
    addMouseMotionListener(h)
  }

  override fun getPreferredScrollableViewportSize(): Dimension {
    val d = super.getPreferredScrollableViewportSize()
    d.width = minOf(d.width, MAX_WIDTH)
    return d
  }

  // Performs the JList action of the name, or moves to the selected directory
  // for DirectoryPopupMenu.NAVIGATE
  fun performAction(name: String) {
    if (name == DirectoryPopupMenu.NAVIGATE) {
      navigateSelected()
    } else {
      val e = ActionEvent(this, ActionEvent.ACTION_PERFORMED, name)
      actionMap[name]?.actionPerformed(e)
    }
  }

  // Closes the popup and moves to the selected directory
  fun navigateSelected() {
    val entry = selectedValue ?: return
    val popup = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, this)
    if (popup is JPopupMenu) {
      popup.isVisible = false
    }
    navigator(entry.path)
  }

  private inner class RolloverHandler : MouseAdapter() {
    private fun setRollover(e: MouseEvent) {
      val pt = e.point
      val index = locationToIndex(pt)
      val r = getCellBounds(index, index)
      if (r?.contains(pt) == true) {
        selectedIndex = index
      } else {
        clearSelection()
      }
    }

    override fun mouseMoved(e: MouseEvent) {
      setRollover(e)
    }

    override fun mouseDragged(e: MouseEvent) {
      setRollover(e)
    }

    override fun mouseExited(e: MouseEvent) {
      clearSelection()
    }

    override fun mouseClicked(e: MouseEvent) {
      if (SwingUtilities.isLeftMouseButton(e)) {
        setRollover(e)
        navigateSelected()
      }
    }
  }

  companion object {
    private const val MAX_ROWS = 12
    private const val MAX_WIDTH = 400

    private fun createModel(entries: List<DirEntry>) = DefaultListModel<DirEntry>().also {
      entries.forEach(it::addElement)
    }
  }
}

// Shows the name and icon of a DirEntry; ancestors of the current directory are bold
// (current == null: no bold)
private class DirEntryRenderer(
  private val current: Path?,
) : DefaultListCellRenderer() {
  override fun getListCellRendererComponent(
    list: JList<*>,
    value: Any?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    super.getListCellRendererComponent(list, value, index, isSelected, false)
    if (value is DirEntry) {
      text = value.name
      icon = value.icon
      val bold = current?.startsWith(value.path) == true
      font = font.deriveFont(if (bold) Font.BOLD else Font.PLAIN)
    }
    border = BorderFactory.createEmptyBorder(2, 4, 2, 8)
    return this
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
