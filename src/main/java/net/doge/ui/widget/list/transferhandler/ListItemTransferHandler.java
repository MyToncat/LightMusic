package net.doge.ui.widget.list.transferhandler;

import net.doge.util.core.log.LogUtil;
import net.doge.util.core.math.MathUtil;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;

/**
 * 泛型 TransferHandler，支持任意类型 T 的 JList 内部拖拽排序。
 * <p>
 * 要求：JList 的 Model 必须是 DefaultListModel<T>，且 T 正确重写 equals() 和 hashCode()。
 *
 * @param <T> 列表元素的类型
 */
public class ListItemTransferHandler<T> extends TransferHandler {
    // 自定义 DataFlavor，利用 JVM 本地对象传递 ArrayList<T>
    private final DataFlavor listFlavor;
    // 暂存被拖拽的数据（用于 exportDone 清理）
    private List<T> draggedItems;

    public ListItemTransferHandler() {
        // 关键：DataFlavor 的 class 参数指定为 ArrayList.class，
        // 因为传递的是 ArrayList 实例（泛型擦除不影响运行时）
        listFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.ArrayList", "List of Items");
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        @SuppressWarnings("unchecked")
        JList<T> list = (JList<T>) c;
        List<T> selectedValues = list.getSelectedValuesList();

        if (selectedValues == null || selectedValues.isEmpty()) return null;

        // 保存一份副本，用于后续删除
        draggedItems = new ArrayList<>(selectedValues);

        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{listFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return listFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (isDataFlavorSupported(flavor)) return draggedItems;  // 直接返回 ArrayList<T>
                throw new UnsupportedFlavorException(flavor);
            }
        };
    }

    @Override
    public boolean canImport(TransferSupport support) {
        // 仅接受我们自定义的 flavor 并检查目标组件是否为 JList
        if (!support.isDataFlavorSupported(listFlavor) || !(support.getComponent() instanceof JList)) return false;

        // 检查目标 JList 的 Model 是否为 DefaultListModel（可变模型）
        JList<?> list = (JList<?>) support.getComponent();
        if (!(list.getModel() instanceof DefaultListModel)) return false;

        // 不允许拖拽到空白区域（索引 -1）
        JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
        return dropLocation.getIndex() != -1;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;

        // 获取目标 JList 及其模型
        @SuppressWarnings("unchecked")
        JList<T> targetList = (JList<T>) support.getComponent();
        @SuppressWarnings("unchecked")
        DefaultListModel<T> model = (DefaultListModel<T>) targetList.getModel();

        JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
        int targetIndex = dropLocation.getIndex();

        // 获取拖拽的数据（List<T>）
        Transferable transferable = support.getTransferable();
        List<T> sourceItems;
        try {
            sourceItems = (List<T>) transferable.getTransferData(listFlavor);
        } catch (Exception e) {
            LogUtil.error(e);
            return false;
        }

        if (sourceItems.isEmpty()) return false;

        // ----- 核心：从模型中删除源数据，然后在目标位置插入 -----
        // 1. 记录被删除元素的索引（用于修正目标位置）
        List<Integer> removedIndices = new ArrayList<>();
        for (T item : sourceItems) {
            int index = model.indexOf(item);
            if (index != -1) {
                removedIndices.add(index);
                model.remove(index);
            }
        }

        // 2. 修正目标插入索引
        //    如果目标位置位于某个被删除元素之后，则目标索引需要减去前面已删除的数量
        int adjustment = 0;
        for (int removedIndex : removedIndices) {
            if (targetIndex > removedIndex) adjustment++;
        }
        int finalInsertIndex = targetIndex - adjustment;

        // 边界保护
        finalInsertIndex = MathUtil.clamp(finalInsertIndex, 0, model.getSize());

        // 3. 按原始顺序插入到新位置
        int insertPos = finalInsertIndex;
        for (T item : sourceItems) {
            model.add(insertPos, item);
            insertPos++;
        }

        // 4. 选中新插入的项目
        int start = finalInsertIndex, end = finalInsertIndex + sourceItems.size() - 1;
        targetList.setSelectionInterval(start, end);

        return true;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        // 清理引用，帮助 GC
        draggedItems = null;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;  // 移动操作
    }
}
